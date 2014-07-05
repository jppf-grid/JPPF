@echo off
set JAVA_ARGS=-cp config;classes;../shared/lib/* -Xmx256m -Dlog4j.configuration=log4j-client.properties -Djppf.config=jppf-client.properties org.jppf.ui.monitoring.UILauncher org/jppf/example/jaligner/xml/JPPFSequenceAlignment.xml file

rem *** start the JPPF admin UI with the DOS console,
rem call java %JAVA_ARGS%

rem *** to start the JPPF admin UI without the DOS console,
rem *** comment the line above and uncomment the line below
start javaw %JAVA_ARGS%
