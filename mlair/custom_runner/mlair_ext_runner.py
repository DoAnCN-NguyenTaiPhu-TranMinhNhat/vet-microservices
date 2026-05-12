from __future__ import annotations

import json
import os
import sys
import time
import urllib.error
import urllib.request
from typing import Any, Dict, Optional


def _env_int(name: str, default: int) -> int:
    raw = (os.getenv(name) or "").strip()
    if not raw:
        return default
    try:
        return int(raw)
    except ValueError:
        return default


def _post_json(url: str, token: str, body: Dict[str, Any], timeout_seconds: int) -> Dict[str, Any]:
    data = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=data,
        method="POST",
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {token}",
        },
    )
    with urllib.request.urlopen(req, timeout=timeout_seconds) as resp:
        raw = resp.read()
    if not raw:
        return {}
    return json.loads(raw.decode("utf-8"))


def _get_json(url: str, timeout_seconds: int) -> Dict[str, Any]:
    with urllib.request.urlopen(url, timeout=timeout_seconds) as resp:
        raw = resp.read()
    if not raw:
        return {}
    return json.loads(raw.decode("utf-8"))


def _vetai_training_invoke_context(ctx: Dict[str, Any]) -> Dict[str, Any]:
    # Support both "context" nesting (MLAir run payload style) and direct flat dict.
    if isinstance(ctx.get("context"), dict):
        c = dict(ctx.get("context") or {})
    else:
        c = dict(ctx)

    # Allow either explicit project_id or derive from env fallback (rare).
    pid = (c.get("project_id") or "").strip()
    if not pid:
        raise ValueError("context.project_id is required (default_project or clinic_<id>)")

    out: Dict[str, Any] = {"project_id": pid}
    tid = (c.get("tenant_id") or "").strip()
    if tid:
        out["tenant_id"] = tid

    # default: force true (bypass threshold if at least one eligible sample exists)
    out["force"] = bool(c.get("force", True))
    mode = str(c.get("training_mode") or "local").strip() or "local"
    out["training_mode"] = mode
    fb = (c.get("finetune_base_model_version") or None)
    if fb is not None:
        s = str(fb).strip()
        if s:
            out["finetune_base_model_version"] = s
    return out


def _vetai_dataset_train_context(ctx: Dict[str, Any]) -> Dict[str, Any]:
    if isinstance(ctx.get("context"), dict):
        c = dict(ctx.get("context") or {})
    else:
        c = dict(ctx)

    pid = (c.get("project_id") or "").strip()
    if not pid:
        raise ValueError("context.project_id is required (default_project or clinic_<id>)")

    dvid = (c.get("dataset_version_id") or "").strip()
    if not dvid:
        raise ValueError("context.dataset_version_id is required")

    out: Dict[str, Any] = {"project_id": pid, "dataset_version_id": dvid}
    tid = (c.get("tenant_id") or "").strip()
    if tid:
        out["tenant_id"] = tid
    mode = str(c.get("training_mode") or "local").strip() or "local"
    out["training_mode"] = mode
    return out


