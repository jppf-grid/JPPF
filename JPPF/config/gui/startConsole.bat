@echo off

:: compute the path to the Java executable
set CONFIG_FILE="config\jppf-gui.properties"
:: set the default value
set JAVA_PATH=javaw
:: try and parse from the config file
call:getvalue %CONFIG_FILE% "jppf.java.path" JAVA_PATH


:: *** start the JPPF admin UI
set JAVA_ARGS=-cp config;lib/* -Xmx256m -Dlog4j.configuration=log4j-gui.properties -Djppf.config=jppf-gui.properties -Djava.util.logging.config.file=config/logging-gui.properties org.jppf.ui.console.JPPFAdminConsole
start %JAVA_PATH% %JAVA_ARGS%
goto:eof

:getvalue
:: This function reads a value from a properties file and stores it in a variable
:: %1 = name of properties file to search in
:: %2 = search term to look for
:: %3 = variable to place search result
FOR /F "eol=# tokens=1,2* delims==" %%i in ('findstr /b /l /i %~2= %1') DO set %~3=%%~j
goto:eof