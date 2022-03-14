@echo off

set OPTS=-DskipTests

:: build and tests
call mvn clean install %OPTS%
:: aggregted javadoc
call mvn javadoc:aggregate %OPTS% -Pno-samples
:: package redistributable zips
call mvn package %OPTS% -Pjppf-release
