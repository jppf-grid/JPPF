#! /bin/sh

if [ -z $1 ]; then
  NB_NODES=2
else
  NB_NODES=$1
fi

java -cp config:classes:../shared/lib/* -Xmx512m -Dlog4j.configuration=log4j.properties org.jppf.example.embedded.EmbeddedGrid $NB_NODES
