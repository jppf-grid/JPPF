# Embedded Grid demo

<h3>What does the sample do?</h3>
This sample demonstrates how to start and use an embedded JPPF <a href="https://www.jppf.org/doc/6.3/index.php?title=Embedded_driver_and_node#Embedded_driver">driver</a> and
<a href="https://www.jppf.org/doc/6.3/index.php?title=Embedded_driver_and_node#Embedded_node">node</a> programmatically, in the same JVM as the client.

<h3>How do I run it?</h3>
From a command prompt, type the following:
<ul class="samplesList">
  <li>On a Linux/Unix/MacOS system: <b><tt>./run.sh &lt;nbDrivers&gt; &lt;nbNodes&gt;</tt></b></li>
  <li>On a Windows system: <b><tt>run.bat &lt;nbDrivers&gt; &lt;nbNodes&gt;</tt></b></li>
</ul>
Where the following positional arguments are applied:.
<ul class="samplesList">
  <li><i>nbDrivers</i> is the noumber of drivers to start in the embedded grid.
  If there is more than one, every driver will be connected to each of the others
  in a P2P topology. If not specified, it defaults to one driver</li>
  <li><i>nbNodes</i> is the total number of nodes to start in the embedded grid.
  The nodes are assigned to the drivers in round-robin fashion, to make their
  distributio as even as possible. If not specified, it defaults to twwo nodes</li>
</ul>

<p>The demo produces an output put that looks like this, where messages from the demo itself are prefixed with `>>>`:
<pre class="samples" style="white-space: pre-wrap; font-size: 80%">
>>> starting 2 drivers
>>> starting driver 1
driver process id: 8104, uuid: d1
ClientClassServer initialized
NodeClassServer initialized
ClientJobServer initialized
NodeJobServer initialized
management initialized and listening on port 11111
Acceptor initialized
JPPF Driver initialization complete
>>> starting driver 2
driver process id: 8104, uuid: d2
ClientClassServer initialized
NodeClassServer initialized
ClientJobServer initialized
NodeJobServer initialized
management initialized and listening on port 11112
JPPF Driver initialization complete
>>> starting 2 nodes
>>> starting the JPPF node 1
>>> starting the JPPF node 2
>>> starting the JPPF client
Attempting connection to remote peer peer-d1-1@localhost:11111
Reconnected to remote peer peer-d1-1@localhost:11111
client process id: 8104, uuid: C81D5441-D7E3-4DB8-A397-C26C87B68907
node process id: 8104, uuid: 6137686B-A94E-44FB-A571-96D0B12AE08D
node process id: 8104, uuid: 1EA8343C-B7EC-4630-9B9D-004C90F6CAEF
Attempting connection to the class server at localhost:11112
RemoteClassLoaderConnection: Reconnected to the class server
Attempting connection to the class server at localhost:11111
RemoteClassLoaderConnection: Reconnected to the class server
[client: driver-1-1 - ClassServer] Attempting connection to the class server at localhost:11111
[client: driver-2-1 - ClassServer] Attempting connection to the class server at localhost:11112
[client: driver-1-1 - ClassServer] Reconnected to the class server
[client: driver-2-1 - ClassServer] Reconnected to the class server
[client: driver-1-1 - TasksServer] Attempting connection to the task server at localhost:11111
[client: driver-2-1 - TasksServer] Attempting connection to the task server at localhost:11112
JPPF Node management initialized on port 11198
JPPF Node management initialized on port 11199
[client: driver-1-1 - TasksServer] Reconnected to the JPPF task server
Attempting connection to the node server at localhost:11112
Reconnected to the node server
Attempting connection to the node server at localhost:11111
Reconnected to the node server
node successfully initialized
node successfully initialized
[client: driver-2-1 - TasksServer] Reconnected to the JPPF task server
Attempting connection to remote peer peer-d2-1@localhost:11112
Reconnected to remote peer peer-d2-1@localhost:11112
>>> There are now 2 drivers and 2 nodes active in the embedded grid
>>> client connected, now submitting a job with 20 tasks
>>> task-10 says Hello! from Node-1 <<<
>>> task-4 says Hello! from Node-2 <<<
.....
>>> task-6 says Hello! from Node-2 <<<
>>> execution result for job 'embedded grid': 20 successful tasks, 0 in error
>>> shutting down node 1
>>> shutting down node 2
>>> shutting down driver 1
>>> shutting down driver 2
>>> done, exiting program
</pre>

<h3>Demo source code</h3>
<ul class="samplesList">
  <li><a href="src/main/java/org/jppf/example/embedded/EmbeddedGrid.java">EmbeddedGrid.java</a>: fully commented source code for this demo</li>
</ul>

<h3>Documentation references</h3>
<ul class="samplesList">
  <li><a href="https://www.jppf.org/doc/6.3/index.php?title=Embedded_driver_and_node">Embedded driver and node</a></li>
  <li><a href="https://www.jppf.org/doc/6.3/index.php?title=The_JPPF_configuration_API">The JPPF configuration API</a></li>
</ul>

<h3>How can I build the sample?</h3>
To compile the source code, from a command prompt, type: <b>&quot;ant compile&quot;</b><br>
To generate the Javadoc, from a command prompt, type: <b>&quot;ant javadoc&quot;</b>

<h3>I have additional questions and comments, where can I go?</h3>
<p>There are 2 privileged places you can go to:
<ul>
  <li><a href="https://www.jppf.org/forums">The JPPF Forums</a></li>
  <li><a href="https://www.jppf.org/doc/6.2">The JPPF documentation</a></li>
</ul>

