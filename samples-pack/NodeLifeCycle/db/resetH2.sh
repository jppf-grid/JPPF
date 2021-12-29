#!/bin/sh

echo "deleting the current database"
rm -f *.db

echo "starting the H2 database server"
./startH2.sh &
sleep 5

echo "creating the jppf_samples database"
pushd ..
JVM_OPTS="-Djppf.config=jppf-client.properties -Dlog4j.configuration=log4j-client.properties"
java -cp config:target/classes:target/lib/* $JVM_OPTS org.jppf.example.nodelifecycle.client.CreateDB
popd

echo "stopping the H2 database server"
./stopH2.sh
