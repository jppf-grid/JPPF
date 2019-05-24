#! /bin/bash

n=0
list=$(docker ps -a | grep jppf | awk '{printf "%s ",$NF} END {print ""}')
echo "list = $list"
for i in ${list}
do
  echo "i = $i"
  name=$i
  if [[ $i = *driver* ]]; then
    docker exec -ti $name cat jppf-driver.log > jppf-driver.log
  else
    n=$(( n + 1 ))
    $(docker exec -ti $name cat jppf-node.log > jppf-node-$n.log)
  fi
done