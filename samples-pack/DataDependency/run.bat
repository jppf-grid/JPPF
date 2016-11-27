@echo off
call java -cp config;classes;lib/*;../shared/lib/* -Djppf.config=jppf.properties -Dlog4j.configuration=log4j.properties org.jppf.example.datadependency.DataDependency
