#! /bin/sh

export JVM_OPTS=-server -Xmx1024m -cp config:classes:classes-client:../shared/lib/*
export SYS_PROPS=-Dlog4j.configuration=log4j.properties -Djppf.config=jppf.properties -Djava.util.logging.config.file=config/logging.properties
java $JVM_OPTS $SYS_PROPS org.jppf.example.wordcount.WordCountRunner