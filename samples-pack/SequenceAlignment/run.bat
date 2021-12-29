@echo off

set JAVA_CP=config;target/classes;target/lib/*
set JVM_OPTS=-Xmx256m -Dlog4j.configuration=log4j-client.properties -Djppf.config=jppf-client.properties
set JAVA_ARGS=-cp %JAVA_CP% %JVM_OPTS% org.jppf.ui.monitoring.UILauncher org/jppf/example/jaligner/xml/JPPFSequenceAlignment.xml file

:: start the JPPF admin UI with the DOS console,
::call java %JAVA_ARGS%

:: to start the JPPF admin UI without the DOS console,
:: comment the line above and uncomment the line below
start javaw %JAVA_ARGS%
