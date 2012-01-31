@echo off
call java -cp config;lib/* -Xmx16m -Dlog4j.configuration=log4j-driver.properties -Djppf.config=jppf-driver.properties -Djava.util.logging.config.file=config/logging-driver.properties org.jppf.server.DriverLauncher
