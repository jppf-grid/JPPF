#! /bin/bash

n=0
list=$(docker ps -a | grep jppf | awk '{printf "%s ",$NF} END {print ""}')
for i in ${list}
do
  echo "i = $i"
  name=$i
  if [[ $i = *driver* ]]; then
    docker logs --details $name > jppf-driver.out.log
  else
    n=$(( n + 1 ))
    docker logs --details $name > jppf-node-$n.out.log
  fi
done