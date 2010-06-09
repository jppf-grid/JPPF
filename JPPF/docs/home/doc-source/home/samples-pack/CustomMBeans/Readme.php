<?php $currentPage="Samples" ?>
$template{name="samples-page-header" title="Custom MBeans sample"}$

<h3>What does the sample do?</h3>
This samples illustrates the implementation of custom MBeans for a JPPF server and node.
Each MBean simply retrieves the number of processors available to the JVM it is running in.

<h3>How do I run it?</h3>
Before running this sample application, you need to install a JPPF server and at least one node.<br>
For information on how to set up a node and server, please refer to the <a href="http://www.jppf.org/wiki/index.php?title=Introduction">JPPF documentation</a>.<br>
Once you have installed a server and node, perform the following steps:
<ol>
	<li>open a command prompt in JPPF-2.0-samples-pack/CustomMBeans</li>
	<li>build the sample: type "<b>ant jar</b>"; this will create a file named <b>CustomMBeans.jar</b></li>
	<li>copy CustomMBeans.jar in the "lib" folder of the JPPF driver installation, to add it to the driver's classpath. This is enough, as the node will download the custom MBean code from the server.</li>
	<li>open a command prompt in the driver's install folder and start the driver by typing "<b>ant run</b>"</li>
	<li>open a command prompt in the node's install folder and start the node by typing "<b>ant run</b>"</li>
	<li>in the sample's command prompt, type "<b>ant run</b>" to run the sample</li>
	<li>you should see the following display:
<pre style="background: #C0C0C0">  [echo] Testing the custom server MBean
  [java] The server has 8 available processors
  [echo] Testing the custom node MBean
  [java] The node has 8 available processors</pre>
	</li>
</ol>

<h3>What features of JPPF are demonstrated?</h3>
Pluggable management beans for JPPF nodes and servers.<br>
Dynamic code deployment from the server to a node.
For a detailed explanation, please refer to the related documentation in the
<a href="http://www.jppf.org/wiki/index.php?title=Extending_and_Customizing_JPPF#Pluggable_MBeans">Extending and Customizing JPPF &gt; Pluggable MBeans</a> section.

<h3>I have additional questions and comments, where can I go?</h3>
<p>If you need more insight into the code of this demo, you can consult the Java source files located in the <b>CustomMBeans/src</b> folder.
<p>In addition, There are 2 privileged places you can go to:
<ul>
	<li><a href="http://www.jppf.org/forums"/>The JPPF Forums</a></li>
	<li><a href="http://www.jppf.org/wiki">The JPPF documentation</a></li>
</ul>

$template{name="about-page-footer"}$
