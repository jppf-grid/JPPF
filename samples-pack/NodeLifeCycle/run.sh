#! /bin/sh

export JAVA_CP=config:target/classes:target/lib/*
export JVM_OPTS=-Djppf.config=jppf-client.properties -Dlog4j.configuration=log4j-client.properties -Djava.util.logging.config.file=config/logging.properties

java -cp $JAVA_CP $JVM_OPTS org.jppf.example.nodelifecycle.client.DBRunner
