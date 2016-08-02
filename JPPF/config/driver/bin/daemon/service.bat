@echo off
rem Licensed to the Apache Software Foundation (ASF) under one or more
rem contributor license agreements.  See the NOTICE file distributed with
rem this work for additional information regarding copyright ownership.
rem The ASF licenses this file to You under the Apache License, Version 2.0
rem (the "License"); you may not use this file except in compliance with
rem the License.  You may obtain a copy of the License at
rem
rem     http://www.apache.org/licenses/LICENSE-2.0
rem
rem Unless required by applicable law or agreed to in writing, software
rem distributed under the License is distributed on an "AS IS" BASIS,
rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem See the License for the specific language governing permissions and
rem limitations under the License.

rem ---------------------------------------------------------------------------
rem NT Service Install/Uninstall script
rem
rem Options
rem install                Install the service using JPFPDriver as service name.
rem                        Service is installed using default settings.
rem remove                 Remove the service from the System.
rem
rem name        (optional) If the second argument is present it is considered
rem                        to be new service name
rem ---------------------------------------------------------------------------

rem ---------------------------------------------------------------------------
rem User configurable options
rem ---------------------------------------------------------------------------
rem must point to the root installation folder of a JDK or JRE
set JRE_HOME=%JAVA_HOME%
rem JVM architecture, must be one of 32, 64, ia64
set ARCH=64
rem add more jars and class folders to the default classpath
set ADDITIONAL_CP=

rem User should not need to edit below this line
setlocal
set JPPF_COMP=Driver
set SERVICE_NAME=JPPF%JPPF_COMP%
set DISPLAYNAME=JPPF %JPPF_COMP%
set "DESCRIPTION=A JPPF %JPPF_COMP% installed as a Windows service"
set "SELF=%~dp0%service.bat"
set "BIN_DIR=%~dp0"
set "JPPF_HOME=%BIN_DIR%..\.."
set "CURRENT_DIR=%cd%"
set "EXECUTABLE=%BIN_DIR%JPPF%JPPF_COMP%-%ARCH%.exe"

if "x%1x" == "xx" goto displayUsage
set SERVICE_CMD=%1
shift
if "x%1x" == "xx" goto checkServiceCmd
:checkUser
if "x%1x" == "x/userx" goto runAsUser
if "x%1x" == "x--userx" goto runAsUser
set SERVICE_NAME=%1
set DISPLAYNAME=JPPF Driver %1
shift
if "x%1x" == "xx" goto checkServiceCmd
goto checkUser
:runAsUser
shift
if "x%1x" == "xx" goto displayUsage
set SERVICE_USER=%1
shift
runas /env /savecred /user:%SERVICE_USER% "%COMSPEC% /K \"%SELF%\" %SERVICE_CMD% %SERVICE_NAME%"
goto end
:checkServiceCmd
if /i %SERVICE_CMD% == install goto doInstall
if /i %SERVICE_CMD% == remove goto doRemove
if /i %SERVICE_CMD% == uninstall goto doRemove
echo Unknown parameter "%SERVICE_CMD%"

:displayUsage
echo.
echo Usage: service.bat install/remove [service_name] [/user username]
goto end

:doRemove
rem Remove the service
echo Removing the service '%SERVICE_NAME%' ...

"%EXECUTABLE%" //DS//%SERVICE_NAME% ^
    --LogPath "%JPPF_HOME%\logs"
if not errorlevel 1 goto removed
echo Failed removing '%SERVICE_NAME%' service
goto end
:removed
echo The service '%SERVICE_NAME%' has been removed
goto end

:doInstall
rem Install the service
echo Installing the service '%SERVICE_NAME%' ...
echo Using JPPF_HOME:       "%JPPF_HOME%"
echo Using JRE_HOME:        "%JRE_HOME%"

rem Try to use the server jvm
set "JVM=%JRE_HOME%\bin\server\jvm.dll"
if exist "%JVM%" goto foundJvm
rem Try to use the client jvm
set "JVM=%JRE_HOME%\bin\client\jvm.dll"
if exist "%JVM%" goto foundJvm
rem Assume JDK instead of JRE and try to use the server jvm
set "JVM=%JRE_HOME%\jre\bin\server\jvm.dll"
if exist "%JVM%" goto foundJvm
rem Assume JDK instead of JRE and try to use the client jvm
set "JVM=%JRE_HOME%\jre\bin\client\jvm.dll"
if exist "%JVM%" goto foundJvm
echo Warning: Neither 'server' nor 'client' jvm.dll was found at JRE_HOME.
set JVM=auto
:foundJvm
echo Using JVM:             "%JVM%"

"%EXECUTABLE%" //IS//%SERVICE_NAME% ^
    --Description "%DESCRIPTION%" ^
    --DisplayName "%DISPLAYNAME%" ^
    --Install "%EXECUTABLE%" ^
    --LogPath "%JPPF_HOME%\logs" ^
    --StdOutput auto ^
    --StdError auto ^
    --Classpath "%JPPF_HOME%\config;%JPPF_HOME%\lib\*;%ADDITIONAL_CP%" ^
    --Jvm "%JVM%" ^
    --StartMode jvm ^
    --StopMode jvm ^
    --StartPath "%JPPF_HOME%" ^
    --StopPath "%JPPF_HOME%" ^
    --StartClass org.jppf.server.DriverLauncher ^
    --StopClass java.lang.System ^
    ++JvmOptions "-Dlog4j.configuration=log4j-driver.properties" ^
    ++JvmOptions "-Djppf.config=jppf-driver.properties" ^
    ++JvmOptions "-Djava.util.logging.config.file=config/logging-driver.properties" ^
    --JvmMs 16 ^
    --JvmMx 32 ^
    --Startup auto
if not errorlevel 1 goto installed
echo Failed installing '%SERVICE_NAME%' service
goto end
:installed
echo The service '%SERVICE_NAME%' has been installed.

:end
cd "%CURRENT_DIR%"
