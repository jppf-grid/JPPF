@echo off
set JAVA_ARGS=-cp config;classes;../shared/lib/* -Xmx256m -Dlog4j.configuration=log4j-client.properties -Djppf.config=jppf-clint.properties org.jppf.ui.monitoring.UILauncher org/jppf/example/fractals/xml/JPPFFractals.xml file

rem *** start the JPPF admin UI with the DOS console,
call java %JAVA_ARGS%

rem *** to start the JPPF admin UI without the DOS console,
rem *** comment the line above and uncomment the line below
rem start javaw %JAVA_ARGS%