def _run_vetai_train_invoke(plugin_context: Dict[str, Any]) -> Dict[str, Any]:
    url = (os.getenv("VETAI_TRAINING_INVOKE_URL") or "").strip()
    token = (os.getenv("VETAI_TRAINING_INVOKE_TOKEN") or "").strip()
    if not url:
        raise RuntimeError("VETAI_TRAINING_INVOKE_URL is not set on mlair-executor")
    if not token:
        raise RuntimeError("VETAI_TRAINING_INVOKE_TOKEN is not set on mlair-executor")

    timeout = _env_int("VETAI_HTTP_TIMEOUT_SECONDS", 30)
    poll_seconds = max(1, _env_int("VETAI_TRAINING_POLL_SECONDS", 5))
    max_wait = max(10, _env_int("VETAI_TRAINING_MAX_WAIT_SECONDS", 3600))

    body = _vetai_training_invoke_context(plugin_context)
    try:
        invoke_resp = _post_json(url, token, body, timeout_seconds=timeout)
    except urllib.error.HTTPError as exc:
        detail = ""
        try:
            detail = (exc.read() or b"")[:2000].decode("utf-8", errors="replace")
        except Exception:
            detail = ""
        raise RuntimeError(f"vet-ai training-invoke HTTP {exc.code}: {detail or exc.reason}") from exc
    training_id = invoke_resp.get("training_id")
    if training_id is None:
        raise RuntimeError(f"Vet-AI training-invoke did not return training_id: {invoke_resp}")

    status_url = (os.getenv("VETAI_TRAINING_STATUS_URL") or "").strip()
    if not status_url:
        # Vet-AI continuous-training router prefix is /continuous-training.
        # Keep it configurable for other deployments.
        base = (os.getenv("VETAI_BASE_URL") or "http://vet-ai:8000").strip().rstrip("/")
        status_url = f"{base}/continuous-training/training/status"

    deadline = time.time() + max_wait
    last: Optional[Dict[str, Any]] = None
    while True:
        if time.time() > deadline:
            raise TimeoutError(f"Vet-AI training_id={training_id} did not finish within {max_wait}s")
        try:
            last = _get_json(f"{status_url}?training_id={training_id}", timeout_seconds=timeout)
        except urllib.error.HTTPError as exc:
            # transient while job row is being created/committed
            if exc.code in (404, 502, 503, 504):
                time.sleep(poll_seconds)
                continue
            raise

        st = str(last.get("status") or "").lower()
        if st in ("completed", "failed"):
            break
        time.sleep(poll_seconds)

    ok = str(last.get("status") or "").lower() == "completed"
    training_metrics_blob = last.get("training_metrics") if isinstance(last, dict) else None
    training_metrics = training_metrics_blob if isinstance(training_metrics_blob, dict) else {}
    metrics: Dict[str, Any] = {}
    for k in ("training_accuracy", "validation_accuracy", "f1_score"):
        v = last.get(k)
        if v is None:
            continue
        try:
            metrics[k] = {"step": 1, "value": float(v)}
        except (TypeError, ValueError):
            continue

    params: Dict[str, Any] = {
        "source": "vetai_train_invoke",
        "training_id": int(training_id),
        "vetai_status": str(last.get("status") or ""),
    }
    if last.get("clinic_id") is not None:
        params["clinic_id"] = str(last.get("clinic_id") or "")

    artifacts = []
    lineage = None
    mlair_sync = training_metrics.get("mlair_sync") if isinstance(training_metrics.get("mlair_sync"), dict) else {}
    mv = mlair_sync.get("mlair_version") if isinstance(mlair_sync.get("mlair_version"), dict) else {}
    artifact_uri = mv.get("artifact_uri") or mv.get("uri") or None
    if artifact_uri:
        artifacts.append({"path": "model", "uri": str(artifact_uri)})
        lineage = {
            "outputs": [
                {"name": "model", "version": str(mv.get("version") or ""), "uri": str(artifact_uri)},
            ]
        }

    out = {"params": params, "metrics": metrics, "artifacts": artifacts, "lineage": lineage}
    if not ok:
        err = str(last.get("error_message") or "vet-ai training failed")
        raise RuntimeError(err)
    return out


def _log(msg: str) -> None:
    """Debug log to stderr + file (executor may not expose stderr)."""
    line = f"[mlair_ext_runner] {msg}"
    print(line, file=sys.stderr, flush=True)
    try:
        with open("/tmp/mlair_ext_runner.log", "a") as f:
            f.write(f"{time.strftime('%Y-%m-%d %H:%M:%S')} {line}\n")
    except Exception:
        pass


