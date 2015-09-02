<?php $currentPage="Samples" ?>
$template{name="samples-page-header" title="Adaptive Grid demo"}$

<div align="justify" class="blockWithHighlightedTitle" style="padding: 5px">

          <script src="http://www.jppf.org/scripts/code-prettify/run_prettify.js?skin=java"></script>
          <script src="http://www.jppf.org/scripts/code-prettify/lang-conf.js"></script>
          <h3>What does the sample do?</h3>
          <p>This sample dmeonstrates how a JPPF Grid can be grown or shrunk dynamically based on the current workload.
          <p>To achieve this, the demo will adjust two components of the grid topology:
          <ul class="samplesList">
            <li>the number of connections in the <a href="http://www.jppf.org/doc/v4/index.php?title=Connection_pools">JPPF client's connection pool</a></li>
            <li>the number of nodes in the grid, using the <a href="http://www.jppf.org/doc/v4/index.php?title=Node_provisioning">node provisioning facility</a></li>
          </ul>
          <p>The calculations for the number of client connections and the number of nodes are done as follows in a <a href="http://www.jppf.org/doc/v4/index.php?title=Notifications_of_client_job_queue_events">client queue listener</a>:
          <ul class="samplesList">
            <li>the number of connections in the client pool is set to the number of jobs in the client queue, with a configurable maximum allowed</li>
            <li>the number of nodes is computed as one node for every five jobs in the client queue, with a minimum of 1 and a configurable maximum allowed</li>
          </ul>
          <p>The demo itself will start with a single connection and a single node, then run a sequence of job batches, where the number of batches and the number of jobs in each batch are also configurable.
          It will display a message each tile the size of the connection pool or the number of nodes is changed.

          <h3>How do I run it?</h3>
          <p>You will first need to start a JPPF server and one node. For information on how to set up a node and server, please refer to the <a href="http://www.jppf.org/doc/v4/index.php?title=Introduction">JPPF documentation</a>.
          <p>To run the demo, in a command prompt or shell console, type "./run.sh" on Linux or "run.bat" on Windows. You will then see a number of meesages like these while the demo is running:
<pre class="prettyprint lang-txt">
**** submitting jobs batch #2 *****
increasing the number of server connections to 2
increasing the number of server connections to 3
increasing the number of server connections to 4
increasing the number of server connections to 5
increasing the number of nodes to 3
increasing the number of nodes to 4
job 'Job 11 - batch 2 (1/20)' result: execution successful
job 'Job 12 - batch 2 (2/20)' result: execution successful
job 'Job 13 - batch 2 (3/20)' result: execution successful
...
</pre>
          <p>It is also interesting to monitor the grid with the amdinistration console: the topology view will show the slave nodes that are started and stopped,
          and the job data view will show more or less jobs in the server quueue, depending on the client connection pool size.
          <p>You can also play with the parameters of the demo that are configurable in the <a href="config/jppf.properties.html">config/jppf.properties</a> file:
<pre class="prettyprint lang-conf">
# the maximum allowed connection pool size
maxAllowedPoolSize = 10
# the maximum allowed number of nodes
maxAllowedNodes = 10
# defines the number of job batches and their sizes
jobBatches = 1 20 50 10
# duration of each task in millis
taskDuration = 1500
</pre>

          <h3>Source files</h3>
          <ul class="samplesList">
            <li><a href="src/org/jppf/example/adaptivegrid/AdaptiveGridDemo.java.html">AdaptiveGridDemo.java</a>: The entry point for the demo</li>
            <li><a href="src/org/jppf/example/adaptivegrid/DriverConnectionManager.java.html">DriverConnectionManager.java</a>: encapsulates the fuctionality to update the connection pool size and nulber of slave nodes</li>
            <li><a href="src/org/jppf/example/adaptivegrid/MyQueueListener.java.html">MyQueueListener.java</a>: reacts to client queue events by adapting the grid topology</li>
            <li><a href="src/org/jppf/example/adaptivegrid/SimpleTask.java.html">SimpleTask.java</a>: a very simple JPPF task used in this demo</li>
          </ul>

          <h3>I have additional questions and comments, where can I go?</h3>
          <p>If you need more insight into the code of this demo, you can consult the Java source files located in the <b>CustomLoadBalancer/src</b> folder.
          <p>In addition, There are 2 privileged places you can go to:
          <ul>
            <li><a href="http://www.jppf.org/forums"/>The JPPF Forums</a></li>
            <li><a href="http://www.jppf.org/doc/v4/">The JPPF documentation</a></li>
          </ul>
          
</div><br>

$template{name="about-page-footer"}$
