@echo off
call java -cp config;target/classes;target/lib/* -Xmx256m -Djava.library.path=lib -Dlog4j.configuration=log4j.properties -Djppf.config=jppf.properties org.jppf.example.aparapi.AparapiRunner