def _extract_run_id(ctx: Dict[str, Any]) -> Optional[str]:
    """Try multiple field paths to extract run_id from the executor's stdin JSON."""
    for key in ("run_id", "runId", "task_run_id"):
        v = ctx.get(key)
        if isinstance(v, str) and v.strip():
            return v.strip()
        if isinstance(v, int):
            return str(v)
    nested = ctx.get("context")
    if isinstance(nested, dict):
        for key in ("run_id", "runId"):
            v = nested.get(key)
            if isinstance(v, str) and v.strip():
                return v.strip()
            if isinstance(v, int):
                return str(v)
    meta = ctx.get("metadata")
    if isinstance(meta, dict):
        v = meta.get("run_id") or meta.get("runId")
        if isinstance(v, str) and v.strip():
            return v.strip()
        if isinstance(v, int):
            return str(v)
    task = ctx.get("task")
    if isinstance(task, dict):
        v = task.get("run_id") or task.get("runId")
        if isinstance(v, str) and v.strip():
            return v.strip()
        if isinstance(v, int):
            return str(v)
    return None


def _mlair_api_headers() -> Dict[str, str]:
    api_token = (os.getenv("ML_AIR_TRACKING_TOKEN") or os.getenv("VETAI_TRAINING_INVOKE_TOKEN") or "").strip()
    headers: Dict[str, str] = {"Content-Type": "application/json"}
    if api_token:
        headers["Authorization"] = f"Bearer {api_token}"
    return headers


def _mlair_run_url(run_id: str, suffix: str) -> Optional[str]:
    api_base = (os.getenv("ML_AIR_API_BASE_URL") or "").strip().rstrip("/")
    tenant = (os.getenv("ML_AIR_TENANT_ID") or "default").strip()
    project = (os.getenv("ML_AIR_PROJECT_ID") or "default_project").strip()
    if not api_base or not run_id:
        return None
    return f"{api_base}/v1/tenants/{tenant}/projects/{project}/runs/{run_id}/{suffix}"


def _mlair_post_run_metric(
    run_id: str, name: str, value: float, step: int, timeout_seconds: int = 15,
) -> None:
    """POST a single metric to the MLAir pipeline run (best-effort)."""
    url = _mlair_run_url(run_id, "metrics")
    if not url:
        return
    body = {"key": name, "value": value, "step": step}
    try:
        data = json.dumps(body).encode("utf-8")
        req = urllib.request.Request(url, data=data, method="POST", headers=_mlair_api_headers())
        urllib.request.urlopen(req, timeout=timeout_seconds)
    except Exception as exc:
        _log(f"POST metric {name}={value} step={step} to {url} failed: {exc}")


def _mlair_post_run_param(
    run_id: str, name: str, value: str, timeout_seconds: int = 15,
) -> None:
    """POST a single param to the MLAir pipeline run (best-effort)."""
    url = _mlair_run_url(run_id, "params")
    if not url:
        return
    body = {"key": name, "value": value}
    try:
        data = json.dumps(body).encode("utf-8")
        req = urllib.request.Request(url, data=data, method="POST", headers=_mlair_api_headers())
        urllib.request.urlopen(req, timeout=timeout_seconds)
    except Exception as exc:
        _log(f"POST param {name}={value[:100]} to {url} failed: {exc}")


def _mlair_post_run_artifact(
    run_id: str, path: str, uri: str, timeout_seconds: int = 15,
) -> None:
    """POST a single artifact to the MLAir pipeline run (best-effort)."""
    url = _mlair_run_url(run_id, "artifacts")
    if not url:
        return
    body = {"path": path, "uri": uri}
    try:
        data = json.dumps(body).encode("utf-8")
        req = urllib.request.Request(url, data=data, method="POST", headers=_mlair_api_headers())
        urllib.request.urlopen(req, timeout=timeout_seconds)
    except Exception as exc:
        _log(f"POST artifact {path} to {url} failed: {exc}")


