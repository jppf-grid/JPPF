@echo off

call java -cp ../target/lib/* org.h2.tools.Server -tcpShutdown tcp://localhost:9092 -tcpShutdownForce
