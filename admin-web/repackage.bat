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

set current_dir=%~dp0


rmdir /S /Q %current_dir%tmp > nul
mkdir %current_dir%\tmp\WEB-INF\lib

echo copying jar files to %current_dir%tmp\WEB-INF\lib

for %%x in (%*) do (
  if NOT "%1"=="%%x" (
    for %%g in (%%x) do (
      copy %%g %current_dir%tmp\WEB-INF\lib > nul
    )
  )
)

echo updating %1

call jar uf %1 -C %current_dir%tmp .

rmdir /S /Q %current_dir%tmp > nul

echo done