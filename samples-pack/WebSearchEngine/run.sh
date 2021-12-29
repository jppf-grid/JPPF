#! /bin/sh

JAVA_CP=config:target/classes:target/lib/*
JVM_OPTS=-Xmx256m -Djppf.config=jppf-client.properties -Dlog4j.configuration=log4j-client.properties

java -cp $JAVA_CP $JVM_OPTS org.jppf.ui.monitoring.UILauncher org/jppf/example/webcrawler/xml/JPPFWebCrawler.xml file
