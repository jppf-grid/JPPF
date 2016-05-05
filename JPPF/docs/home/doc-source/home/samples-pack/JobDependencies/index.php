$template{name="sample-readme-html-header" title="Job Dependencies demo"}$
<h3>What does the sample do?</h3>
<p>This demo illustrates a solution to submit jobs that have a dependency on one or more other jobs.
It supports any non-cyclic dependency graph and will work even when the jobs in the dependency graph are submitted from multiple JPPF clients running on multiple machines.

<h3>How do I run it?</h3>
<p style="font-weight: bold">1. Grid setup
<ul class="samplesList">
  <li>you will first need to build the server extension which manages the job dependencies: start a command prompt in <code>JPPF-x.y.z-samples-pack/JobDependencies</code> then type "ant".
  This will create a file named <b>JobDependencies.jar</b>. Copy this file to your server's /lib folder</li>
  <li>then start a JPPF server and at least one node. For information on how to set up a node and server, please refer to the <a href="http://www.jppf.org/doc/v5/index.php?title=Introduction">JPPF documentation</a>.</li>
  <li>during the server startup, you should see the following two lines in the console output:
<pre class="prettyprint lang-regex">in ServerDependenciesHandler()
Initializing ServerDependenciesHandler</pre>
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
<pre class="prettyprint lang-regex"><br>Job F ==> Job A, Job B, Job E
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

<h3>Source files</h3>
<ul class="samplesList">
  <li><a href="src/org/jppf/example/job/dependencies/JobDependenciesRunner.java.html">JobDependenciesRunner.java</a>: the entry point for the demo, reads the dependency graph and submits multiple jobs accordingly</li>
  <li><a href="src/org/jppf/example/job/dependencies/ServerDependenciesHandler.java.html">ServerDependenciesHandler.java<a/>: deployed as a <a href="/doc/5.2/index.php?title=JPPF_startup_classes#Server_startup_classes">server startup class</a>,
  manages the dependencies between jobs as they arrive in the server queue</li>
  <li><a href="src/org/jppf/example/job/dependencies/JobIdentifier.java.html">JobIdentifier.java</a>: store the association between JPPF job uuids and application-defined ids</li>
  <li><a href="src/org/jppf/example/job/dependencies/MyTask.java.html">MyTask.java</a>: a simple JPPF task implementation used by all the jobs in the demo</li>
  <li><a href="dependency_graph.txt.html">dependency_graph.txt</a>: this is the configuration file where the job dependency graph is defined</li>
</ul>

<h3>I have additional questions and comments, where can I go?</h3>
<p>If you need more insight into the code of this demo, you can consult the Java source files located in the <b>CustomLoadBalancer/src</b> folder.
<p>In addition, There are 2 privileged places you can go to:
<ul>
  <li><a href="http://www.jppf.org/forums"/>The JPPF Forums</a></li>
  <li><a href="http://www.jppf.org/doc/v5/">The JPPF documentation</a></li>
</ul>
$template{name="sample-readme-html-footer"}$
