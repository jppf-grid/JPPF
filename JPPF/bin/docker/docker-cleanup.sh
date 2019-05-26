#! /bin/sh

# stop all running containers:
docker stop $(docker ps -a -q)

# remove all containers:
docker rm $(docker ps -a -q)

# remove all images:
docker rmi jppf-driver:6.2-alpha
docker rmi jppf-node:6.2-alpha
docker rmi $(docker images -a -q | grep jppf)
#docker rmi $(docker images -a -q)
