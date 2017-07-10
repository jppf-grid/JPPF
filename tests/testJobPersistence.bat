@echo off
del /F /Q logs\*.zip
call ant test.pattern -Dpattern=test/org/jppf/job/persistence/Test*.java