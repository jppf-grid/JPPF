#!/bin/sh
java -cp ../lib/h2.jar org.h2.tools.Server -tcpShutdown tcp://localhost:9092 -tcpShutdownForce

