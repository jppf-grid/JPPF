#! /bin/sh

DIR=`dirname $0`

# deploy a jppf cluster with docker stack

# export variables defined in .env
set -a
. $DIR/.env
set +a

echo "deploying JPPF v$JPPF_VERSION"

# deploy and run the JPPF cluster
docker stack deploy -c $DIR/docker-compose.yml jppf
