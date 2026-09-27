#!/usr/bin/env zsh
#
# create-postgres-container.zsh
#
# Stands up (or resumes) the local Postgres container used for Mockingbird
# Bank development. Idempotent: safe to run repeatedly.
#
#   ./scripts/create-postgres-container.zsh
#
# Override any of these via the environment before calling the script, e.g.
#   POSTGRES_PASSWORD=changeme ./scripts/create-postgres-container.zsh

set -euo pipefail

CONTAINER_NAME="${CONTAINER_NAME:-postgres-local}"
POSTGRES_IMAGE="${POSTGRES_IMAGE:-postgres:17.2}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
POSTGRES_DB="${POSTGRES_DB:-mydatabase}"
POSTGRES_USER="${POSTGRES_USER:-admin}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-secret}"
VOLUME_NAME="${VOLUME_NAME:-${CONTAINER_NAME}-data}"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker is not installed or not on PATH." >&2
  exit 1
fi

if docker ps --filter "name=^${CONTAINER_NAME}\$" --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}\$"; then
  echo "Container '${CONTAINER_NAME}' is already running."
  exit 0
fi

if docker ps -a --filter "name=^${CONTAINER_NAME}\$" --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}\$"; then
  echo "Container '${CONTAINER_NAME}' exists but is stopped. Starting it..."
  docker start "${CONTAINER_NAME}"
  exit 0
fi

echo "Creating container '${CONTAINER_NAME}' from ${POSTGRES_IMAGE}..."
docker run -d \
  --name "${CONTAINER_NAME}" \
  -e POSTGRES_DB="${POSTGRES_DB}" \
  -e POSTGRES_USER="${POSTGRES_USER}" \
  -e POSTGRES_PASSWORD="${POSTGRES_PASSWORD}" \
  -p "${POSTGRES_PORT}:5432" \
  -v "${VOLUME_NAME}:/var/lib/postgresql/data" \
  "${POSTGRES_IMAGE}"

echo "Container '${CONTAINER_NAME}' created and running on port ${POSTGRES_PORT}."
echo "  Database: ${POSTGRES_DB}"
echo "  User:     ${POSTGRES_USER}"
