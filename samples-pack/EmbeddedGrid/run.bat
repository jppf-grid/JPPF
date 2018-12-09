@echo off
call java -cp config;classes;../shared/lib/* -Xmx512m -Dlog4j.configuration=log4j.properties org.jppf.example.embedded.EmbeddedGrid