def _run_vetai_dataset_train(plugin_context: Dict[str, Any]) -> Dict[str, Any]:
    url = (os.getenv("VETAI_DATASET_TRAIN_URL") or "").strip()
    token = (os.getenv("VETAI_TRAINING_INVOKE_TOKEN") or "").strip()
    if not url:
        raise RuntimeError("VETAI_DATASET_TRAIN_URL is not set on mlair-executor")
    if not token:
        raise RuntimeError("VETAI_TRAINING_INVOKE_TOKEN is not set on mlair-executor")

    timeout = _env_int("VETAI_HTTP_TIMEOUT_SECONDS", 30)
    poll_seconds = max(1, _env_int("VETAI_TRAINING_POLL_SECONDS", 5))
    max_wait = max(10, _env_int("VETAI_TRAINING_MAX_WAIT_SECONDS", 3600))

    _log(f"stdin context keys: {list(plugin_context.keys())}")
    _log(f"stdin context (truncated): {json.dumps(plugin_context, default=str)[:2000]}")

    run_id = _extract_run_id(plugin_context)
    _log(f"extracted run_id={run_id}")
    _log(f"ML_AIR_API_BASE_URL={os.getenv('ML_AIR_API_BASE_URL', '(unset)')}")
    _log(f"ML_AIR_TRACKING_TOKEN={'set' if os.getenv('ML_AIR_TRACKING_TOKEN') else '(unset)'}")

    body = _vetai_dataset_train_context(plugin_context)
    if run_id:
        body["mlair_run_id"] = run_id

    try:
        invoke_resp = _post_json(url, token, body, timeout_seconds=timeout)
    except urllib.error.HTTPError as exc:
        detail = ""
        try:
            detail = (exc.read() or b"")[:2000].decode("utf-8", errors="replace")
        except Exception:
            detail = ""
        raise RuntimeError(f"vet-ai dataset-train HTTP {exc.code}: {detail or exc.reason}") from exc

    training_id = invoke_resp.get("training_id")
    if training_id is None:
        raise RuntimeError(f"Vet-AI dataset-train did not return training_id: {invoke_resp}")
    _log(f"training started: training_id={training_id}")

    status_url = (os.getenv("VETAI_TRAINING_STATUS_URL") or "").strip()
    if not status_url:
        base = (os.getenv("VETAI_BASE_URL") or "http://vet-ai:8000").strip().rstrip("/")
        status_url = f"{base}/continuous-training/training/status"

    deadline = time.time() + max_wait
    last: Optional[Dict[str, Any]] = None
    metric_step = 0
    last_pct: Optional[int] = None

    while True:
        if time.time() > deadline:
            raise TimeoutError(f"Vet-AI training_id={training_id} did not finish within {max_wait}s")
        try:
            last = _get_json(f"{status_url}?training_id={training_id}", timeout_seconds=timeout)
        except urllib.error.HTTPError as exc:
            if exc.code in (404, 502, 503, 504):
                time.sleep(poll_seconds)
                continue
            raise

        # Post intermediate progress metrics to MLAir pipeline run
        if run_id and isinstance(last, dict):
            pct = last.get("progress_pct")
            phase = last.get("current_phase")
            if pct is not None and pct != last_pct:
                last_pct = pct
                metric_step += 1
                _log(f"progress: phase={phase} pct={pct} step={metric_step}")
                _mlair_post_run_metric(run_id, "progress_pct", float(pct), metric_step)
                if phase:
                    _mlair_post_run_param(run_id, "current_phase", str(phase))
                pm = last.get("phase_metrics")
                if isinstance(pm, dict):
                    for mk, mv in pm.items():
                        if isinstance(mv, (int, float)) and not isinstance(mv, bool):
                            _mlair_post_run_metric(run_id, str(mk), float(mv), metric_step)

        st = str(last.get("status") or "").lower()
        if st in ("completed", "failed"):
            _log(f"training finished: status={st}")
            break
        time.sleep(poll_seconds)

    ok = str(last.get("status") or "").lower() == "completed"
    training_metrics_blob = last.get("training_metrics") if isinstance(last, dict) else None
    training_metrics = training_metrics_blob if isinstance(training_metrics_blob, dict) else {}

    # Build final metrics/params/artifacts for executor result
    metrics: Dict[str, Any] = {}
    for k in ("training_accuracy", "validation_accuracy", "f1_score"):
        v = last.get(k)
        if v is None:
            continue
        try:
            metrics[k] = {"step": 1, "value": float(v)}
        except (TypeError, ValueError):
            continue

    params: Dict[str, Any] = {"source": "vetai_dataset_train", "training_id": int(training_id)}
    artifacts = []
    lineage = None
    mlair_sync = training_metrics.get("mlair_sync") if isinstance(training_metrics.get("mlair_sync"), dict) else {}
    mv = mlair_sync.get("mlair_version") if isinstance(mlair_sync.get("mlair_version"), dict) else {}
    artifact_uri = mv.get("artifact_uri") or mv.get("uri") or None
    if artifact_uri:
        artifacts.append({"path": "model", "uri": str(artifact_uri)})
        lineage = {"outputs": [{"name": "model", "version": str(mv.get("version") or ""), "uri": str(artifact_uri)}]}

    # Post final params/metrics/artifacts to the pipeline run
    if run_id:
        _log(f"posting final tracking to run_id={run_id}")
        final_step = metric_step + 1
        _mlair_post_run_metric(run_id, "progress_pct", 100.0, final_step)
        for pk, pv in params.items():
            _mlair_post_run_param(run_id, str(pk), str(pv))
        for mk_name, mk_val in metrics.items():
            if isinstance(mk_val, dict) and "value" in mk_val:
                _mlair_post_run_metric(run_id, mk_name, float(mk_val["value"]), final_step)

        # Post only essential training metrics (skip internal blobs)
        essential_metrics = {
            "training_accuracy", "validation_accuracy", "validation_f1",
            "cv_mean_accuracy", "cv_std_accuracy", "cv_mean_f1_weighted",
            "cv_std_f1_weighted", "training_time_seconds",
            "n_samples", "n_features", "n_classes",
            "calibration_brier_before", "calibration_brier_after",
            "golden_base_accuracy", "golden_new_accuracy",
            "golden_base_f1_weighted", "golden_new_f1_weighted",
            "feedback_base_score", "feedback_new_score",
        }
        essential_params = {
            "training_id", "model_version", "training_mode",
            "pipeline_kind", "cv_strategy", "validation_mode_used",
            "validation_note", "calibration_method", "training_scope",
        }
        for mk, mv_val in training_metrics.items():
            if mk not in essential_metrics:
                continue
            if isinstance(mv_val, (int, float)) and not isinstance(mv_val, bool):
                _mlair_post_run_metric(run_id, mk, float(mv_val), final_step)
        for pk_name, pk_val in training_metrics.items():
            if pk_name not in essential_params:
                continue
            if isinstance(pk_val, str):
                _mlair_post_run_param(run_id, f"vetai_{pk_name}", pk_val[:8000])
        if artifact_uri:
            _mlair_post_run_artifact(run_id, "vet-ai/model", str(artifact_uri))
        _log("final tracking posted")
    else:
        _log("WARNING: no run_id available, cannot post realtime metrics to MLAir")

    out = {"params": params, "metrics": metrics, "artifacts": artifacts, "lineage": lineage}
    if not ok:
        err = str(last.get("error_message") or "vet-ai training failed")
        raise RuntimeError(err)
    return out


