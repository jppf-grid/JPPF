@echo off
set JAVA_ARGS=-cp config;lib/* -Xmx256m -Dlog4j.configuration=log4j-gui.properties -Djppf.config=jppf-gui.properties -Djava.util.logging.config.file=config/logging-gui.properties org.jppf.ui.monitoring.UILauncher org/jppf/ui/options/xml/JPPFAdminTool.xml file

rem *** start the JPPF admin UI with the DOS console,
rem call java %JAVA_ARGS%

rem *** to start the JPPF admin UI without the DOS console,
rem *** comment the line above and uncomment the line below
start javaw %JAVA_ARGS%
