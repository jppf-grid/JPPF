$template{name="sample-readme-html-header" title="Job Recovery demo"}$
<h3>What does the sample do?</h3>
This sample demonstrates how the state of a JPPF job can be persisted on the client side, then recovered after a client crash.<br/>
For this purpose, the demo application submits a job then, while the job is still executing, exits brutally.
The next time the application is started, it reads the latest persisted state of the job and submits it again to the grid.
Only the part of the job that hadn't completed will be executed.

<h3>How do I run it?</h3>
<ol class="samplesList">
  <li>Before running this sample application, you must have a JPPF server and one node running. For information on how to set up a node and server, please refer to the <a href="http://www.jppf.org/doc/5.2">JPPF documentation</a>.</li>
  <li>Once you have a server and node, you can either run the "<b>run.bat</b>" script (on Windows), "<b>./run.sh</b>" script (on Linux/Unix) or, from a command prompt, type: <b>&quot;ant run&quot;</b>.</li>
  <li>A the first execution, the application will show that no job exists in the job perisstence store, with a message similar to this:
<pre class="samples">no job found in persistence store, creating a new job with 10 tasks</pre>
  </li>
  <li>You will then see, in the node's shell console, messages like this:
<pre class="samples">task 1 completed sucessfully
...
task 5 completed sucessfully
task 6 completed with error [java.lang.InterruptedException: sleep interrupted]</pre>
  </li>
  <li>at this point, you will notice that the application has exited, without displaying the job execution results</li>
  <li>restart the application: you should now see a message like:
<pre class="samples">found jobs in persistence store: [the_job_uuid]
loaded job 'the_job_uuid' from persistence store ...</pre>
  </li>
  <li>the node's console will again display successful completion messages, but starting from the latest task that was successfully persisted</li>
</ol>

<h3>Related source files</h3>
<ul class="samplesList">
  <li><a href="src/org/jppf/example/jobrecovery/Runner.java.html">Runner.java</a> : this is the application that submits a job, simulates a crash, and performs the job recovery</li>
  <li><a href="src/org/jppf/example/jobrecovery/MyTask.java.html">MyTask.java</a> : a JPPF task that simply waits for one second before completing</li>
</ul>

<h3>How can I build the sample?</h3>
To compile the source code, from a command prompt, type: <b>&quot;ant compile&quot;</b><br>
To generate the Javadoc, from a command prompt, type: <b>&quot;ant javadoc&quot;</b>

<h3>I have additional questions and comments, where can I go?</h3>
<p>If you need more insight into the code of this demo, you can consult the source, or have a look at the
<a href="javadoc/index.html">API documentation</a>.
<p>In addition, There are 2 privileged places you can go to:
<ul class="samplesList">
  <li><a href="http://www.jppf.org/forums"/>The JPPF Forums</a></li>
  <li><a href="http://www.jppf.org/doc/5.2">The JPPF documentation</a></li>
</ul>
$template{name="sample-readme-html-footer"}$
