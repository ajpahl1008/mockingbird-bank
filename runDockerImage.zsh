#!/bin/zsh

if [ -z "$1" ]; then
  echo "Error: Not enough arguments"
  echo "Usage: runDockerImage.zsh <version>"
  exit 1;
fi


set -a
source .env
set +a

echo "Deleting any previous version of mockingbird-bank "

docker stop mockingbird-bank 2>/dev/null || true
docker rm mockingbird-bank 2>/dev/null || true

echo "Running ajpahl1008/mockingbird-bank:${1} "

# The app always listens on 8080 inside the container (server.port in
# application.yml, overridable via SERVER_PORT) - map the host side to
# whatever you want. Postgres is reached via host.docker.internal in .env,
# so the app container doesn't need to share a Docker network with it.
docker run \
  -p 8090:8080 \
  --name mockingbird-bank \
  --env-file .env \
  ajpahl1008/mockingbird-bank:${1}
