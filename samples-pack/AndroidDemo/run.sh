#! /bin/sh

JAVA_CP=config:classes:../shared/lib/*
JVM_OPTS=-Xmx256m -Djppf.config=jppf.properties -Dlog4j.configuration=log4j.properties

java -cp $JAVA_CP $JVM_OPTS org.jppf.example.android.demo.Runner
