@echo off

:: deploy jppf artifacts to Sonatype nexus
call mvn deploy %JPPF_BUILD_OPTS% -Pdeployment
