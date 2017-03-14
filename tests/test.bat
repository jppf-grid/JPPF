@echo off
del /F /Q logs\*.zip
if "%1" == ""  (set PATTERN=**/JPPFSuite.java) else (set PATTERN=%1)
call ant test.pattern -Dpattern=%PATTERN%