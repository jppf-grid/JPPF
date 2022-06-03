@echo off

call java -cp ../target/lib/* org.h2.tools.Server -tcp -tcpAllowOthers

if "exit_on_close"=="%1" exit