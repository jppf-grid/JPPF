@echo off

:: Repeat the specified tests a specified number of times
:: and stop at the first failure, if any
::
:: Parameters:
:: %1 is an Ant fileset pattern for the tests to execute
:: %2 is the number of repeats

if "%1" == ""  goto missing_pattern
if "%2" == ""  (set NB_REPEATS=1) else (set NB_REPEATS=%2)

echo start time: %TIME% > time.tmp

del /F /Q target\logs\*.zip > nul

for /L %%i in (1,1,%NB_REPEATS%) do (
  echo(
  echo ***** Test run %%i *****
  del /F /Q target\*.log > nul
  call mvn surefire:test -Dtest=%1 -q
  if ERRORLEVEL 1 goto end
)

goto end

:missing_pattern
echo you must specify the tests to run as first argument

:end

echo end time:   %TIME% >> time.tmp
echo(
type time.tmp
del /F /Q time.tmp