# ── Multi-phase pipeline handlers ──────────────────────────────────────


def _vetai_phase_context(ctx: Dict[str, Any]) -> Dict[str, Any]:
    """Build the common POST body for phase endpoints from executor context."""
    if isinstance(ctx.get("context"), dict):
        c = dict(ctx.get("context") or {})
    else:
        c = dict(ctx)

    pid = (c.get("project_id") or "").strip()
    if not pid:
        raise ValueError("context.project_id is required")

    out: Dict[str, Any] = {"project_id": pid}
    tid = (c.get("tenant_id") or "").strip()
    if tid:
        out["tenant_id"] = tid
    mode = str(c.get("training_mode") or "local").strip() or "local"
    out["training_mode"] = mode

    dvid = (c.get("dataset_version_id") or "").strip()
    if dvid:
        out["dataset_version_id"] = dvid

    return out


def _poll_phase_status(
    session_id: str,
    phase: str,
    run_id: Optional[str],
    poll_seconds: int,
    max_wait: int,
) -> Dict[str, Any]:
    """Poll GET /mlops/mlair/phase-status until the phase completes or fails."""
    base = (os.getenv("VETAI_BASE_URL") or "http://vet-ai:8000").strip().rstrip("/")
    timeout = _env_int("VETAI_HTTP_TIMEOUT_SECONDS", 30)
    status_url = f"{base}/mlops/mlair/phase-status?session_id={session_id}&phase={phase}"

    deadline = time.time() + max_wait
    metric_step = 0
    last_pct: Optional[int] = None

    while True:
        if time.time() > deadline:
            raise TimeoutError(f"Phase {phase} session={session_id} did not finish within {max_wait}s")
        try:
            last = _get_json(status_url, timeout_seconds=timeout)
        except urllib.error.HTTPError as exc:
            if exc.code in (404, 502, 503, 504):
                time.sleep(poll_seconds)
                continue
            raise

        # Post per-task progress to MLAir run (prefixed by phase name)
        if run_id and isinstance(last, dict):
            pct = last.get("progress_pct")
            cur_phase = last.get("current_phase")
            if pct is not None and pct != last_pct:
                last_pct = pct
                metric_step += 1
                _log(f"phase {phase}: progress={pct}% sub_phase={cur_phase} step={metric_step}")
                _mlair_post_run_metric(run_id, f"{phase}_progress_pct", float(pct), metric_step)
                if cur_phase:
                    _mlair_post_run_param(run_id, f"{phase}_phase", str(cur_phase))

        st = str(last.get("status") or "").lower()
        if st == "completed":
            _log(f"phase {phase} completed")
            return last
        if st == "failed":
            err = last.get("error_message") or f"phase {phase} failed"
            raise RuntimeError(err)
        time.sleep(poll_seconds)


