@echo off

set OPTS=-DskipTests

:: deploy jppf artifacts to Sonatype nexus
call mvn deploy %OPTS% -Pdeployment
