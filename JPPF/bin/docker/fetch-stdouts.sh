#! /bin/bash

d=0
n=0
list=$(docker ps -a | grep jppf | awk '{printf "%s ",$NF} END {print ""}')
for i in ${list}
do
  echo "i = $i"
  name=$i
  if [[ $i = *driver* ]]; then
    d=$(( d + 1 ))
    docker logs --details $name > jppf-driver-$d.out.log
  elif [[ $i = *node* ]]; then
    n=$(( n + 1 ))
    docker logs --details $name > jppf-node-$n.out.log
  fi
done
