@echo off

set JAVA_CP=config;target/classes;target/lib/*
set JAVA_OPTS=-Xmx256m -Dlog4j.configuration=log4j-client.properties -Djppf.config=jppf-client.properties

set JAVA_ARGS=-cp %JAVA_CP% %JAVA_OPTS% org.jppf.ui.monitoring.UILauncher org/jppf/example/fractals/xml/JPPFFractals.xml file

rem *** start the JPPF admin UI with the DOS console,
call java %JAVA_ARGS%

rem *** to start the JPPF admin UI without the DOS console,
rem *** comment the line above and uncomment the line below
rem start javaw %JAVA_ARGS%
