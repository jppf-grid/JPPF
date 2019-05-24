#! /bin/sh

# display the environment so it can be retrieved with 'docker logs ...'
env

# start the driver
java -cp config:lib/* -Xmx16m -Djppf.config=jppf-driver.properties -Dlog4j.configuration=log4j-driver.properties org.jppf.server.DriverLauncher

# if the driver didn't start, we can still get its full log with 'docker logs ...' 
cat jppf-driver.log
