#! /bin/sh

#-----------------------------------------------------------------------------------
# Repackage a war file by adding specified jars in its WEB-INF/lib directory
# 
# Parameters:
# - $1 is the path to the war file to repackage
#   example: build/jppf-admin-web-6.0.war
# - all subsequent parameters specify the path to one or more jars to add
#   Wildcards * and ? are allowed
#   example: ~/jppf/lib/mylib.jar ~/jppf/lib2/*.jar 
# 
# Full example usage:
# ./repackage.sh build/jppf-admin-web-6.0.war ~/jppf/lib/mylib.jar ~/jppf/lib2/*.jar 
#-----------------------------------------------------------------------------------

current_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

rm -rf "$current_dir"
mkdir -p "$current_dir"/tmp/WEB-INF/lib

echo copying jar files to $current_dir/tmp/WEB-INF/lib

for p in $*; do
  if "$p" != $1
    cp $p "$current_dir"/tmp/WEB-INF/lib
  fi
done

echo updating $1

jar uf $1 -C $current_dir/tmp .

rm -rf $current_dir
