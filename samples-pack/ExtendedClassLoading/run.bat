@echo off

set JAVA_CP=config;target/classes;target/dynamicLibs;target/lib/*
set SYS_PROPS=-Djppf.config=jppf.properties -Dlog4j.configuration=log4j.properties

call java -cp %JAVA_CP% %SYS_PROPS% org.jppf.example.extendedclassloading.client.MyRunner %*
