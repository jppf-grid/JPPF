<?php $currentPage="Samples" ?>
$template{name="samples-page-header" title="Initialization Hoook sample"}$

<div align="justify">

					<h3>What does the sample do?</h3>
					<p>This sample demonstrates the use of a node initialization hook to implement a failover mechanism for the connection to the server.
					The hook reads a list of JPPF server addresses from the node configuration, and uses these addresses in the configured order.
					<p>The failover is performed as follows:
					<ol>
						<li>At node startup time, read the list of servers and store it in memory</li>
						<li>Use the first server in the list and attempt to connect</li>
						<li>When the connection fails, the <a href="http://www.jppf.org/doc/v3/index.php?title=Node_configuration#Recovery_and_failover">recovery mechanism</a> will attempt to reconnect to the current server</li>
						<li>If the recovery fails, the current server is put at the end of the list and we get back to step 2</li>
					</ol>

					<h3>How do I run it?</h3>
					Before running this sample, you need to install at least two JPPF server and at least one node.<br>
					For information on how to set up a node and server, please refer to the <a href="http://www.jppf.org/wiki/index.php?title=Introduction">JPPF documentation</a>.<br>
					<p>For convenience, this sample provides 2 configurations for the servers, which you will find in <b>InitializationHook/config/driver1</b> and <b>InitializationHook/config/driver2</b>.
					Additionally, a node configuration is provided in <b>InitializationHook/config/node</b>.<br/>
					Once you have installed the 2 servers and at least one node, perform the following steps:
					<ol>
						<li>Open a command prompt in JPPF-x.y-samples-pack/InitializationHook</li>
						<li>Build the sample: type "<b>ant jar</b>"; this will create a file named <b>InitializationHook.jar</b></li>
						<li>Copy InitializationHook.jar in the "lib" folder of the JPPF node installation, to add it to the node's classpath.</li>
						<li>Replace the node's configuration with the one provided in this sample</li>
						<li>Replace the servers configurations with those provided in this sample</li>
						<li>Start the two drivers</li>
						<li>Start the node. Upon startup you should see the following messages in the node's console:
<pre style="background: #C0C0C0; margin-top: 0px; margin-bottom: 0px">*** found 3 servers ***
  registered server localhost:11111
  registered server localhost:11121
  registered server localhost:11131</pre>
  					</li>
						<li>Kill the first driver (the one listening to port 11111), the node console will display the following:
<pre style="background: #C0C0C0; margin-top: 0px; margin-bottom: 0px">Attempting connection to the node server at localhost:11111
SocketInitializer.initializeSocket(): Could not reconnect to the remote server
Attempting connection to the class server at localhost:11121
Reconnected to the class server
JPPF Node management initialized
Attempting connection to the node server at localhost:11121
Reconnected to the node server
Node successfully initialized</pre>
						The first two lines show the node trying to reconnect to the same driver, and failing to do so. This is the built-in connection recovery mechanism.
						The next lines show the node connecting to the second driver (port 11121), this is our failover mechanism taking place.
						</li>
						<li>Now, start the first driver again, and kill the second driver (port 11121), and you should see the following:
<pre style="background: #C0C0C0; margin-top: 0px; margin-bottom: 0px">Attempting connection to the node server at localhost:11121
SocketInitializer.initializeSocket(): Could not reconnect to the remote server
Attempting connection to the class server at localhost:11131
Attempting connection to the class server at localhost:11111
Reconnected to the class server
JPPF Node management initialized
Attempting connection to the node server at localhost:11111
Reconnected to the node server
Node successfully initialized</pre>
						In the same way as previously, we first see the node attempting to recover the broken connection.
						Then our failover mechanism attempts to connect to a driver listening to port 11131. Since we never started this driver, the connection attempt fails.
						The last lines show the node connecting to the first driver again. The failover mechanism browses the configured servers as if they were in a ring.
						</li>
					</ol>

					<h3>Related source files</h3>
					<ul>
						<li><a href="src/org/jppf/example/initializationhook/DiscoveryHook.java.html">DiscoveryHook.java</a> : this is the implementation of our connection failover mechainsm, via a node initialization hook.</li>
						<li><a href="config/driver1/jppf-driver.properties">driver1/jppf-driver.properties</a> : the configuration of the first driver</li>
						<li><a href="config/driver2/jppf-driver.properties">driver2/jppf-driver.properties</a> : the configuration of the second driver</li>
						<li><a href="config/node/jppf-node.properties">node/jppf-node.properties</a> : the configuration of the node</li>
					</ul>

					<h3>What features of JPPF are demonstrated?</h3>
					Pluggable startup classes for JPPF nodes and servers.<br>
					Dynamic code deployment from the server to a node.
					For a detailed explanation, please refer to the related documentation in the
					<a href="http://www.jppf.org/wiki/index.php?title=Extending_and_Customizing_JPPF#JPPF_startup_classes">Extending and Customizing JPPF &gt; Receiving node connection events in the server</a> section.

					<h3>I have additional questions and comments, where can I go?</h3>
					<p>If you need more insight into the code of this demo, you can consult the Java source files located in the <b>CustomMBeans/src</b> folder.
					<p>In addition, There are 2 privileged places you can go to:
					<ul>
						<li><a href="http://www.jppf.org/forums"/>The JPPF Forums</a></li>
						<li><a href="http://www.jppf.org/wiki">The JPPF documentation</a></li>
					</ul>
					
</div>

$template{name="about-page-footer"}$
