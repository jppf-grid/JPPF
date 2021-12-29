#! /bin/sh

JAVA_CP=config:target/classes:target/lib/*
JVM_OPTS=-Xmx256m -Dlog4j.configuration=log4j-client.properties -Djppf.config=jppf-client.properties

java -cp $JAVA_CP $JVM_OPTS org.jppf.ui.monitoring.UILauncher org/jppf/example/jaligner/xml/JPPFSequenceAlignment.xml file
