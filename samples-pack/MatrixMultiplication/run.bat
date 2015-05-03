@echo off
call java -cp config;classes;../shared/lib/* -Xmx256m -Dlog4j.configuration=log4j-client.properties -Djppf.config=jppf-client.properties org.jppf.example.matrix.MatrixRunner
