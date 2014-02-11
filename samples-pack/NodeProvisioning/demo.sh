#! /bin/sh

export CLIENT_CP=classes/client:../shared/lib/jppf-client.jar:../shared/lib/jppf-common.jar:node-dist/lib/*
java -cp $CLIENT_CP -Xmx256m -Dlog4j.configuration=log4j.properties -Djppf.config=jppf.properties org.jppf.example.provisioning.client.Runner
