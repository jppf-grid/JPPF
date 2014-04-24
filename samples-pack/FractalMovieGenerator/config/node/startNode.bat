@echo off
call java -cp config;lib/* -Xmx32m -Dlog4j.configuration=log4j-node.properties -Djppf.config=jppf-node.properties -Djava.util.logging.config.file=config/logging-node.properties org.jppf.node.NodeLauncher

rem to start the node without a shell console, uncomment the line below and comment the line above
rem start javaw -cp config;lib/* -Xmx32m -Dlog4j.configuration=log4j-node.properties -Djppf.config=jppf-node.properties -Djava.util.logging.config.file=config/logging-node.properties org.jppf.node.NodeLauncher
