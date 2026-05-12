from __future__ import annotations

from typing import Any, Dict


class VetAiTrainInvokePlugin:
    """
    API-side plugin object for MLAir plugin registry discovery + validate().

    Execution is handled by the executor runner module (see vet-microservices/mlair/custom_runner/mlair_ext_runner.py).
    """

    meta: Dict[str, Any] = {
        "name": "vetai_train_invoke",
        "version": "0.1.0",
        "engine_version": "1.0.0",
        "inputs": {
            "project_id": "string",
            "tenant_id": "string?",
            "training_mode": "string? (local|eks_hybrid)",
            "force": "boolean?",
            "finetune_base_model_version": "string?",
        },
        "outputs": {
            "training_id": "int",
            "vetai_status": "string",
        },
        "ui_schema": None,
        "lineage": None,
    }

    def validate(self, context: dict[str, Any]) -> bool:
        if not isinstance(context, dict):
            raise ValueError("context must be an object")
        pid = str(context.get("project_id") or "").strip()
        if not pid:
            raise ValueError("project_id is required (default_project or clinic_<id>)")
        mode = str(context.get("training_mode") or "local").strip() or "local"
        if mode not in ("local", "eks_hybrid"):
            raise ValueError("training_mode must be local or eks_hybrid")
        return True


class VetAiTrainFromDatasetVersionPlugin:
    """
    Train from an MLAir immutable dataset version (CSV download), not from feedback tables.
    """

    meta: Dict[str, Any] = {
        "name": "vetai_train_from_dataset_version",
        "version": "0.1.0",
        "engine_version": "1.0.0",
        "inputs": {
            "project_id": "string",
            "tenant_id": "string?",
            "dataset_version_id": "string",
            "training_mode": "string? (local|eks_hybrid)",
        },
        "outputs": {"training_id": "int", "model_uri": "string?"},
        "ui_schema": None,
        "lineage": {"inputs": ["dataset_version"], "outputs": ["model"]},
    }

    def validate(self, context: dict[str, Any]) -> bool:
        if not isinstance(context, dict):
            raise ValueError("context must be an object")
        pid = str(context.get("project_id") or "").strip()
        if not pid:
            raise ValueError("project_id is required")
        dvid = str(context.get("dataset_version_id") or "").strip()
        if not dvid:
            raise ValueError("dataset_version_id is required")
        mode = str(context.get("training_mode") or "local").strip() or "local"
        if mode not in ("local", "eks_hybrid"):
            raise ValueError("training_mode must be local or eks_hybrid")
        return True


# ── Multi-phase pipeline plugins ───────────────────────────────────────


class VetAiDataPrepPlugin:
    """Phase 1: data collection + preprocessing."""

    meta: Dict[str, Any] = {
        "name": "vetai_data_prep",
        "version": "0.1.0",
        "engine_version": "1.0.0",
        "inputs": {
            "project_id": "string",
            "tenant_id": "string?",
            "dataset_version_id": "string",
            "training_mode": "string? (local|eks_hybrid)",
        },
        "outputs": {"session_id": "string", "samples": "int"},
        "ui_schema": None,
        "lineage": {"inputs": ["dataset_version"]},
    }

    def validate(self, context: dict[str, Any]) -> bool:
        if not isinstance(context, dict):
            raise ValueError("context must be an object")
        if not str(context.get("project_id") or "").strip():
            raise ValueError("project_id is required")
        if not str(context.get("dataset_version_id") or "").strip():
            raise ValueError("dataset_version_id is required")
        return True


class VetAiModelTrainPlugin:
    """Phase 2: model fitting + calibration."""

    meta: Dict[str, Any] = {
        "name": "vetai_model_train",
        "version": "0.1.0",
        "engine_version": "1.0.0",
        "inputs": {"project_id": "string", "tenant_id": "string?"},
        "outputs": {"session_id": "string"},
        "ui_schema": None,
        "lineage": None,
    }

    def validate(self, context: dict[str, Any]) -> bool:
        if not isinstance(context, dict):
            raise ValueError("context must be an object")
        if not str(context.get("project_id") or "").strip():
            raise ValueError("project_id is required")
        return True


class VetAiValidationPlugin:
    """Phase 3: regression gate + feedback improvement gate."""

    meta: Dict[str, Any] = {
        "name": "vetai_validation",
        "version": "0.1.0",
        "engine_version": "1.0.0",
        "inputs": {"project_id": "string", "tenant_id": "string?"},
        "outputs": {"session_id": "string"},
        "ui_schema": None,
        "lineage": None,
    }

    def validate(self, context: dict[str, Any]) -> bool:
        if not isinstance(context, dict):
            raise ValueError("context must be an object")
        if not str(context.get("project_id") or "").strip():
            raise ValueError("project_id is required")
        return True


class VetAiPersistPlugin:
    """Phase 4: model save, S3 upload, MLflow log, MLAir sync."""

    meta: Dict[str, Any] = {
        "name": "vetai_persist",
        "version": "0.1.0",
        "engine_version": "1.0.0",
        "inputs": {"project_id": "string", "tenant_id": "string?"},
        "outputs": {"model_uri": "string?"},
        "ui_schema": None,
        "lineage": {"outputs": ["model"]},
    }

    def validate(self, context: dict[str, Any]) -> bool:
        if not isinstance(context, dict):
            raise ValueError("context must be an object")
        if not str(context.get("project_id") or "").strip():
            raise ValueError("project_id is required")
        return True

