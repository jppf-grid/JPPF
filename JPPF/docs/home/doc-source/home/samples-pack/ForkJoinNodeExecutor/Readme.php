<?php $currentPage="Samples" ?>
$template{name="samples-page-header" title="Fibonacci Fork/Join sample"}$

<div align="justify" class="blockWithHighlightedTitle" style="padding: 5px">

          <h3>What does the sample do?</h3>
          This demo demonstrates a node add-on which replaces the standard node processing thread pool with a <a href="http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ForkJoinPool.html">fork/join thread pool</a>.
          This allows JPPF tasks to locally (in the node) spawn ForkJoinTask (or any of its subclasses) instances and have them processed as expected for a ForkJoinPool.
          The use of this fork/join executor is illustrated with a Fibonacci computation demo.

          <h3>How do I run it?</h3>
          Before running this sample application, you must have a JPPF server and at least one node running.<br>
          For information on how to set up a node and server, please refer to the <a href="http://www.jppf.org/wiki">JPPF documentation</a>.<br>
          <ol class="samplesList">
            <li>For a node to use the fork/join executor add-on, you need to add the following property to its configuration file:
<pre class="samples">jppf.thread.manager.class = org.jppf.execute.ThreadManagerForkJoin</pre>
            </li>
            <li>Once this is done, start the server and the node(s)</li>
            <li>From a command prompt, type: <b>&quot;run.bat&quot;</b> (on Windows) or <b>&quot;./run.sh&quot;</b> (on Linux/Unix)</li>
          </ol>

          <h3>How do I use it?</h3>
          <p>This sample doesn't have a graphical user interface, however you can modify some of the parameters in the JPPF configuration file:
          <ol class="samplesList">
            <li>open the file "<b>config/jppf.properties</b>" in a text editor</li>
            <li>at the top of the file, you will see the following properties:
<pre class="samples"><font color="green"># number of Fibonacci computation tasks to execute</font>
fib.fj.nbTasks = 10
<font color="green"># order of Fibonacci number to compute</font>
fib.fj.N = 10</pre>
            </li>
            <li>"<b>fib.fj.nbTasks</b>" allows you change the number of tasks in the submitted job, and thusto simulate various workloads on the grid and especially on the nodes</li>
            <li>"<b>fib.fj.N</b>" is the Fibonacci order, the higher it is the larger the number of spawned fork/join tasks will be</li>
          </ol>

          <h3>How can I build the sample?</h3>
          To compile the source code, from a command prompt type: <b>&quot;ant compile&quot;</b><br>
          To generate the Javadoc, from a command prompt type: <b>&quot;ant javadoc&quot;</b>

          <h3>I have additional questions and comments, where can I go?</h3>
          <p>If you need more insight into the code of this demo, you can consult the source, or have a look at the
          <a href="javadoc/index.html">API documentation</a>.
          <p>In addition, There are 2 privileged places you can go to:
          <ul>
            <li><a href="http://www.jppf.org/forums"/>The JPPF Forums</a></li>
            <li><a href="http://www.jppf.org/doc/v4">The JPPF documentation</a></li>
          </ul>
          
</div><br>

$template{name="about-page-footer"}$
