#! /bin/sh

JAVA_CP=config:target/classes:target/lib/*
JVM_OPTS=-Xmx256m -Dlog4j.configuration=log4j.properties -Djppf.config=jppf.properties

java -cp $JAVA_CP $JVM_OPTS org.jppf.example.jobrecovery.Runner
