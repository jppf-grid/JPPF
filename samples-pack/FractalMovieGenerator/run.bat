@echo off

set JAVA_CP=config;classes;../Fractals/classes;lib/monte-cc.jar;../shared/lib/*
set JVM_OPTS=-Xmx512m -Djppf.config=jppf.properties -Dlog4j.configuration=log4j.properties

call java -cp %JAVA_CP% %JVM_OPTS% org.jppf.example.fractals.moviegenerator.MovieGenerator %*
