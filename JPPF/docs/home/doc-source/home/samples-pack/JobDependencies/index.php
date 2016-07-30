$template{name="sample-readme-html-header" title="Job Dependencies demo"}$
<h3>What does the sample do?</h3>
<p>This demo illustrates a solution to submit jobs that have a dependency on one or more other jobs.
It supports any non-cyclic dependency graph and will work even when the jobs in the dependency graph are submitted from multiple JPPF clients running on multiple machines.

<h3>How do I run it?</h3>
<p style="font-weight: bold">1. Grid setup
<ul class="samplesList">
  <li>you will first need to build the server extension which manages the job dependencies: start a command prompt in <code>JPPF-x.y.z-samples-pack/JobDependencies</code> then type "ant".
  This will create a file named <b>JobDependencies.jar</b>. Copy this file to your server's /lib folder</li>
  <li>then start a JPPF server and at least one node. For information on how to set up a node and server, please refer to the <a href="http://www.jppf.org/doc/5.2/index.php?title=Introduction">JPPF documentation</a>.</li>
  <li>during the server startup, you should see the following two lines in the console output:
<pre class="prettyprint lang-regex">[2016-05-09 21:36:13.585] processor: In DependencyProcessor()
[2016-05-09 21:36:13.585] processor: Initializing DependencyProcessor</pre>
  </li>
</ul>
<p><b>2. Running the demo</b>
<ul class="samplesList">
  <li>open a command prompt in the <code>JPPF-x.y.z-samples-pack/JobDependencies</code> folder</li>
  <li>start the demo by entering "run.bat" on Windows, "./run.sh" on Linux/Unix or by launching the Ant script "ant run"</li>
  <li>During the demo, the console output will show the jobs that complete, along with their execution results</li>
</ul>
<p><b>3. Defining your own dependency graph</b>
<ul class="samplesList">
  <li>The jobs graph is defined in the file <a href="dependency_graph.txt.html"><code>JobDependencies/dependency_graph.txt</code></a></li>
  <li>for example, the dependency graph defined in the vanilla version of this sample is defined as:<br>
    <table border="0" cellpadding="5" style="width: 100%">
    <tr>
      <td align="center" valign="top"><p style="font-weight: bold;">Configuration</td>
      <td align="center" valign="top"><p style="font-weight: bold">Corresponding graph</td>
    </tr>
    <tr>
      <td align="left" valign="center">
<pre class="prettyprint lang-regex"><br>Job F ==> Job A, Job B, Job E | remove
Job E ==> Job C, Job D
Job D ==> Job B, Job C
Job C ==> Job A
Job B ==> Job A
Job A ==><br> </pre>
      </td>
      <td align="center" valign="top"><img src="data/DependenciesGraph.gif"/></td>
    </tr>
    </table>
  </li>
</ul>
<p><b>4. Detection of cycles in the dependency graph</b>
<p>When a cycle is detected, the server will print a message to the console, similar to this:
<pre class="prettyprint lang-regex">[2016-05-09 21:48:00.621] cycle detected while adding dependency 'Job C' to 'Job A' :
Job A ==> Job B ==> Job C ==> Job A</pre>
Furthermore, no dependency will be assigned to the job, so that it can be executed and avoid being stuck in the server queue.

<p><b>5. Distributed scenario: submitting jobs in the dependency graph from separate clients</b>
<p>As mentioned above, this demo will work even when the jobs are submitted from multiple client applications. To illustrate this, we will proceed as follows:
<ul class="samplesList">
  <li>Make a copy of the <code>JPPF-x.y.z-samples-pack/JobDependencies</code> folder, let's call it <code>JobDependencies2</code> for this exercise</li>
  <li>in <code>JobDependencies/dependency_graph.txt</code>, remove or comment out the <i>last</i> 3 declared dependencies, so the dependencies should look like this:
