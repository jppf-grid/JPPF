@echo off
del /F /Q logs\*.zip
call ant test.pattern -Dpattern=%1