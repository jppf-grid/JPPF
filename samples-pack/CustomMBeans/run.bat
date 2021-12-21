@echo off

set JAVA_CP=config;target/classes;target/lib/*
set JVM_OPTS=-Xmx256m -Djppf.config=jppf-client.properties -Dlog4j.configuration=log4j-client.properties

echo Testing the custom server MBean
call java -cp %JAVA_CP% %JVM_OPTS% org.jppf.example.driver.test.AvailableProcessorsMBeanTest

echo Testing the custom node MBean
call java -cp %JAVA_CP% %JVM_OPTS% org.jppf.example.node.test.AvailableProcessorsMBeanTest