<pre class="prettyprint lang-regex" style="margin: 0px">Job F ==> Job A, Job B, Job E | remove
Job E ==> Job C, Job D
Job D ==> Job B, Job C</pre>
  </li>
  <li>in <code>JobDependencies2/dependency_graph.txt</code>, remove or comment out the <i>first</i> 3 declared dependencies, the dependencies should look like this:
<pre class="prettyprint lang-regex" style="margin: 0px">Job C ==> Job A
Job B ==> Job A
Job A ==></pre>
  </li>
  <li>if not already done, start a server and at least one node</li>
  <li>start the demo in JobDependencies with "run.bat" or "./run.sh". You will observe that it prints the following message:
  <pre class="prettyprint lang-regex" style="margin: 0px">[2016-07-22 08:51:33.667] runner: ***** awaiting results for 'Job F' *****</pre>
  </li>
  <li>start the demo in JobDependencies2 with "run.bat" or "./run.sh". It will print completion messages for the jobs A, B and C</li>
  <li>now if you get back to the console for JobDependencies, you will see that jobs D, E, F were completed as well, following the completion of their dependencies submitted by the other instance of the demo in JobDependencies2</li>
</ul>

<h3>Source files</h3>
<p>package <b>org.jppf.example.job.dependencies</b>:
<ul class="samplesList">
  <li><a href="src/org/jppf/example/job/dependencies/JobDependenciesRunner.java.html">JobDependenciesRunner.java</a>: the entry point for the demo, reads the dependency graph and submits multiple jobs accordingly</li>
  <li><a href="src/org/jppf/example/job/dependencies/DependencyProcessor.java.html">DependencyProcessor.java<a/>: deployed as a <a href="http://www.jppf.org/doc/5.2/index.php?title=JPPF_startup_classes#Server_startup_classes">server startup class</a>,
  suspends / resumes jobs based on the state of their dependencies</li>
  <li><a href="src/org/jppf/example/job/dependencies/DependencyGraph.java.html">DependencyGraph.java</a>: represents the job dependencies graph</li>
  <li><a href="src/org/jppf/example/job/dependencies/DependencyNode.java.html">DependencyNode.java</a>: represents a node (aka vertex) in the dependency graph</li>
  <li><a href="src/org/jppf/example/job/dependencies/DependencySpec.java.html">DependencySpec.java</a>: represents information of a job and its dependencies, supplied to the job's metadata</li>
  <li><a href="src/org/jppf/example/job/dependencies/Utils.java.html">Utils.java</a>: provides utility methods to read the dependency graph file and print formatted messages to the ouput console</li>
  <li><a href="src/org/jppf/example/job/dependencies/MyTask.java.html">MyTask.java</a>: a simple JPPF task implementation used by all the jobs in the demo</li>
  <li><a href="dependency_graph.txt.html">dependency_graph.txt</a>: this is the configuration file where the job dependency graph is defined</li>
</ul>
<p>package <b>org.jppf.example.job.dependencies.management</b>:
<ul class="samplesList">
  <li><a href="src/org/jppf/example/job/dependencies/management/DependencyManagerMBean.java.html">DependencyManagerMBean.java</a>: the job dependencies management interface</li>
  <li><a href="src/org/jppf/example/job/dependencies/management/DependencyManager.java.html">DependencyManager.java</a>: the job dependencies management implementation</li>
  <li><a href="src/org/jppf/example/job/dependencies/management/DependencyManagerMBeanProvider.java.html">DependencyManagerMBeanProvider.java</a>: implementation of the discovery interface for the pluggable MBean</li>
</ul>

<h3>I have additional questions and comments, where can I go?</h3>
<p>If you need more insight into the code of this demo, you can consult the Java source files located in the <b>CustomLoadBalancer/src</b> folder.
<p>In addition, There are 2 privileged places you can go to:
<ul>
  <li><a href="http://www.jppf.org/forums"/>The JPPF Forums</a></li>
  <li><a href="http://www.jppf.org/doc/5.2/">The JPPF documentation</a></li>
</ul>
$template{name="sample-readme-html-footer"}$
