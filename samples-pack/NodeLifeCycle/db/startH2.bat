@echo off
call java -cp ../lib/h2.jar org.h2.tools.Server -tcp
@if errorlevel 1 pause