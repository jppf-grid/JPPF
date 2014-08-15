@echo off

set JAVA_CP=config;classes;../shared/lib/*
set JVM_OPTS=-Xmx256m -Djppf.config=jppf.properties -Dlog4j.configuration=log4j.properties

call java -cp %JAVA_CP% %JVM_OPTS% org.jppf.example.concurrentjobs.ConcurrentJobs %1
