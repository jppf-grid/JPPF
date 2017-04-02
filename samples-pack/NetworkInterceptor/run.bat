@echo off

set JAVA_CP=config;NetworkInterceptor.jar;classes;../shared/lib/*
set JVM_OPTS=-Xmx256m -Djppf.config=jppf.properties -Dlog4j.configuration=log4j.properties -Djppf.user.name=%1

call java -cp %JAVA_CP% %JVM_OPTS% org.jppf.example.interceptor.NetworkInterceptorDemo
