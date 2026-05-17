#!/usr/bin/env bash
# Build MLAir images from a local ml-air checkout for vet-microservices Compose.
set -euo pipefail

MLAIR_SRC="${MLAIR_SRC:-/home/teifu142/ATE/WORK/ml-air}"
TAG="${MLAIR_LOCAL_TAG:-local}"

if [[ ! -d "${MLAIR_SRC}/api" ]]; then
  echo "MLAIR_SRC must point to ml-air repo root (api/, scheduler/, …): ${MLAIR_SRC}" >&2
  exit 1
fi

# Python services: Dockerfile expects repo root as context (COPY api/requirements.txt, …).
build_python() {
  local name=$1
  local dockerfile=$2
  echo "==> ${name} (context: ${MLAIR_SRC})"
  docker build -t "localhost/ml-air-${name}:${TAG}" \
    -f "${MLAIR_SRC}/${dockerfile}" \
    "${MLAIR_SRC}"
}

MLAIR_BROWSER_PORT="${MLAIR_FRONTEND_HOST_PORT:-38080}"

build_python api api/Dockerfile
build_python scheduler scheduler/Dockerfile
build_python executor executor/Dockerfile

echo "==> frontend (context: ${MLAIR_SRC}, dockerfile: frontend/Dockerfile)"
docker build -t "localhost/ml-air-frontend:${TAG}" \
  --build-arg "NEXT_PUBLIC_API_BASE_URL=http://localhost:${MLAIR_BROWSER_PORT}" \
  -f "${MLAIR_SRC}/frontend/Dockerfile" \
  "${MLAIR_SRC}"

cat <<EOF

Built:
  localhost/ml-air-api:${TAG}
  localhost/ml-air-scheduler:${TAG}
  localhost/ml-air-executor:${TAG}
  localhost/ml-air-frontend:${TAG}

In vet-microservices/.env:
  MLAIR_API_IMAGE=localhost/ml-air-api:${TAG}
  MLAIR_SCHEDULER_IMAGE=localhost/ml-air-scheduler:${TAG}
  MLAIR_EXECUTOR_IMAGE=localhost/ml-air-executor:${TAG}
  MLAIR_FRONTEND_IMAGE=localhost/ml-air-frontend:${TAG}
  MLAIR_PULL_POLICY=never
EOF
