@echo off

::--------------------------------------------------------------------------------
:: Repackage a war file by adding specified jars in its WEB-INF/lib directory
:: 
:: Parameters:
:: - %1 is the path to the war file to repackage
::   example: build/jppf-admin-web-6.0.war
:: - all subsequent parameters specify the path to one or more jars to add
::   Wildcards * and ? are allowed
::   example: C:\jppf\lib\mylib.jar C:\jppf\lib2\*.jar 
:: 
:: Full example usage:
:: repackage build\jppf-admin-web-6.0.war C:\jppf\lib\mylib.jar C:\jppf\lib2\*.jar 
::--------------------------------------------------------------------------------

set SCRIPT_PATH=%~dp0
set TEMP_FOLDER=%SCRIPT_PATH%tmp
set LIB_FOLDER=%TEMP_FOLDER%\WEB-INF\lib

:: if no parameter, print usage and exit
if "%1"=="" goto usage

rmdir /S /Q %TEMP_FOLDER% > nul
mkdir %LIB_FOLDER%

echo copying jar files to %LIB_FOLDER%

for %%p in (%*) do (
  if NOT "%1"=="%%p" (
    copy %%p %LIB_FOLDER% > nul
  )
)

echo updating %1

call jar uf %1 -C %TEMP_FOLDER% .

rmdir /S /Q %TEMP_FOLDER% > nul
goto end

:usage
echo Repackage a war file by adding specified jars in its WEB-INF/lib directory
echo(
echo Usage:
echo   repackage.bat ^<war_location^> ^<jar_location_1^> ... ^<jar_location_n^>
echo where:
echo - war_location is the path to the war file to repackage
echo - jar_location_x is the path to one or more jar files to add to the war's WEB-INF/lib folder
echo   wildcards * and ? are accepted
echo(
echo Example:
echo   repackage.bat build\jppf-admin-web-6.0.war C:\jppf\lib\mylib.jar C:\jppf\lib2\*.jar 

:end
