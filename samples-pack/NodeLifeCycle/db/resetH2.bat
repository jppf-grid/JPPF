@echo off

echo deleting the current database
del /F *.db

echo starting the H2 database server
start /B startH2.bat exit_on_close > nul
timeout /t 5

echo creating the jppf_samples database
pushd ..
set JVM_OPTS=-Djppf.config=jppf-client.properties -Dlog4j.configuration=log4j-client.properties
call java -cp config;target/classes;target/lib/* %JVM_OPTS% org.jppf.example.nodelifecycle.client.CreateDB
popd

echo stopping the H2 database server
call stopH2.bat
