<?php $currentPage="Samples" ?>
$template{name="samples-page-header" title="Tasks Notifications sample"}$

<div align="justify">

					<h3>What does the sample do?</h3>
					This sample proposes a design to enable JPPF tasks to send real-time notifications, and to allow any client to subscribe to these notifications.

					<h3>Description of the problem to solve</h3>
					The goal is to provide an easy to use API that will allow tasks to send simple or complex notifications, so they can report on their progress, or synchronize with external components.

					<h3>Description of the solution</h3>
					This feature is implemented via two JPPF extensions for the nodes:
					<ul class="samplesList">
						<li>A <a href="http://www.jppf.org/doc/v4/index.php?title=Pluggable_MBeans#Writing_a_custom_node_MBean"/>custom node MBean</a> that will send notifications to local or remote listeners using the JMX APIs.</li>
						<li>A simple facade to the MBean, deployed as a <a href="http://www.jppf.org/doc/v4/index.php?title=JPPF_startup_classes#Node_startup_classes">JPPF node startup class</a>,
							that provides an easy access to the MBean without having to deal with its complexities, initialization, etc.</li>
					</ul>
					In addition, we have a JPPF client that will query the server for all attached nodes, connect to each node's JMX server, and subscribe to all notifications from each instance of our custom MBean.

					<h3>Related source files:</h3>
					<ul class="samplesList">
						<li>package <b>org.jppf.example.tasknotifications.mbean</b>:<br/>
							<a href="src/org/jppf/example/tasknotifications/mbean/TaskNotificationsMBean.java.html">TaskNotificationsMBean</a>: this is the MBean interface that defines 2 methods to send notifications<br/>
							<a href="src/org/jppf/example/tasknotifications/mbean/TaskNotifications.java.html">TaskNotifications</a>: this is the actual MBean implementation<br/>
							<a href="src/org/jppf/example/tasknotifications/mbean/TaskNotificationsMBeanProvider.java.html">TaskNotificationsMBeanProvider</a>: this is the code that plugs the custom MBean into the JPPF node<br/>
						&nbsp;</li>
						<li>package <b>org.jppf.example.tasknotifications.startup</b>:<br/>
							<a href="src/org/jppf/example/tasknotifications/startup/TaskNotifier.java.html">TaskNotifier</a>: provides a facade to the MBean, with static methods that can be easily used from any task.<br/>
						&nbsp;</li>
						<li>package <b>org.jppf.example.tasknotifications.test</b>:<br/>
							<a href="src/org/jppf/example/tasknotifications/test/TaskNotificationsRunner.java.html">TaskNotificationsRunner</a>: a sample JPPF client that subscribes to all notifications from the nodes.<br/>
							<a href="src/org/jppf/example/tasknotifications/test/NotifyingTask.java.html">NotifyingTask</a>: a sample JPPF task that sends notifications to report on its progress.<br/>
						&nbsp;</li>
						<li>folder <b>META-INF/services</b>:<br/>
							<a href="src/META-INF/services/org.jppf.management.spi.JPPFNodeMBeanProvider.html">org.jppf.management.spi.JPPFNodeMBeanProvider</a>: the service file that will allow the node to dynamically retrieve the MBean.<br/>
							<a href="src/META-INF/services/org.jppf.startup.JPPFNodeStartupSPI.html">org.jppf.startup.JPPFNodeStartupSPI</a>: the service file that will allow the node to dynamically retrieve the startup class.
						</li>
					</ul>

					<h3>How do I run it?</h3>
					Before running this sample application, you need to install a JPPF server and at least one node.<br>
					For information on how to set up a node and server, please refer to the <a href="http://www.jppf.org/doc/v4/index.php?title=Introduction">JPPF documentation</a>.<br>
					Once you have installed a server and node, perform the following steps:
					<ol class="samplesList">
						<li>open a command prompt in JPPF-x.y.z-samples-pack/TaskNotifications</li>
						<li>build the sample: type "<b>ant jar</b>"; this will create a file named <b>TasksNotifications.jar</b></li>
						<li>copy TasksNotifications.jar in the "lib" folder of the JPPF driver installation, to add it to the driver's classpath. This is enough, as the nodes will download the custom MBean and startup class code from the server.</li>
						<li>start the server and node</li>
						<li>Once you have a server and node, you can either run the "<b>run.bat</b>" script (on Windows), "<b>./run.sh</b>" script (on Linux/Unix) or, from a command prompt, type: <b>&quot;ant run&quot;</b>.</li>
						<li>you should see the following display:
<pre class="samples">  [java] received notification: Task '1' : reached stage 1
  [java] received notification: Task '1' : reached stage 2
  [java] ...
  [java] received notification: Task '1' : reached stage 10
  [java] Task 1 successful: the execution was performed successfully</pre>
						</li>
					</ol>

					<h3>What features of JPPF are demonstrated?</h3>
					<ul class="samplesList">
						<li><a href="http://www.jppf.org/doc/v4/index.php?title=Pluggable_MBeans#Writing_a_custom_node_MBean"/>Pluggable management beans for JPPF nodes</a></li>
						<li><a href="http://www.jppf.org/doc/v4/index.php?title=JPPF_startup_classes#Node_startup_classes">Node startup classes</a></li>
						<li><a href="http://www.jppf.org/doc/v4/index.php?title=Node_management#Subscribing_to_MBean_notifications">Subscribing to notifications from a node</a></li>
					</ul>

					<h3>I have additional questions and comments, where can I go?</h3>
					<p>If you need more insight into the code of this demo, you can consult the Java source files located in the <b>CustomMBeans/src</b> folder.
					<p>In addition, There are 2 privileged places you can go to:
					<ul class="samplesList">
						<li><a href="http://www.jppf.org/forums"/>The JPPF Forums</a></li>
						<li><a href="http://www.jppf.org/wiki">The JPPF documentation</a></li>
					</ul>
					
</div>

$template{name="about-page-footer"}$
