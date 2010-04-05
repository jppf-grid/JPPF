<?php $currentPage="What's New" ?>
$template{name="about-page-header" title="What's new"}$
	<div align="justify">

		<h1>What's new in JPPF 2.1</h1>

		<h3>Performance</h3>
		The development of this release has seen a lot of emphasis on both memory usage and performance improvements.<br/>
		The JPPF server now executes multithreaded network I/O and reaches new levels of scalability.<br/>
		JPPF applications can now enjoy faster execution of larger jobs.

		<h3>Localization</h3>
		Russian localization is now available for the graphical administration console.

		<h3>Load balancing</h3>
		Custom load-balancers can now use information about the nodes' environment and configuration, along with metadata about the jobs.<br/>
		This allows for sophisticated load-balancing implementations that can adapt to the capabilities of each node and the computational characteristics of each job.<br/>
		A sophisticated and fully documented <a href="samples-pack/CustomLoadBalancer/Readme.html"><b>sample</b></a> illustrates these new capabilities and will get you started in no time.

		<h3>Configuration</h3>
		The managment port auto-incrementation enables servers and nodes to automatically find port available numbers, making JPPF configuration even easier and removing one the  main configuration hurdles.<br/>
		It is now possible to specify the number of concurrent threads performing I/O in the server.

		<h3>New Samples</h3>
		Three new samples complement our offering in the JPPF samples pack:
		<ul>
			<li><a href="samples-pack/DataDependency/Readme.html">Simulation of large portfolio updates</a></li>
			<li><a href="samples-pack/NodeTray/Readme.html">JPPF node health monitor in the system tray</a></li>
			<li><a href="samples-pack/CustomLoadBalancer/Readme.html">An example of a sophisticated load-balancer implementation</a></li>
		</ul>

		<h3>New Feature Requests</h3>
		<ul>
			<li>2966065 - Adapt the load-balancing to the weight of each job</li>
			<li>2955505 - Give load balancers access to the node's configuration</li>
			<li>2182052 - Simple local node monitoring in system tray</li>
		</ul>

		<h3>Bug Fixes</h3>
		<ul>
			<li>2972979 - Un-connected nodes shouldn't report a valid number of thread</li>
			<li>2969881 - Undefined processing.threads causes execution policy to fail</li>
			<li>2969126 - ClientDataProvider feature not implemented in J2EE connector</li>
			<li>2967151 - Improper exception handling in JPPFBroadcaster</li>
			<li>2962404 - Changing load-balancer settings is done at the wrong time</li>
			<li>2955491 - Connection pool not working whith server discovery disabled</li>
			<li>2953562 - Node does not report results of tasks with timeout</li>
			<li>2933677 - Server thread stuck when serialization fails in node</li>
			<li>2914622 - Local IP addresses should include more than 127.0.0.1</li>
			<li>2907258 - JMX initialization failure causes node to stop working</li>
			<li>2907246 - Remote debugging is only possible on localhost</li>
		</ul>

		<h3></h3>

		<h3></h3>

	</div>
	<br>
$template{name="about-page-footer"}$
