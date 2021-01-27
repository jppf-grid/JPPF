@echo off

if "%1" == "" (set NB_NODES=2) else (set NB_NODES=%1)

call java -cp config;classes;../shared/lib/* -Xmx512m -Dlog4j.configuration=log4j.properties org.jppf.example.embedded.EmbeddedGrid %NB_NODES%
