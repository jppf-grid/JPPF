#! /bin/sh


export JAVA_CP=config:classes:../Fractals/classes:lib/monte-cc.jar:../shared/lib/*
export JVM_OPTS=-Xmx512m -Djppf.config=jppf.properties -Dlog4j.configuration=log4j.properties

java -cp $JAVA_CP $JVM_OPS org.jppf.example.fractals.moviegenerator.MovieGenerator $*
