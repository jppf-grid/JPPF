#! /bin/bash

d=0
n=0
list=$(kubectl get pod -o=custom-columns=NAME:.metadata.name -n jppf | grep jppf)
echo "list = $list"
for i in ${list}
do
  echo "i = $i"
  name=$i
  if [[ $i = *driver* ]]; then
    d=$(( d + 1 ))
    kubectl logs -n jppf $name > jppf-driver-$d.out.log
  elif [[ $i = *node* ]]; then
    n=$(( n + 1 ))
    kubectl logs -n jppf $name > jppf-node-$n.out.log
  fi
done
