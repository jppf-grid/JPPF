<?php $currentPage="What's New" ?>
$template{name="about-page-header" title="What's new"}$
	<div align="justify">

		<h1>What's new in JPPF 2.1.1</h1>

		<h3>This release addresses and fixes a critical bug:</h3>
		2990287 - Killing a node running on Unix causes the server to hang

		<h3>To patch an existing v2.1 server installation:</h3>
		<ul>
			<li>download the driver installation binaries (JPPF-2.1.1-driver.zip)</li>
			<li>unzip anywhere on your file system</li>
			<li>copy the two files "jppf-server.jar" and "jppf-common.jar" into your v2.1 server installation's "lib" directory (replace the existing files with the new ones)</li>
			<li>restart the JPPF server</li>
			<li>no jar deployment is needed for the nodes, they will automatically pick the fixed code from the server</li>
		</ul>

	</div>
	<br>
$template{name="about-page-footer"}$
