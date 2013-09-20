@echo off
call java -cp config;classes;../shared/lib/* -Dlog4j.configuration=log4j-client.properties -Djppf.config=jppf-client.properties org.jppf.example.tasknotifications.test.TaskNotificationsRunner
