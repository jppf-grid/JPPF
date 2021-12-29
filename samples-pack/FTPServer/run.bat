@echo off

set JAVA_CP=config;target/classes;target/lib/*
set JVM_OPTS=-Djppf.config=jppf.properties -Dlog4j.configuration=log4j.properties

call java -cp %JAVA_CP% %JVM_OPTS% org.jppf.example.ftp.runner.FTPRunner
