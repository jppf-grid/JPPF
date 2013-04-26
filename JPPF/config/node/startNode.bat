@echo off
call java -cp config;lib/* -Xmx32m -Dlog4j.configuration=log4j-node.properties -Djppf.config=jppf-node.properties -Djava.util.logging.config.file=config/logging-node.properties org.jppf.node.NodeLauncher
