@echo off

set JAVA_CP=config;classes;../shared/lib/*
set JVM_OPTS=-Xmx256m -Dlog4j.configuration=log4j.properties

call java -cp %JAVA_CP% %JVM_OPTS% org.jppf.example.configuration.ConfigurationHTMLPrinter
