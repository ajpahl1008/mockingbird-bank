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

docker stop /mockingbird-bank
docker rm /mockingbird-bank

echo "Running ajpahl1008/mockingbird-bank:${1} "

docker run \
  -p 8090:8090 \
  --name mockingbird-bank \
  --network mockingbird-bank-network \
  --env-file .env \
  ajpahl1008/mockingbird-bank:${1}
