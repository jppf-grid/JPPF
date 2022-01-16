# Custom Load Balancer demo

<h3>What does the sample do?</h3>
This sample illustrates the implementation of a <a href="https://www.jppf.org/doc/6.3/index.php?title=Creating_a_custom_load-balancer">custom load-balancer</a> that uses information about the nodes and the submitted jobs, to dispatch JPPF tasks to the nodes.

<h3>Description of the problem to solve</h3>
We are in a situation where we need to submit two types of jobs for execution in the JPPF grid:
<ul class="samplesList">
  <li>"heavy jobs", made of a small number of long-lived tasks with a large memory footprint</li>
  <li>"light jobs", made of a large number of short-lived tasks with a small memory footprint</li>
</ul>
We want the JPPF server to be able to dispatch the tasks to the apppropriate node(s), based on what it knows of each node's capabilities and on the jobs characteristics.

<h3>Description of the solution</h3>
<p>To resolve this problem, we will use a combination of a custom load balancer that is aware of the node's capabilities, execution policies for the jobs, and job metadata the load-balancer will be able to use.
<p>a) Information provided by each job (job metadata):
<ul class="samplesList">
  <li>"task.memory": the maximum size in bytes neded by each task in the job</li>
  <li>"task.time": the maximum duration of each task in the job</li>
  <li>"allowed.time": the maximum allowed time for execution of a single set of tasks on a node</li>
  <li>"id": the job id (for debugging and logging purposes only)</li>
  <li>for full details, please take a look at the <a href="src/main/java/org/jppf/example/loadbalancer/client/CustomLoadBalancerRunner.java">fully commented job runner source code</a></li>
</ul>
b) Information provided by each node:
<ul class="samplesList">
  <li>"maxMemory": the maximum heap size available to the node</li>
  <li>"processing.threads": the number of processing threads available to the node</li>
</ul>
c) Our load-balancer will compute the maximum number of tasks in the job that can be sent to a node, based on:
<ul class="samplesList">
  <li>the estimated total memory footprint of the tasks, which must fit in the node's available memory.</li>
  <li>the estimated total execution time, that must be less than the allowed time</li>
  <li>for full details, please take a look at the <a href="src/main/java/org/jppf/example/loadbalancer/server/CustomLoadBalancer.java">fully commented load-balancer source code</a></li>
</ul>
d) Furthermore, additional node filtering will be applied before the load-balancing comes into play, by the means of an execution policy for each job:
<ul class="samplesList">
  <li>heavy jobs can only be executed on nodes such that the amount of available memory per processing thread is at least the memory footprint of each task;
  this will avoid under-utilization of nodes with many processing threads, while avoiding out of memory issues</li>
  <li>light jobs will only be executed on nodes that have at east 2 processing threads, to minimize the network transport overhead, which can be significant for short-lived tasks</li>
</ul>
Here is a set of links to the source of each Java class in this sample:
<ul>
  <li><a href="src/main/java/org/jppf/example/loadbalancer/server/CustomLoadBalancer.java">CustomLoadBalancer</a>: load-balancer implementation</li>
  <li><a href="src/main/java/org/jppf/example/loadbalancer/server/CustomLoadBalancerProvider.java">CustomLoadBalancerProvider</a>: the provider class that plugs our load balancer into the JPPF server</li>
  <li><a href="src/main/java/org/jppf/example/loadbalancer/client/CustomLoadBalancerRunner.java">CustomLoadBalancerRunner</a>: the client side application that executes the jobs on the grid</li>
  <li><a href="src/main/java/org/jppf/example/loadbalancer/client/CustomLoadBalancerTask.java">CustomLoadBalancerTask</a>: the JPPF tasks enclosed in the jobs</li>
  <li><a href="src/main/java/org/jppf/example/loadbalancer/common/MyCustomPolicy.java">MyCustomPolicy</a>: the custom execution policy used for "heavy" jobs</li>
</ul>

