#! /bin/sh

# display the environment so it can be retrieved with 'docker logs ...'
env | sort

# start the node
java -cp config:lib/* -Xmx16m -Djppf.unquote.env.vars=true -Djppf.config=jppf-node.properties -Dlog4j.configuration=log4j-node.properties org.jppf.node.NodeLauncher

# if the node didn't start, we can still get its full log with 'docker logs ...' 
cat jppf-node.log
