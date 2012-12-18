#! /bin/sh

java -cp config:classes:../shared/lib/*:lib/* -Xmx256m -Djppf.config=jppf-client.properties -Dlog4j.configuration=log4j-client.properties org.jppf.ui.monitoring.UILauncher org/jppf/example/webcrawler/xml/JPPFWebCrawler.xml file
