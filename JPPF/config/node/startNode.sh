#! /bin/sh

java -cp config:lib/* -Xmx32m -Djppf.config=jppf-node.properties -Dlog4j.configuration=log4j-node.properties -Djava.util.logging.config.file=config/logging-node.properties org.jppf.node.NodeLauncher
