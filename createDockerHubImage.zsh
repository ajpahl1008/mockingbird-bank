#!/bin/zsh

if [ -z "$1" ]; then
  echo "Error: Not enough arguments"
  echo "Usage: createDockerHubImage.zsh <version>"
  exit 1;
fi

docker buildx build --platform linux/arm64,linux/amd64 --no-cache --provenance=true --sbom=true \
--push --tag ajpahl1008/mockingbird-bank:${1} .

echo "DockerHub Build Complete: ajpahl/mockingbird-bank:${1} "
