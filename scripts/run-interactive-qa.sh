#!/usr/bin/env bash
# Drives the real, running app through an actual browser (Playwright) the way
# a person would: sign in with the seeded demo login, look at the dashboard,
# open an account, sign out. Unlike the unit/integration test suite, this
# exercises the built frontend and a live HTTP session end to end.
#
# Fully self-contained: always starts its own throwaway Postgres on a
# dedicated port (5433, not 5432) rather than pointing at whatever a
# developer already has running locally - a shared local Postgres may carry
# unrelated schemas/tables from other projects, and Flyway's
# baseline-on-migrate (see application.yml) would silently treat that as an
# already-migrated Mockingbird Bank schema. Everything this script starts, it
# also tears down.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

QA_PORT=5433
QA_CONTAINER="mockingbird-qa-postgres"
APP_LOG="$(mktemp -t mockingbird-qa-app)"
APP_PID=""

cleanup() {
    local exit_code=$?
    if [[ -n "$APP_PID" ]] && kill -0 "$APP_PID" 2>/dev/null; then
        echo "Stopping app (pid $APP_PID)..."
        kill "$APP_PID" 2>/dev/null || true
        wait "$APP_PID" 2>/dev/null || true
    fi
    echo "Removing throwaway Postgres container ($QA_CONTAINER)..."
    docker rm -f "$QA_CONTAINER" >/dev/null 2>&1 || true
    if [[ $exit_code -ne 0 ]]; then
        echo
        echo "App log ($APP_LOG):"
        tail -n 80 "$APP_LOG" || true
    else
        rm -f "$APP_LOG"
    fi
    exit $exit_code
}
trap cleanup EXIT

echo "== Interactive QA: Mockingbird Bank =="

docker rm -f "$QA_CONTAINER" >/dev/null 2>&1 || true
echo "Starting a throwaway Postgres on port $QA_PORT..."
docker run --rm -d --name "$QA_CONTAINER" \
    -e POSTGRES_USER=admin -e POSTGRES_PASSWORD=secret -e POSTGRES_DB=mydatabase \
    -p "$QA_PORT":5432 postgres:17.2 >/dev/null

for _ in $(seq 1 30); do
    if docker exec "$QA_CONTAINER" pg_isready -U admin >/dev/null 2>&1; then
        break
    fi
    sleep 1
done

echo "Starting the app (./gradlew bootRun) against it..."
SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:${QA_PORT}/mydatabase" \
    SPRING_DATASOURCE_USERNAME=admin \
    SPRING_DATASOURCE_PASSWORD=secret \
    ./gradlew bootRun >"$APP_LOG" 2>&1 &
APP_PID=$!

echo "Waiting for http://localhost:8080/actuator/health..."
READY=false
for _ in $(seq 1 90); do
    if curl -sf http://localhost:8080/actuator/health 2>/dev/null | grep -q '"status":"UP"'; then
        READY=true
        break
    fi
    if ! kill -0 "$APP_PID" 2>/dev/null; then
        echo "App process exited before becoming healthy."
        break
    fi
    sleep 2
done

if [[ "$READY" != true ]]; then
    echo "App never became healthy within the timeout."
    exit 1
fi
echo "App is up."

echo "Running Playwright QA suite..."
cd "$ROOT_DIR/qa"
if [[ ! -d node_modules ]]; then
    npm install
fi
npx playwright test
