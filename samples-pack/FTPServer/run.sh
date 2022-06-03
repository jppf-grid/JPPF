#! /bin/sh

JAVA_CP=config:target/classes:target/lib/*
JVM_OPTS=-Djppf.config=jppf.properties -Dlog4j.configuration=log4j.properties

java -cp $JAVA_CP $JVM_OPTS org.jppf.example.ftp.runner.FTPRunner
