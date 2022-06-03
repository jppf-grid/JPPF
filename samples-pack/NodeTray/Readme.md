# Node Tray demo

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
For information on how to set up a node and server, please refer to the <a href="https://www.jppf.org/doc/6.3/index.php?title=Introduction">JPPF documentation</a>.<br>
Once you have installed a server and node, perform the following steps:
<ol>
  <li>open a command prompt in JPPF-x.y-samples-pack/NodeTray</li>
  <li>build the sample: type "<b>mwn clean install</b>"; this will create a file named <b>NodeTray.jar</b> in the <b>target</b> folder</li>
  <li>copy <b>NodeTray.jar</b> in the "<b>lib</B>" folder of the JPPF node installation, to add it to the node's classpath. This is enough to deploy the add-on.</li>
  <li>start the server and node</li>
  <li>you should see the new system tray icon</li>
</ol>

<h3>What features of JPPF are demonstrated?</h3>
<ul>
  <li><a href="https://www.jppf.org/doc/6.3/index.php?title=Receiving_notifications_of_node_life_cycle_events">JPPF node life cycle notifications</a></li>
  <li><a href="https://www.jppf.org/doc/6.3/index.php?title=Node_management#Subscribing_to_MBean_notifications">Subscribing to task-level JMX notifications</a></li>
</ul>

<h3>I have additional questions and comments, where can I go?</h3>
<p>If you need more insight into the code of this demo, you can consult the Java source files located in the <b>CustomMBeans/src</b> folder.
<p>In addition, There are 2 privileged places you can go to:
<ul>
  <li><a href="https://www.jppf.org/forums">The JPPF Forums</a></li>
  <li><a href="https://www.jppf.org/doc/6.2">The JPPF documentation</a></li>
</ul>

