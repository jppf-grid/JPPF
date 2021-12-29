#! /bin/sh

JAVA_CP=config:target/classes:target/lib/*
JAVA_OPTS=-Xmx256m -Djppf.config=jppf-client.properties -Dlog4j.configuration=log4j-client.properties

java -cp $JAVA_CP $JAVA_OPTS org.jppf.ui.monitoring.UILauncher org/jppf/example/fractals/xml/JPPFFractals.xml file
