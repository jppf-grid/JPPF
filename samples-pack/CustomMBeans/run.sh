#! /bin/sh

export JAVA_CP=config:target/classes:target/lib/*
export JVM_OPTS=-Xmx256m -Djppf.config=jppf-client.properties -Dlog4j.configuration=log4j.properties

echo "Testing the custom server MBean"
java -cp $JAVA_CP $JVM_OPTS org.jppf.example.driver.test.AvailableProcessorsMBeanTest

echo "Testing the custom node MBean"
java -cp $JAVA_CP $JVM_OPTS org.jppf.example.node.test.AvailableProcessorsMBeanTest
