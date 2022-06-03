#! /bin/sh

java -cp config:target/classes:target/lib/* -Djppf.config=jppf.properties -Dlog4j.configuration=log4j.properties org.jppf.example.datadependency.DataDependency
