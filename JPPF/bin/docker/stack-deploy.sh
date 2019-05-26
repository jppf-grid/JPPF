#! /bin/sh

# deploy a jppf cluster with docker compose

# export variables defined in .env
set -a
. ./.env
JPPF_SERVER_HOST=$(docker info | grep "Node Address:" | cut -d ':' -f 2 | sed s/[[:space:]]*//)
set +a

NODE_ID=$(docker node inspect --pretty self | grep "ID:" | cut -d ':' -f 2 | sed s/[[:space:]]*//)

echo "node id = '$NODE_ID', driver host = '$JPPF_SERVER_HOST'"

docker node update --label-add jppf_server_host=$JPPF_SERVER_HOST $NODE_ID

# deploy and run the JPPF cluster
docker stack deploy -c docker-compose.yml jppf
