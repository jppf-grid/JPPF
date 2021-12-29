@echo off
set JVM_OPTS=-server -Xmx1024m -cp config;target/classes;target/lib/*
set SYS_PROPS=-Dlog4j.configuration=log4j.properties -Djppf.config=jppf.properties -Djava.util.logging.config.file=config/logging.properties
call java %JVM_OPTS% %SYS_PROPS% org.jppf.example.wordcount.WordCountRunner