def _run_vetai_phase(
    plugin_context: Dict[str, Any],
    phase: str,
    endpoint_env: str,
    endpoint_default: str,
) -> Dict[str, Any]:
    """Generic handler for all multi-phase plugins."""
    url = (os.getenv(endpoint_env) or "").strip()
    if not url:
        base = (os.getenv("VETAI_BASE_URL") or "http://vet-ai:8000").strip().rstrip("/")
        url = f"{base}{endpoint_default}"
    token = (os.getenv("VETAI_TRAINING_INVOKE_TOKEN") or "").strip()
    if not token:
        raise RuntimeError("VETAI_TRAINING_INVOKE_TOKEN is not set on mlair-executor")

    timeout = _env_int("VETAI_HTTP_TIMEOUT_SECONDS", 30)
    poll_seconds = max(1, _env_int("VETAI_TRAINING_POLL_SECONDS", 5))
    max_wait = max(10, _env_int("VETAI_TRAINING_MAX_WAIT_SECONDS", 3600))

    _log(f"phase={phase} stdin context keys: {list(plugin_context.keys())}")

    run_id = _extract_run_id(plugin_context)
    _log(f"phase={phase} extracted run_id={run_id}")

    body = _vetai_phase_context(plugin_context)
    if run_id:
        body["mlair_run_id"] = run_id

    try:
        invoke_resp = _post_json(url, token, body, timeout_seconds=timeout)
    except urllib.error.HTTPError as exc:
        detail = ""
        try:
            detail = (exc.read() or b"")[:2000].decode("utf-8", errors="replace")
        except Exception:
            detail = ""
        raise RuntimeError(f"vet-ai {phase} HTTP {exc.code}: {detail or exc.reason}") from exc

    session_id = invoke_resp.get("session_id")
    if not session_id:
        session_id = run_id or ""
    _log(f"phase={phase} triggered: session_id={session_id}")

    result = _poll_phase_status(session_id, phase, run_id, poll_seconds, max_wait)

    # Ensure final progress is recorded as 100% for this task
    if run_id:
        _mlair_post_run_metric(run_id, f"{phase}_progress_pct", 100.0, 9999)

    # Build output compatible with executor expectations
    params: Dict[str, Any] = {"source": f"vetai_{phase}", "phase": phase}
    metrics: Dict[str, Any] = {}
    artifacts: list = []
    lineage = None

    phase_result = result.get("result")
    if isinstance(phase_result, dict):
        tm = phase_result.get("training_metrics", {})
        for k in ("training_accuracy", "validation_accuracy", "f1_score"):
            v = tm.get(k)
            if v is not None:
                try:
                    metrics[k] = {"step": 1, "value": float(v)}
                except (TypeError, ValueError):
                    continue
        mlair_sync = tm.get("mlair_sync") if isinstance(tm.get("mlair_sync"), dict) else {}
        mv = mlair_sync.get("mlair_version") if isinstance(mlair_sync.get("mlair_version"), dict) else {}
        artifact_uri = mv.get("artifact_uri") or mv.get("uri")
        if artifact_uri:
            artifacts.append({"path": "model", "uri": str(artifact_uri)})
            lineage = {"outputs": [{"name": "model", "version": str(mv.get("version") or ""), "uri": str(artifact_uri)}]}

    return {"params": params, "metrics": metrics, "artifacts": artifacts, "lineage": lineage}


