#! /bin/sh

java -cp config:lib/* -Xmx16m -Djppf.config=jppf-driver.properties -Dlog4j.configuration=log4j-driver.properties org.jppf.server.DriverLauncher
