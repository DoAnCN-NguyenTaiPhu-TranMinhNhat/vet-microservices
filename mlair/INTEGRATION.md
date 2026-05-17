# Vet-AI ↔ MLAir integration

This directory is the **Vet-AI sidecar** for [ml-air](https://github.com/phu142857/ml-air) (canonical docs in your local clone, e.g. `~/ATE/WORK/ml-air`).

MLAir is consumed as **published images** — not as a git submodule. See ml-air:

- `docs/guides/consume-mlair-from-compose.md`
- `docs/guides/reference-integrations.md`
- `docs/guides/configure-tenant-project-scope.md`
- `docs/guides/downstream-model-promote-webhook.md`

## Layout

| Path | Role |
|------|------|
| `custom_plugins/` | Entry points `mlair.plugins` — registry + `validate()` |
| `custom_runner/mlair_ext_runner.py` | Executor subprocess — calls Vet-AI `/mlops/mlair/*` |
| `executor_main_override.py` | Redis queue worker (replaces stock executor `main.py`) |
| `nginx-mlair-browser.conf.template` + `nginx-browser-gateway-entrypoint.sh` | Compose/Podman: UI + `/v1` (auto DNS resolver) |
| `nginx-mlair-browser.conf` | K8s ConfigMap: static upstream (cluster DNS) |
| `mlair-runtime-config.js` | `api_base_url = window.location.origin` |
| `build-local-images.sh` | Build `localhost/ml-air-*:local` from `MLAIR_SRC` |

## Vet-AI HTTP contract

| MLAir plugin | Vet-AI endpoint |
|--------------|-----------------|
| `vetai_train_invoke` | `POST /mlops/mlair/training-invoke` |
| `vetai_train_from_dataset_version` | `POST /mlops/mlair/dataset-train-invoke` |
| `vetai_data_prep` | `POST /mlops/mlair/data-prep-invoke` |
| `vetai_model_train` | `POST /mlops/mlair/model-train-invoke` |
| `vetai_validation` | `POST /mlops/mlair/validation-invoke` |
| `vetai_persist` | `POST /mlops/mlair/persist-invoke` |
| (all phases) | `GET /mlops/mlair/phase-status` |
| Promote callback | `POST /mlops/mlair/promote-webhook` |

Client (registry, feedback buffer, disk import): `vet-ai/ai_service/app/infrastructure/external/mlair_client.py`.

## Environment (Compose)

- **Vet-AI:** `MLAIR_API_BASE_URL=http://mlair-api:8080`, `MLAIR_API_TOKEN` = `ML_AIR_TRACKING_TOKEN`
- **mlair-api:** `MLAIR_MODEL_PROMOTE_WEBHOOK_URL`, `MLAIR_MODEL_PROMOTE_WEBHOOK_BEARER_TOKEN` (not legacy `MLAIR_VETAI_*`)
- **Executor:** `VETAI_*_URL` or `VETAI_BASE_URL` + paths above

## Images

| Môi trường | Cơ chế | Image |
|------------|--------|--------|
| **Local** (mvn + Compose) | `docker-compose.override.yml` hoặc `MLAIR_*_IMAGE` trong `vet-microservices/.env` | `localhost/ml-air-*:local` |
| **AWS EKS** | `vet-infra/.env` → Ansible | `ghcr.io/<MLAIR_IMAGE_OWNER>/ml-air-*:<MLAIR_IMAGE_TAG>` |

Local (khuyến nghị):

```bash
export MLAIR_SRC=/home/teifu142/ATE/WORK/ml-air
./mlair/build-local-images.sh
# Python images: build context = repo root (see api/Dockerfile). Frontend: context = frontend/
cp docker-compose.override.example.yml docker-compose.override.yml
docker compose --profile mlair up -d --force-recreate mlair-api mlair-scheduler mlair-executor mlair-frontend
```

AWS: chỉ chỉnh `vet-infra/env.template` → `.env` (`MLAIR_IMAGE_OWNER`, `MLAIR_IMAGE_TAG`), **không** copy `MLAIR_API_IMAGE` từ microservices.

## EKS

Mirror of this tree: `vet-infra/ansible/mlair/`. Enable with `ENABLE_MLAIR=true` in `vet-infra/.env`.
