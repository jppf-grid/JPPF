@echo off
set JAVA_ARGS=-cp config;classes;../shared/lib/* -Xmx256m -Dlog4j.configuration=log4j.properties -Djppf.config=jppf.properties org.jppf.examples.jobrecovery.Runner

rem *** start the JPPF admin UI with the DOS console,
rem call java %JAVA_ARGS%

rem *** to start the JPPF admin UI without the DOS console,
rem *** comment the line above and uncomment the line below
start javaw %JAVA_ARGS%
