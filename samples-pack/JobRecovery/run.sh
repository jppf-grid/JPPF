#! /bin/sh

java -cp config:classes:../shared/lib/* -Xmx256m -Dlog4j.configuration=log4j.properties -Djppf.config=jppf.properties org.jppf.example.jobrecovery.Runner
