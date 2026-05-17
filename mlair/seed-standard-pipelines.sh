#!/usr/bin/env bash
# Seed production MLAir pipelines (idempotent):
#   vetai_train_pipeline       — diagnosis model train (all projects): data_prep → train → validation → persist
#   vetai_clinic_data_pipeline — clinic feedback ETL (clinic_* only): extract → transform → load
set -euo pipefail

T="${MLAIR_TENANT_ID:-default}"
P="${MLAIR_PROJECT_ID:-default_project}"
B="${MLAIR_API_BASE_URL:-http://localhost:18080}"
H="Authorization: Bearer ${MLAIR_API_TOKEN:-admin-token}"
CT="Content-Type: application/json"

TRAIN_PIPE="${MLAIR_VETAI_TRAIN_PIPELINE_ID:-vetai_train_pipeline}"
DATA_PIPE="${MLAIR_CLINIC_DATA_PIPELINE_ID:-${MLAIR_TRAIN_FROM_IMPORT_PIPELINE_ID:-vetai_clinic_data_pipeline}}"
DSV="${MLAIR_SEED_DATASET_VERSION_ID:-}"
CLINIC_PREFIX="${MLAIR_PROJECT_CLINIC_PREFIX:-clinic_}"

ensure_version() {
  local pipe=$1
  local cfg=$2
  local url="${B}/v1/tenants/${T}/projects/${P}/pipelines/${pipe}/versions"
  local list_json
  list_json=$(curl -sS "${url}?limit=1" -H "$H") || {
    echo "ERROR: cannot reach ${B} (start mlair-api or set MLAIR_API_BASE_URL)" >&2
    exit 1
  }
  n=$(echo "$list_json" | python3 -c "import sys,json; d=json.load(sys.stdin); print(len(d.get('items') or []))")
  if [[ "$n" != "0" ]]; then
    echo "==> ${pipe}: version exists (skip)"
    return 0
  fi
  echo "==> ${pipe}: creating version"
  curl -sS -X POST "$url" -H "$H" -H "$CT" -d "{\"config\":${cfg}}" | python3 -m json.tool | head -20
}

if [[ -n "$DSV" ]]; then
  TRAIN_CFG=$(cat <<EOF
{"tasks":[
  {"id":"data_prep","plugin":"vetai_data_prep","context":{"tenant_id":"${T}","project_id":"${P}","training_mode":"local","dataset_version_id":"${DSV}"}},
  {"id":"model_train","plugin":"vetai_model_train","depends_on":["data_prep"]},
  {"id":"validation","plugin":"vetai_validation","depends_on":["model_train"]},
  {"id":"persist","plugin":"vetai_persist","depends_on":["validation"]}
]}
EOF
)
else
  TRAIN_CFG=$(cat <<EOF
{"tasks":[
  {"id":"data_prep","plugin":"vetai_data_prep","context":{"tenant_id":"${T}","project_id":"${P}","training_mode":"local"}},
  {"id":"model_train","plugin":"vetai_model_train","depends_on":["data_prep"]},
  {"id":"validation","plugin":"vetai_validation","depends_on":["model_train"]},
  {"id":"persist","plugin":"vetai_persist","depends_on":["validation"]}
]}
EOF
)

echo "Tenant=${T} project=${P} api=${B}"
ensure_version "$TRAIN_PIPE" "$TRAIN_CFG"

if [[ "$P" == "${CLINIC_PREFIX}"* ]]; then
  CLINIC_ID="${P#${CLINIC_PREFIX}}"
  DATA_CFG=$(cat <<EOF
{"tasks":[
  {"id":"extract","plugin":"vetai_feedback_extract","context":{"tenant_id":"${T}","project_id":"${P}","clinic_id":"${CLINIC_ID}","batch_limit":0}},
  {"id":"transform","plugin":"vetai_feedback_transform","depends_on":["extract"],"context":{"tenant_id":"${T}","project_id":"${P}","clinic_id":"${CLINIC_ID}","batch_limit":0}},
  {"id":"load","plugin":"vetai_feedback_load","depends_on":["transform"],"context":{"tenant_id":"${T}","project_id":"${P}","clinic_id":"${CLINIC_ID}"}}
]}
EOF
)
  ensure_version "$DATA_PIPE" "$DATA_CFG"
else
  echo "==> ${DATA_PIPE}: skipped (not a ${CLINIC_PREFIX}* project)"
fi

echo ""
echo "Production pipelines:"
echo "  1) ${TRAIN_PIPE} — vetai-diagnosis-* model train (Dataset Hub → Train uses model → pipeline mapping)"
echo "  2) ${DATA_PIPE} — clinic doctor feedback ETL (clinic_* projects only; Pipelines → Trigger run)"
echo ""
echo "Hub: http://localhost:38080/pipelines (scope: ${T} / ${P})"
