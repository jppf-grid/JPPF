@echo off
if %2 == "" (set iter=1) else (set iter=%2)

for /L %%i in (1,1,%iter%) do (
  echo run #%%i 
  call ant test.pattern -Dpattern=%1
  echo errorlevel = %ERRORLEVEL%
  if %ERRORLEVEL% NEQ 0 (goto:eof)
)

:eof