#! /bin/sh

java -cp config:lib/* -Xmx256m -Djppf.config=jppf-gui.properties -Dlog4j.configuration=log4j-gui.properties -Djava.util.logging.config.file=config/logging-gui.properties org.jppf.ui.monitoring.UILauncher org/jppf/ui/options/xml/JPPFAdminTool.xml file
