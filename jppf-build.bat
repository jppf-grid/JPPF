@echo off

:: build and tests
call mvn clean install %JPPF_BUILD_OPTS%
if not "%errorlevel%"=="0" goto end

:: aggregated javadoc
call mvn javadoc:aggregate %JPPF_BUILD_OPTS% -Pno-samples
if not "%errorlevel%"=="0" goto end

:: package redistributable zips
call mvn package %JPPF_BUILD_OPTS% -Pjppf-release
if not "%errorlevel%"=="0" goto end

:end