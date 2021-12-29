@echo off

set JAVA_CP=config;target/classes;target/lib/*
set JVM_OPTS=-Xmx256m -Dlog4j.configuration=log4j-client.properties -Djppf.config=jppf-client.properties

call java -cp %JAVA_CP% %JVM_OPTS% org.jppf.example.matrix.MatrixRunner