def _run_vetai_data_prep(ctx: Dict[str, Any]) -> Dict[str, Any]:
    return _run_vetai_phase(ctx, "data_prep", "VETAI_DATA_PREP_URL", "/mlops/mlair/data-prep-invoke")


def _run_vetai_model_train(ctx: Dict[str, Any]) -> Dict[str, Any]:
    return _run_vetai_phase(ctx, "model_train", "VETAI_MODEL_TRAIN_URL", "/mlops/mlair/model-train-invoke")


def _run_vetai_validation(ctx: Dict[str, Any]) -> Dict[str, Any]:
    return _run_vetai_phase(ctx, "validation", "VETAI_VALIDATION_URL", "/mlops/mlair/validation-invoke")


def _run_vetai_persist(ctx: Dict[str, Any]) -> Dict[str, Any]:
    return _run_vetai_phase(ctx, "persist", "VETAI_PERSIST_URL", "/mlops/mlair/persist-invoke")


def main() -> int:
    plugin_name = sys.argv[1] if len(sys.argv) > 1 else ""
    raw = sys.stdin.read().strip() or "{}"
    context = json.loads(raw)

    _log(f"plugin_name={plugin_name}")
    _log(f"argv={sys.argv}")

    if plugin_name == "vetai_train_invoke":
        out = _run_vetai_train_invoke(context)
    elif plugin_name == "vetai_train_from_dataset_version":
        out = _run_vetai_dataset_train(context)
    elif plugin_name == "vetai_data_prep":
        out = _run_vetai_data_prep(context)
    elif plugin_name == "vetai_model_train":
        out = _run_vetai_model_train(context)
    elif plugin_name == "vetai_validation":
        out = _run_vetai_validation(context)
    elif plugin_name == "vetai_persist":
        out = _run_vetai_persist(context)
    else:
        _log(f"unknown plugin {plugin_name!r}, returning empty result")
        out = {"params": {}, "metrics": {}, "artifacts": []}

    print(json.dumps(out))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
