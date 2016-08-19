$template{name="sample-readme-html-header" title="Adaptive Grid demo"}$
<h3>What does the sample do?</h3>
<p>This sample demonstrates how a JPPF Grid can be grown or shrunk dynamically based on the current workload.
<p>To achieve this, the demo will adjust two components of the grid topology:
<ul class="samplesList">
  <li>the number of connections in the <a href="http://www.jppf.org/doc/5.2/index.php?title=Connection_pools">JPPF client's connection pool</a></li>
  <li>the number of nodes in the grid, using the <a href="http://www.jppf.org/doc/5.2/index.php?title=Node_provisioning">node provisioning facility</a></li>
</ul>
<p>The calculations for the number of client connections and the number of nodes are done as follows:
<ul class="samplesList">
  <li>the number of connections in the client pool is set to the number of jobs in the client queue, with a configurable maximum allowed</li>
  <li>the number of nodes is computed as one node for every five jobs in the client queue, with a minimum of 1 and a configurable maximum allowed</li>
</ul>
<p>The demo itself will start with a single connection and a single node, then run a sequence of job batches, where the number of batches and the number of jobs in each batch are also configurable.
It will display a message each tile the size of the connection pool or the number of nodes is changed.

<h3>How do I run it?</h3>
<p>You will first need to start a JPPF server and one node. For information on how to set up a node and server, please refer to the <a href="http://www.jppf.org/doc/5.2/index.php?title=Introduction">JPPF documentation</a>.
<p>To run the demo, in a command prompt or shell console, type "./run.sh" on Linux or "run.bat" on Windows. You will then see a number of meesages like these while the demo is running:
<pre class="prettyprint lang-txt">
[demo] Starting the demo with maxAllowedNodes=10 and maxAllowedPoolSize=10
[demo] **** submitting jobs batch #1 (1 jobs) *****
[demo] job 'Job 1 - batch 1 (1/1)' result: execution successful
[demo] **** submitting jobs batch #2 (20 jobs) *****
[demo] increasing the number of server connections to 10
[demo] increasing the number of nodes to 5
[demo] job 'Job 2 - batch 2 (1/20)' result: execution successful
[demo] ...
[demo] job 'Job 21 - batch 2 (20/20)' result: execution successful
[demo] **** submitting jobs batch #3 (50 jobs) *****
[demo] increasing the number of nodes to 10
[demo] job 'Job 22 - batch 3 (1/50)' result: execution successful
[demo] ...
[demo] job 'Job 71 - batch 3 (50/50)' result: execution successful
[demo] **** submitting jobs batch #4 (8 jobs) *****
[demo] decreasing the number of server connections to 8
[demo] decreasing the number of nodes to 2
[demo] job 'Job 72 - batch 4 (1/8)' result: execution successful
[demo] ...
[demo] job 'Job 79 - batch 4 (8/8)' result: execution successful
[demo] demo has completed, resetting to initial grid configuration
[demo] decreasing the number of server connections to 1
[demo] decreasing the number of nodes to 1
</pre>
          <p>It is also interesting to monitor the grid with the amdinistration console: the topology view will show the slave nodes that are started and stopped,
          and the job data view will show more or less jobs in the server queue, depending on the client connection pool size.
          <p>You can also play with the parameters of the demo that are configurable in the <a href="config/jppf.properties.html">config/jppf.properties</a> file:
<pre class="prettyprint lang-conf">
# the maximum allowed connection pool size
maxAllowedPoolSize = 10
# the maximum allowed number of nodes
maxAllowedNodes = 10
# defines the number of job batches and their sizes
jobBatches = 1 20 50 8
# duration of each task in millis
taskDuration = 1500
</pre>

<h3>Source files</h3>
<ul class="samplesList">
  <li><a href="src/org/jppf/example/adaptivegrid/AdaptiveGridDemo.java.html">AdaptiveGridDemo.java</a>: The entry point for the demo</li>
  <li><a href="src/org/jppf/example/adaptivegrid/DriverConnectionManager.java.html">DriverConnectionManager.java</a>: encapsulates the fuctionality to update the connection pool size and number of slave nodes</li>
  <li><a href="src/org/jppf/example/adaptivegrid/SimpleTask.java.html">SimpleTask.java</a>: a very simple JPPF task used in this demo</li>
</ul>

<h3>I have additional questions and comments, where can I go?</h3>
<p>If you need more insight into the code of this demo, you can consult the Java source files located in the <b>AdaptiveGrid/src</b> folder.
<p>In addition, There are 2 privileged places you can go to:
<ul>
  <li><a href="http://www.jppf.org/forums"/>The JPPF Forums</a></li>
  <li><a href="http://www.jppf.org/doc/5.2/">The JPPF documentation</a></li>
</ul>
$template{name="sample-readme-html-footer"}$
