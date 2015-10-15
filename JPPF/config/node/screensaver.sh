#! /bin/sh

# this script runs the built-in JPPF screen saver in standalone mode (the node is not started)
# this is provided as a convenience for testing (or other) purposes

java -cp config:lib/* -Xmx64m -Djppf.config=jppf-node.properties -Dlog4j.configuration=log4j-node.properties -Djava.util.logging.config.file=config/logging-node.properties org.jppf.node.screensaver.ScreenSaverMain
