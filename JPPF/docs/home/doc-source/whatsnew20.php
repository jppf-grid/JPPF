<?php $currentPage="What's New" ?>
$template{name="about-page-header" title="About JPPF"}$
	<div align="justify">

		<h1>What's new in JPPF 2.0</h1>

		<h3>Jobs management and monitoring</h3>
		Jobs can be suspended, resumed or terminated at anytime in their life cycle<br>
		Each job has an SLA, including rule-based node filtering, priority, maximum number of nodes and scheduled start date<br>
		All job state changes can be traced through real time notifications<br>
		Jobs can be traced down to each node on which they execute<br>
		Execution policies (i.e. rule-based node filtering) now have access to the node storage information (requires Java 1.6 or later)<br>

		<h3>Platform extensibility features</h3>
		All management beans are now pluggable, users can add their own management modules at server or node level<br>
		Startup classes: users can now add their own initialization modules at server and node startup<br>
		Security: any data transiting over the network can now be encrypted by the way of user-defined transformations<br>
		Pluggable load-balancing modules allow users to write their own load balancing strategies<br>
		
		<h3>Performance improvements</h3>
		Object loading and deserialization are now done in parallel in the nodes, leading to a susbtantial performance improvement<br>
		A simplification of the communication protocol allows for faster network I/O and greater memory efficiency
		A new built-in load-balancing algorithm was added, based on reinforcement learning techniques, and particularly efficient with a large number of nodes.

		<h3>Client APIs</h3>
		Emphasis was set on job-related APIs<br>
		Older task-related APIs were deprecated but kept functional for compatibility with older versions<br>
		Client connections state notifications were refactored, exposed and documented<br>

		<h3>Administration console</h3>
		A new panel was added for jobs management and monitoring<br>
		Topology and jobs panels now have a toolbar adapting to the selected elements<br>
		The "admin" panel was removed<br>
		The load-balancing settings panel was updated to handle pluggable load-balancing algorithms<br>
		Server states are now emphasized using color highlighting<br>
		New icons are associated with servers, nodes and jobs in the topoloy and jobs panels<br>
		Usability was improved by automatically saving the user settings from one session to another for window size and location on the screen, as well as table columns sizes<br>
		The default look and feel was changed to JGoodies Looks<br>

		<h3>Documentation</h3>
		The documentation was almost completely rewritten and reorganized, in an effort to overcome what was our weakest point<br>
		The division into chapters and sections follows a logical path, resulting in easier navigation<br>
		An end-to-end tutorial was added, illustrating clearly and simply the development process for JPPF-empowered applications<br>
		The documentation is now available online on our wiki pages or as a PDF document<br>

		<h3>Distribution and download</h3>
		The downloadable modules were given a clearer name: JPPF-2.0-<i>module_name</i><br>
		A new web installer is now available, making the JPPF installation simpler than ever (uses Java Web Start technology)<br>
		The J2EE connector now comes as a separate module<br>
		A new "application template" module is now available, to quickly and easily start developing JPPF applications<br>

		<h3>Compatibility with previous versions</h3>
		All applications developped with earlier versions of JPPF are compatible at the source level with JPPF 2.0. They may require a rebuild or recompilation, however the overhead should be kept to a minimum.<br>
		Due to changes in the communication protocol, version 1.9.1 clients, servers or nodes will NOT work with version 2.0 components. All components must be upgraded to version 2.0.<br>

	</div>
	<br>
$template{name="about-page-footer"}$
