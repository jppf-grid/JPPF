#! /bin/sh

java -cp config:classes:lib/aparapi.jar:../shared/lib/* -Xmx256m -Djava.library.path=lib -Dlog4j.configuration=log4j.properties -Djppf.config=jppf.properties org.jppf.example.aparapi.AparapiRunner