<h3>How do I run it?</h3>
Before running this sample, you need to install a JPPF server and at least two nodes.<br>
For information on how to set up a node and server, please refer to the <a href="https://www.jppf.org/doc/6.3/index.php?title=Introduction">JPPF documentation</a>.<br>
Once you have installed a server and node, perform the following steps:
<ol class="samplesList">
  <li>To configure the two nodes, there are 2 predefined node configurations we will use:
    <ul class="samplesNestedList">
      <li>copy <a href="config/node1/jppf-node.properties"><b>CustomLoadBalancer/config/node1/jppf-node.properties</b></a> to the <b>config</b> folder of your first node installation (it will replace the existing file).
      This will setup the first node with 64 MB of heap and 1 processing thread; this node will be used for "heavy" jobs</li>
      <li>copy <a href="config/node2/jppf-node.properties"><b>CustomLoadBalancer/config/node2/jppf-node.properties</b></a> to the <b>config</b> folder of your second node installation (it will replace the existing file).
      This will setup the second node with 64 MB of heap and 4 processing thread; this node will be used for "light" jobs</li>
    </ul>
  </li>
  <li>open a command prompt in <b>JPPF-x.y-samples-pack/CustomLoadBalancer</b></li>
  <li>build the sample: type "<b>mvn clean instakk</b>"; this will create a file named <b>CustomLoadBalancer.jar</b> in the <b>target</b> folder</li>
  <li>copy <b>target/CustomLoadBalancer.jar</b> in the "<b>lib</b>" folder of the JPPF driver installation, to add it to the driver's classpath. This will effectively install the new load-balancer.</li>
  <li>in the server's installation config/jppf-driver.properties file, replace the property "<b>jppf.load.balancing.algorithm = xxxxx</b>" with "<b>jppf.load.balancing.algorithm = customLoadBalancer</b>",
  to let the server know it must use the new load-balancer</li>
  <li>start the JPPF server and each of the 2 nodes</li>
  <li>the demo application should already be configured with a connection pool size of 2; to confirm it you can open the file <a href="config/jppf-client.properties"><b>jppf-client.properties</a></b> in <b>CustomerLoadbalancer/config</b>, you should see a line "<b>jppf.pool.size = 2"</b></li>
  <li>start the demo, by opening a console in <b>JPPF-x.y-samples-pack/CustomLoadBalancer</b>, and typing: "<b>ant run</b>"</li>
  <li>in the first node's console output you should see messages of this type:<br>
    <tt>[java] Starting execution of task Heavy Job - task 1</tt> (as many as there are tasks in the "heavy" job)</li>
  <li>in the second node's console output you should see messages of this type:<br>
    <tt>[java] Starting execution of task Light Job - task 2</tt> (as many as there are tasks in the "light" job)</li>
  <li>the demo console output should show something like this:
<pre><code>[java] ********** Results for job : Heavy Job **********
[java]
[java] Result for task Heavy Job - task 1 : the execution was performed successfully
[java] Result for task Heavy Job - task 2 : the execution was performed successfully
[java] Result for task Heavy Job - task 3 : the execution was performed successfully
[java] Result for task Heavy Job - task 4 : the execution was performed successfully
[java]
[java] ********** Results for job : Light Job **********
[java]
[java] Result for task Light Job - task 1 : the execution was performed successfully
[java] Result for task Light Job - task 2 : the execution was performed successfully
[java] Result for task Light Job - task 3 : the execution was performed successfully
[java] Result for task Light Job - task 4 : the execution was performed successfully
       .....
</code></pre>
  </li>
</ol>

<h3>What features of JPPF are demonstrated?</h3>
<ul>
  <li><a href="https://www.jppf.org/doc/6.3/index.php?title=Creating_a_custom_load-balancer">Creating a custom load-balancer</a></li>
  <li><a href="https://www.jppf.org/doc/6.3/index.php?title=Creating_a_custom_load-balancer#Node-aware_load_balancers">Node-aware load-balancers</a></li>
  <li><a href="https://www.jppf.org/doc/6.3/index.php?title=Creating_a_custom_load-balancer#Job-aware_load_balancers">Job-aware load-balancer</a></li>
  <li><a href="https://www.jppf.org/doc/6.3/index.php?title=Job_Service_Level_Agreement#Execution_policy">Execution policies</a></li>
  <li><a href="https://www.jppf.org/doc/6.3/index.php?title=Job_Metadata">Job metadata</a></li>
</ul>

<h3>I have additional questions and comments, where can I go?</h3>
<p>If you need more insight into the code of this demo, you can consult the Java source files located in the <b>CustomLoadBalancer/src</b> folder.
<p>In addition, There are 2 privileged places you can go to:
<ul>
  <li><a href="https://www.jppf.org/forums">The JPPF Forums</a></li>
  <li><a href="https://www.jppf.org/doc/6.3/">The JPPF documentation</a></li>
</ul>

