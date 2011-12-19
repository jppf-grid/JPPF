#! /bin/sh

java -cp config:classes:../shared/lib/* -Xmx256m org.jppf.example.nbody.NBodyRunner
