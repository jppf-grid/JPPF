#! /bin/bash

set -ex

helm_repo="../../JPPF/docs/home/helm-charts"

if [ -z $1 ]; then
  version="6.2-alpha"
else
  version=$1
fi

helm package --version $version jppf
mv -f jppf-$version.tgz $helm_repo
helm repo index $helm_repo
