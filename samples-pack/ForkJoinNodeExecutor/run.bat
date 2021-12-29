@echo off

set JAVA_CP=config;target/classes;target/dynamicLibs;target/lib/*
set JAVA_OPTS=-Xmx256m -Djppf.config=jppf.properties -Dlog4j.configuration=log4j.properties -Djava.util.logging.config.file=config/logging.properties

call java -cp %JAVA_CP% %JAVA_OPTS% org.jppf.example.fj.FibonacciFJ
