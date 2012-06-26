<?php $currentPage="Download" ?>
$template{name="about-page-header" title="Downloads"}$

	<?php
		$jppfVersion = "3.1";
		$base = "http://sourceforge.net/projects/jppf-project/files/jppf-project/jppf%20" . $jppfVersion . "/";
	?>
	<a name="2.0"></a>
	<h1>JPPF <?php echo $jppfVersion ?></h1>

	<h3>Installer</h3>
	<a href="/download/jppf_ws.jnlp">Start the web installer by clicking here</a> (requires Java Web Start 1.5 or later)<br>

	<h3>Deployable module binaries</h3>
	<a href="<?php echo $base . 'JPPF-' . $jppfVersion . '-driver.zip/download'; ?>">JPPF server/driver distribution</a><br>
	<a href="<?php echo $base . 'JPPF-' . $jppfVersion . '-node.zip/download'; ?>">JPPF node distribution</a><br>
	<a href="<?php echo $base . 'JPPF-' . $jppfVersion . '-admin-ui.zip/download'; ?>">JPPF administration and monitoring console</a><br>
	<a href="<?php echo $base . 'JPPF-' . $jppfVersion . '-application-template.zip/download'; ?>">JPPF application template</a> (fully working, fully commented, to use as a starting point).<p>

	<h3>Source code and documentation</h3>
	<a href="<?php echo $base . 'JPPF-' . $jppfVersion . '-full-src.zip/download'; ?>">Full distribution with source code and required libraries</a><br>
	User Guide: <a href="/wiki">view online</a> or <a href="<?php echo $base . 'JPPF-' . $jppfVersion . '-User-Guide.zip/download'; ?>">download the PDF</a><br>
	API documentation: <a href="/api-2.0">browse online</a> or <a href="<?php echo $base . 'JPPF-' . $jppfVersion . '-api.zip/download'; ?>">download</a><p>

	<h3>Connectors and add-ons</h3>
	<a href="<?php echo $base . 'JPPF-' . $jppfVersion . '-j2ee-connector.zip/download'; ?>">J2EE Connector</a><br>
	<a href="<?php echo $base . 'JPPF-' . $jppfVersion . '-jdk7-addons.zip/download'; ?>">JDK 7+ add-ons</a><br>
	<a href="<?php echo $base . 'JPPF-' . $jppfVersion . '-GigaSpaces.zip/download'; ?>">GigaSpaces XAP connector</a><br>

	<h3>Samples and tutorials</h3>
	<a href="<?php echo $base . 'JPPF-' . $jppfVersion . '-samples-pack.zip/download'; ?>">JPPF samples pack</a><br>
	Make sure to get started with our <a href="/wiki/index.php?title=A_first_taste_of_JPPF">online tutorial</a><br/>&nbsp;

	<hr>

	<h3>All JPPF releases</h3>
	All JPPF files can be found from <a href="http://sourceforge.net/projects/jppf-project/files/jppf-project"><b>this location</b></a>.

$template{name="about-page-footer"}$
