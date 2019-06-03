#! /bin/sh

# deploy a jppf cluster with docker compose

# export variables defined in .env
set -a
. ./.env
JPPF_SERVER_HOST=$(docker info | grep "Node Address:" | cut -d ':' -f 2 | sed s/[[:space:]]*//)
set +a

# get the docker swarm node id
NODE_ID=$(docker node inspect --pretty self | grep "ID:" | cut -d ':' -f 2 | sed s/[[:space:]]*//)

echo "JPPF v$JPPF_VERSION, node id = '$NODE_ID', driver host = '$JPPF_SERVER_HOST'"

# to force the driver to be deployed on the docker swarm node that deploys the service stack
docker node update --label-add jppf_server_host=$JPPF_SERVER_HOST $NODE_ID

# deploy and run the JPPF cluster
docker stack deploy -c docker-compose.yml jppf
