#! /bin/bash

d=0
n=0
a=0
list=$(kubectl get pod -o=custom-columns=NAME:.metadata.name --all-namespaces | grep jppf)
echo "list = $list"
for i in ${list}
do
  echo "i = $i"
  name=$i
  if [[ $i = *driver* ]]; then
    d=$(( d + 1 ))
    $(kubectl -n jppf exec -ti $name cat jppf-driver.log > jppf-driver-$d.log)
  elif [[ $i = *node* ]]; then
    n=$(( n + 1 ))
    $(kubectl -n jppf exec -ti $name cat jppf-node.log > jppf-node-$n.log)
  elif [[ $i = *admin* ]]; then
    a=$(( a + 1 ))
    $(kubectl -n jppf exec -ti $name cat jppf.log > jppf-admin-web-$a.log)
  fi
done
