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
	<h3>Latest press release: JPPF 2.0</h3>
	
	<p>This version represents a major evolution of the JPPF platform. It brings innumerable new features and enhancements over the previous versions, which are summarized here.
	For the full list and details of the new features in JPPF 2.0, do not hesitate to read the <a href="release_notes.php?version=2.0">JPPF 2.0 release notes</a>.

	<p><b>Job SLA and management</b>: monitor, control and manage your jobs throughout their life cycle in the Grid. Dynamically control the terms and conditions in which jobs are processed on the Grid.
	The job SLA terms include rule-based node filtering (aka <a href="/wiki/index.php?title=Development_guide#Execution_policy">execution policy</a>), job scheduled start date, maximum number of nodes assigned to job execution.
	Jobs can be suspended, resumed or terminated at anytime during their life cycle. Each job execution is now traceable from end to end.

	<?php $base="wiki/index.php?title=Extending_and_Customizing_JPPF#"; ?>
	<p><b>Platform extensibility</b>: add new capabilities to the platform by plugging-in additional <a href="<?php echo $base . 'Pluggable_MBeans'; ?>">management</a> and 
	<a href="<?php echo $base . 'JPPF_startup_classes'; ?>">startup</a> modules, <a href="<?php echo $base . 'Creating_a_custom_load-balancer'; ?>">load-balancers</a>, 
	<a href="<?php echo $base . 'Transforming_and_encrypting_networked_data'; ?>">data encryption schemes</a>.
	This opens up a new world of possibilities for the platform, from simple tracing and logging up to dynamic topology management.

	<p><b>Enhanced administration console</b>: visualize and manage the grid components and jobs processing from a single user interface, easy to use yet professional-looking.
	A new screen was added for jobs management, dedicated to the control and monitoring of JPPF jobs throughout their life span on the Grid.
	The interface's usability and comfort was greatly improved.

	<p><b>Additions to the distribution</b>: a new "application template" module is now part of the JPPF distribution.
	This is a very simple, but fully working and documented, JPPF client application that can be reused as a starting point for JPPF-enabled development.
	The J2EE connector can be now downloaded and installed separately. It also includes support for the Apache Geronimo application server.

	<p><b>Documentation</b>: a major emphasis was set on the documentation, resulting in a comprehensive, fully detailed and easy to navigate documentation set.
	It is now available <a href="/wiki">online</a> or as <a href="/documents/JPPF-2.0-User-Guide.pdf">a PDF file</a>.
	
	<a name="features"></a>
	<h3>Features</h3>
	<div class="u_link" style="margin-left: 10px">
		<a href="release_notes.php?version=2.0">Release notes</a>: see what's new in JPPF 2.0<br>
		<a href="features.php">Full features list</a><br>
	</div>
	
	<a name="downloads"></a>
	<h3>Downloads</h3>
	
	All files can be found from our <a href="downloads.php">downloads page</a>.<br>
	A <a href="/download/jppf_ws.jnlp">web installer</a> allows you to select and download only the specific modules you want to install (requires Java Web Start 1.5 or later).
	
	<a name="documentation"></a>
	<h3>Documentation</h3>
	
	The JPPF documentation can be found online on our <a href="/wiki">wiki pages</a>. You may also read it offline as <a href="/documents/JPPF-2.0-User-Guide.pdf">a PDF document</a>.
	
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
