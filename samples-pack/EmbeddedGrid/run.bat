@echo off

if "%1" == "" (set NB_DRIVERS=1) else (set NB_DRIVERS=%1)
if "%2" == "" (set NB_NODES=2) else (set NB_NODES=%2)

call java -cp config;classes;../shared/lib/* -Xmx512m -Dlog4j.configuration=log4j.properties org.jppf.example.embedded.EmbeddedGrid %NB_DRIVERSs% %NB_NODES%
