#! /bin/sh

java -cp config:classes:../shared/lib/* -Xmx256m -Djppf.config=jppf-gui.properties -Dlog4j.configuration=log4j-gui.properties org.jppf.ui.monitoring.UILauncher org/jppf/example/fractals/xml/JPPFFractals.xml file
