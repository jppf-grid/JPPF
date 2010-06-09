<?php $currentPage="Press" ?>
$template{name="about-page-header" title="Press Kit"}$

<div align="justify">

	<h1>JPPF Press Kit</h1>

	<h3>Content</h3>
	<div class="u_link" style="margin-left: 10px">
		<a href="#original_release">Text of the original release</a><br>
		<a href="#features">Features</a><br>
		<a href="#downloads">Where to download</a><br>
		<a href="#documentation">Documentation</a><br>
		<a href="#license">License</a><br>
		<a href="#contacts">Contacts</a><br>
	</div>
	
	<br>
	<a name="original_release"></a>
	<h3>Latest press release: JPPF 2.1</h3>
	
	<p>In this version, many improvements and fixes increase the reliability, scalability and ease of use of JPPF.
	For the full list and details of the new features in JPPF 2.1, do not hesitate to read the <a href="release_notes.php?version=2.1">JPPF 2.1 release notes</a>.

	<p><b>Performance</b>: special emphasis on both memory usage and performance improvements. The JPPF server now executes multithreaded network I/O and reaches new levels of scalability.
	JPPF applications can now enjoy faster execution of larger jobs.

	<p><b>Localization</b>: Russian localization is now available for the graphical administration console.

	<p><b>Load balancing</b>: Custom load-balancers can now use information about the nodes' environment and configuration, along with metadata about the jobs.
	This allows for sophisticated load-balancing implementations that can adapt to the capabilities of each node and the computational characteristics of each job.

	<p><b>Configuration</b>: the managment port auto-incrementation enables servers and nodes to automatically find port available numbers, making JPPF configuration even easier and removing one the main configuration hurdles.
	It is now possible to specify the number of concurrent threads performing I/O in the server.

	<p><b>New Samples</b>: three new samples complement our offering in the JPPF samples pack:<br/>
	- Simulation of large portfolio updates<br/>
	- JPPF node health monitor in the system tray<br/>
	- An example of a sophisticated load-balancer implementation<br/>
	
	<a name="features"></a>
	<h3>Features</h3>
	<div class="u_link" style="margin-left: 10px">
		<a href="release_notes.php?version=2.1">Release notes</a>: see what's new in JPPF 2.1<br>
		<a href="features.php">Full features list</a><br>
	</div>
	
	<a name="downloads"></a>
	<h3>Downloads</h3>
	
	All files can be found from our <a href="downloads.php">downloads page</a>.<br>
	A <a href="/download/jppf_ws.jnlp">web installer</a> allows you to select and download only the specific modules you want to install (requires Java Web Start 1.5 or later).
	
	<a name="documentation"></a>
	<h3>Documentation</h3>
	
	The JPPF documentation can be found online on our <a href="/wiki">wiki pages</a>. You may also read it offline as <a href="/documents/JPPF-User-Guide.pdf">a PDF document</a>.
	
	<a name="license"></a>
	<h3>License</h3>
	JPPF is released under the terms of the <a href="license.php">Apachache v2.0</a> license.
	This <a href="http://www.opensource.org">OSI-approved</a> open source license is friendly to individuals, businesses, governments and academia, for commercial and non-commercial purposes.
	It does not restrict the use of JPPF with commercial and proprietary applications.
	
	<a name="contacts"></a>
	<h3>Contacts</h3>
	For any press inquiry, please refer to our <a href="contacts.php">contacts</a> page.

</div>

$template{name="about-page-footer"}$
