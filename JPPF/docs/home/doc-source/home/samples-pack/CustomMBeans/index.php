$template{name="sample-readme-html-header" title="Custom MBeans demo"}$
<h3>What does the sample do?</h3>
This samples illustrates the implementation of custom MBeans for a JPPF server and node.
Each MBean simply retrieves the number of processors available to the JVM it is running in.

<h3>How do I run it?</h3>
Before running this sample application, you need to install a JPPF server and at least one node.<br>
For information on how to set up a node and server, please refer to the <a href="https://www.jppf.org/doc/6.3/index.php?title=Introduction">JPPF documentation</a>.<br>
Once you have installed a server and node, perform the following steps:
<ol class="samplesList">
  <li>open a command prompt in JPPF-x.y-samples-pack/CustomMBeans</li>
  <li>build the sample: type "<b>mvn clean install</b>"; this will create a file named <b>CustomMBeans.jar</b> in the <b>target</b> folder</li>
  <li>copy CustomMBeans.jar in the "lib" folder of the JPPF driver installation, to add it to the driver's classpath. This is enough, as the node will download the custom MBean code from the server.</li>
  <li>start the driver and node</li>
  <li>in the sample's command prompt, type "<b>./run.sh</b>" (Linux/Mac) or "<b>run.bat</b>" (Windows) to run the sample</li>
  <li>you should see the following display:
<pre class="samples">[echo] Testing the custom server MBean
[java] The server has 8 available processors
[echo] Testing the custom node MBean
[java] The node has 8 available processors
</pre>
  </li>
</ol>

<h3>What features of JPPF are demonstrated?</h3>
Pluggable management beans for JPPF nodes and servers.<br>
For a detailed explanation, please refer to the related documentation in the
<a href="https://www.jppf.org/doc/6.3/index.php?title=Pluggable_MBeans">Pluggable MBeans</a> section.

<h3>I have additional questions and comments, where can I go?</h3>
<p>If you need more insight into the code of this demo, you can consult the Java source files located in the <b>CustomMBeans/src</b> folder.
<p>In addition, There are 2 privileged places you can go to:
<ul>
  <li><a href="https://www.jppf.org/forums">The JPPF Forums</a></li>
  <li><a href="https://www.jppf.org/doc/6.2">The JPPF documentation</a></li>
</ul>
$template{name="sample-readme-html-footer"}$
