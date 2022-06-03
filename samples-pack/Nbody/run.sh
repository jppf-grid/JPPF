#! /bin/sh

java -cp config:target/classes:target/lib/* -Xmx256m org.jppf.example.nbody.NBodyRunner
