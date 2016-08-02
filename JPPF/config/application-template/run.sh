#! /bin/sh

# compute the path to the Java executable
CONFIG_FILE="config/jppf.properties"
JAVA_PATH=`sed '/^\#/d' $CONFIG_FILE | grep 'jppf.java.path'  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'`
# if parsed path is empty, set the default value
if [ -z $JAVA_PATH ]
then
  JAVA_PATH="java"
fi

$JAVA_PATH -cp config:classes:lib/* -Xmx64m -Dlog4j.configuration=log4j.properties -Djppf.config=jppf.properties -Djava.util.logging.config.file=config/logging.properties org.jppf.application.template.TemplateApplicationRunner
