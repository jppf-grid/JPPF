<?php $currentPage="Samples" ?>
$template{name="samples-page-header" title="Startup Classes sample"}$


					<h3>What does the sample do?</h3>
					This samples illustrates the implementation of custom startup classes for a JPPF server and node.
					Startup classes are wrappers around any arbitrary code that is executed at server or node intialization time.

					<h3>How do I run it?</h3>
					Before running this sample, you need to install a JPPF server and at least one node.<br>
					For information on how to set up a node and server, please refer to the <a href="http://www.jppf.org/wiki/index.php?title=Introduction">JPPF documentation</a>.<br>
					Once you have installed a server and node, perform the following steps:
					<ol>
						<li>open a command prompt in JPPF-2.0-samples-pack/StartupClasses</li>
						<li>build the sample: type "<b>ant jar</b>"; this will create a file named <b>StartupClasses.jar</b></li>
						<li>copy StartupClasses.jar in the "lib" folder of the JPPF driver installation, to add it to the driver's classpath. This is enough, as the node will download the startup classes code from the server.</li>
						<li>open a command prompt in the driver's install folder and start the driver by typing "<b>ant run</b>"</li>
						<li>in the driver console, you should see the following message: <tt>  [java] I'm a driver startup class</tt></li>
						<li>open a command prompt in the node's install folder and start the node by typing "<b>ant run</b>"</li>
						<li>in the node console, you should see the following message: <tt>  [java] I'm a node startup class</tt></li>
					</ol>

					<h3>What features of JPPF are demonstrated?</h3>
					Pluggable startup classes for JPPF nodes and servers.<br>
					Dynamic code deployment from the server to a node.
					For a detailed explanation, please refer to the related documentation in the
					<a href="http://www.jppf.org/wiki/index.php?title=Extending_and_Customizing_JPPF#JPPF_startup_classes">Extending and Customizing JPPF &gt; JPPF startup classes</a> section.

					<h3>I have additional questions and comments, where can I go?</h3>
					<p>If you need more insight into the code of this demo, you can consult the Java source files located in the <b>CustomMBeans/src</b> folder.
					<p>In addition, There are 2 privileged places you can go to:
					<ul>
						<li><a href="http://www.jppf.org/forums"/>The JPPF Forums</a></li>
						<li><a href="http://www.jppf.org/wiki">The JPPF documentation</a></li>
					</ul>
					

$template{name="about-page-footer"}$
