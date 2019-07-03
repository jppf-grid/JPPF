#! /bin/bash

d=0
n=0
a=0
list=$(docker ps -a | grep jppf | awk '{printf "%s ",$NF} END {print ""}')
echo "list = $list"
for i in ${list}
do
  echo "i = $i"
  name=$i
  if [[ $i = *driver* ]]; then
    d=$(( d + 1 ))
    #docker exec -ti $name cat jppf-driver.log > jppf-driver.log
    $(docker exec -ti $name cat jppf-driver.log > jppf-driver-$d.log)
  elif [[ $i = *node* ]]; then
    n=$(( n + 1 ))
    $(docker exec -ti $name cat jppf-node.log > jppf-node-$n.log)
  elif [[ $i = *admin* ]]; then
    a=$(( a + 1 ))
    $(docker exec -ti $name cat jppf.log > jppf-admin-web-$a.log)
  fi
done
