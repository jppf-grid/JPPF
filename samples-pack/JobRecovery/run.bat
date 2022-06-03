@echo off

set JAVA_CP=config;target/classes;target/lib/*
set JVM_OPTS=-Xmx256m -Dlog4j.configuration=log4j.properties -Djppf.config=jppf.properties

call java -cp %JAVA_CP% %JVM_OPTS% org.jppf.example.jobrecovery.Runner
