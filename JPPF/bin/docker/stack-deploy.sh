#! /bin/sh

# deploy a jppf cluster with docker swarm

# export variables defined in .env
set -a
. ./.env
set +a

# deploy and run JPPF cluster
docker stack deploy -c docker-compose.yml jppf
