@echo off

set JAVA_CP=config;target/classes;target//lib/*
set JVM_OPTS=-Xmx256m -Djppf.config=jppf-client.properties -Dlog4j.configuration=log4j-client.properties
set JAVA_ARGS=-cp %JAVA_CP% %JVM_OPTS% org.jppf.ui.monitoring.UILauncher org/jppf/example/webcrawler/xml/JPPFWebCrawler.xml file

:: start the sample with the DOS console,
::call java %JAVA_ARGS%

:: to start the sample without the DOS console,
:: comment the line above and uncomment the line below
start javaw %JAVA_ARGS%
