#! /bin/sh

export JAVA_CP=config:classes:../shared/lib/*
export JVM_OPTS=-Xmx256m -Djppf.config=jppf.properties -Dlog4j.configuration=log4j.properties

java -cp $JAVA_CP $JVM_OPS org.jppf.example.concurrentjobs.ConcurrentJobs "$1"
