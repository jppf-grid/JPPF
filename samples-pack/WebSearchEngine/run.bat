@echo off
set JAVA_ARGS=-Xmx256m -cp config;classes;../shared/lib/*;lib/* -Dlog4j.configuration=log4j-client.properties -Djppf.config=jppf-client.properties org.jppf.ui.monitoring.UILauncher org/jppf/example/webcrawler/xml/JPPFWebCrawler.xml file

rem *** start the sample with the DOS console,
rem call java %JAVA_ARGS%

rem *** to start the sample without the DOS console,
rem *** comment the line above and uncomment the line below
start javaw %JAVA_ARGS%
