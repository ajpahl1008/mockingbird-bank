#!/bin/zsh

if [ -z "$1" ]; then
  echo "Error: Not enough arguments"
  echo "Usage: createLocalDockerImage.zsh <version>"
  exit 1;
fi

docker buildx build --platform linux/arm64 --no-cache --load --tag ajpahl1008/mockingbird-bank:${1} .

echo "Build Complete: ajpahl1008/mockingbird-bank:${1} "
