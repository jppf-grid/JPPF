<?php $currentPage="Samples" ?>
$template{name="samples-page-header" title="Node Tray sample"}$

<div align="justify">

					<h3>What does the sample do?</h3>
					This sample provides a node health monitor that sits in the system tray.<br>
					It displays the following information:
					<ul>
						<li>A "computer" icon in the system tray, which turns green when the node is connected to a server, and red when it is disconnected</li>
						<li>System tray notifications when the node status changes</li>
						<li>Information about the node's performance and connection to a JPPF server, when the mouse is hovering over the icon</li>
					</ul>
					<p>Here are some screenshots:
					<p align="center">
					<table border="0" cellspacing="10" cellpadding="0">
						<tr><td align="center"><img src="images/NodeTrayAddon-01.gif" border="0"/></td><td align="center"><img src="images/NodeTrayAddon-03.gif" border="0"/></td></tr>
						<tr><td align="center" colspan="2"><img src="images/NodeTrayAddon-02.gif" border="0"/></td></tr>
					</table>

					<h3>How do I run it?</h3>
					Before running this sample application, you need to install a JPPF server and at least one node.<br>
					For information on how to set up a node and server, please refer to the <a href="http://www.jppf.org/wiki/index.php?title=Introduction">JPPF documentation</a>.<br>
					Once you have installed a server and node, perform the following steps:
					<ol>
						<li>open a command prompt in JPPF-2.x-samples-pack/NodeTray</li>
						<li><b>important</b>: to build this sample you need a <b>JDK 1.6 or later</b>, as the system tray APIs are not available in prior Java versions</li>
						<li>build the sample: type "<b>ant jar</b>" or simply "<b>ant</b>"; this will create a file named <b>NodeTray.jar</b></li>
						<li>copy NodeTray.jar in the "lib" folder of the JPPF node installation, to add it to the node's classpath. This is enough to deploy the add-on.</li>
						<li>open a command prompt in the driver's install folder and start the driver by typing "<b>ant run</b>"</li>
						<li>open a command prompt in the node's install folder and start the node by typing "<b>ant run</b>"</li>
						<li>you should see the new system tray icon</li>
					</ol>

					<h3>What features of JPPF are demonstrated?</h3>
					JPPF Node startup class implementation: please refer to the related documentation in the
					<a href="http://www.jppf.org/wiki/index.php?title=Extending_and_Customizing_JPPF#Node_startup_classes">Extending and Customizing JPPF &gt; Node startup classes</a> section.<br>
					Subscribing to task-level JMX notifications: please refer to the
					<a href="http://www.jppf.org/wiki/index.php?title=Management_and_monitoring#Subscribing_to_MBean_notifications">Management and monitoring &gt; Subscribing to MBean notifications</a> section of the documentation.<br>

					<h3>I have additional questions and comments, where can I go?</h3>
					<p>If you need more insight into the code of this demo, you can consult the Java source files located in the <b>CustomMBeans/src</b> folder.
					<p>In addition, There are 2 privileged places you can go to:
					<ul>
						<li><a href="http://www.jppf.org/forums"/>The JPPF Forums</a></li>
						<li><a href="http://www.jppf.org/wiki">The JPPF documentation</a></li>
					</ul>
					
</div>

$template{name="about-page-footer"}$
