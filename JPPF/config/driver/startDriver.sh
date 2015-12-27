#! /bin/sh

# compute the path to the Java executable
CONFIG_FILE="config/jppf-driver.properties"
JAVA_PATH=`sed '/^\#/d' $CONFIG_FILE | grep 'jppf.java.path'  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'`
# if parsed path is empty, set the default value
if [ -z $JAVA_PATH ]
then
  JAVA_PATH="java"
fi

$JAVA_PATH -cp config:lib/* -Xmx16m -Djppf.config=jppf-driver.properties -Dlog4j.configuration=log4j-driver.properties -Djava.util.logging.config.file=config/logging-driver.properties org.jppf.server.DriverLauncher
