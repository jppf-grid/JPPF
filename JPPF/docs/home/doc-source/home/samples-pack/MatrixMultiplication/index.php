$template{name="sample-readme-html-header" title="Dense Matrix Multiplication demo"}$
<h3>What does the sample do?</h3>
This sample performs the multplication of 2 square dense matrices by dividing the operation into as many JPPF tasks as there are rows in each matrix. Each task multiplies a row of the first matrix by the second matrix.

<h3>How do I run it?</h3>
Before running this sample application, you must have a JPPF server and at least one node running.<br>
For information on how to set up a node and server, please refer to the <a href="http://www.jppf.org/doc/5.2">JPPF documentation</a>.<br>
Once you have a server and node, from a command prompt, type: <b>&quot;ant run&quot;</b>

<h3>How do I use it?</h3>
<p>This sample doesn't have a graphical user interface, however you can modify some of the parameters in the JPPF configuration file:
<ol>
  <li>open the file "<b>config/jppf-client.properties</b>" in a text editor</li>
  <li>at the end of the file, you will see the following properties:
<pre class="prettyprint lang-conf">
# the size of the matrices to multiply
matrix.size = 300
# number of times the matrix multiplication is performed
matrix.iterations = 100
</pre>
  </li>
  <li>"<b>matrix.size</b>" allows you to experiment with different sizes of the matrices</li>
  <li>"<b>matrix.iterations</b>" is the number of times the matrix multiplication will be performed.
  As the sample simply prints the time per iteration, this will allow you to have a feel as to how the load-balancing algortihm converges toward a near-optimal performance</li>
</ol>
<p>It is interesting to experiment with a different number of nodes and load-balancing algorithm, and test their efficiency in your environment.

<h3>How can I build the sample?</h3>
To compile the source code, from a command prompt, type: <b>&quot;ant compile&quot;</b><br>
To generate the Javadoc, from a command prompt, type: <b>&quot;ant javadoc&quot;</b>

<h3>I have additional questions and comments, where can I go?</h3>
<p>If you need more insight into the code of this demo, you can consult the source, or have a look at the
<a href="javadoc/index.html">API documentation</a>.
<p>In addition, There are 2 privileged places you can go to:
<ul>
  <li><a href="http://www.jppf.org/forums"/>The JPPF Forums</a></li>
  <li><a href="http://www.jppf.org/doc/5.2">The JPPF documentation</a></li>
</ul>
$template{name="sample-readme-html-footer"}$
