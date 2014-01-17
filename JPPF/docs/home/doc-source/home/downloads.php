<?php $currentPage="Download" ?>
$template{name="about-page-header" title="Downloads"}$

  <?php
    $jppfVersion = "4.0";
    $base = "http://sourceforge.net/projects/jppf-project/files/jppf-project/jppf%20" . $jppfVersion . "/";
  ?>
  <a name="4.0"></a>
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
  User Guide: <a href="/doc/v3">view online</a> or <a href="<?php echo $base . 'JPPF-' . $jppfVersion . '-User-Guide.zip/download'; ?>">download the PDF</a><br>
  API documentation: <a href="/api-3">browse online</a> or <a href="<?php echo $base . 'JPPF-' . $jppfVersion . '-api.zip/download'; ?>">download</a><p>

  <h3>Connectors and add-ons</h3>
  <a href="<?php echo $base . 'JPPF-' . $jppfVersion . '-j2ee-connector.zip/download'; ?>">J2EE Connector</a><br>
  <!--
  <a href="<?php echo $base . 'JPPF-' . $jppfVersion . '-jdk7-addons.zip/download'; ?>">JDK 7+ add-ons</a><br>
  <a href="<?php echo $base . 'JPPF-' . $jppfVersion . '-GigaSpaces.zip/download'; ?>">GigaSpaces XAP connector</a><br>
  -->

  <h3>Samples and tutorials</h3>
  <a href="<?php echo $base . 'JPPF-' . $jppfVersion . '-samples-pack.zip/download'; ?>">JPPF samples pack</a><br>
  Make sure to get started with our <a href="/doc/v4/index.php?title=A_first_taste_of_JPPF">online tutorial</a><br/>&nbsp;

  <hr>

  <h3>All JPPF releases</h3>
  All JPPF files can be found from <a href="http://sourceforge.net/projects/jppf-project/files/jppf-project"><b>this location</b></a>.<br/>&nbsp;

  <hr>

  <h3>Feeling curious or adventurous about JPPF's latest advances?</h3>
  <a href="http://sourceforge.net/projects/jppf-project/files/jppf-project/latest-unstable"><b>Try our latest (unstable) version - last update: 1/2/2014 JPPF 4.0 RC1</b></a>.
  <p><span style="color: #FF7F00">Warning: this is not recommended for production environments</span>

$template{name="about-page-footer"}$
