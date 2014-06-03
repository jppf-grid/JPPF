<?php $currentPage="Download" ?>
$template{name="about-page-header" title="Downloads"}$

  <?php
    $ver1 = "4.1";
    $ver2 = "4.0.3";
    $base = "http://sourceforge.net/projects/jppf-project/files/jppf-project/jppf%20" . $ver1 . "/";
  ?>
  <a name="<?php echo $ver1 ?>"></a>
  <h1>JPPF <?php echo $ver1 ?></h1>

  <h3>Installer</h3>
  <a href="/download/jppf_ws-4.1.jnlp">Start the web installer by clicking here</a> (requires Java Web Start 1.5 or later)<br>

  <h3>Deployable module binaries</h3>
  <a href="<?php echo $base . 'JPPF-' . $ver1 . '-driver.zip/download'; ?>">JPPF server/driver distribution</a><br>
  <a href="<?php echo $base . 'JPPF-' . $ver1 . '-node.zip/download'; ?>">JPPF node distribution</a><br>
  <a href="<?php echo $base . 'JPPF-' . $ver1 . '-admin-ui.zip/download'; ?>">JPPF administration and monitoring console</a><br>
  <a href="<?php echo $base . 'JPPF-' . $ver1 . '-application-template.zip/download'; ?>">JPPF application template</a> (fully working, fully commented, to use as a starting point).<p>

  <h3>Source code and documentation</h3>
  <a href="<?php echo $base . 'JPPF-' . $ver1 . '-full-src.zip/download'; ?>">Full distribution with source code and required libraries</a><br>
  User Guide: <a href="/doc/v4">view online</a> or <a href="<?php echo $base . 'JPPF-' . $ver1 . '-User-Guide.zip/download'; ?>">download the PDF</a><br>
  API documentation: <a href="/api">browse online</a> or <a href="<?php echo $base . 'JPPF-' . $ver1 . '-api.zip/download'; ?>">download</a><p>

  <h3>Connectors and add-ons</h3>
  <a href="<?php echo $base . 'JPPF-' . $ver1 . '-j2ee-connector.zip/download'; ?>">J2EE Connector</a><br>

  <h3>Samples and tutorials</h3>
  <a href="<?php echo $base . 'JPPF-' . $ver1 . '-samples-pack.zip/download'; ?>">JPPF samples pack</a><br>
  Make sure to get started with our <a href="/doc/v4/index.php?title=A_first_taste_of_JPPF">online tutorial</a><br/>&nbsp;

  <hr>

  <a name="<?php echo $ver2 ?>"></a>
  <h1>JPPF <?php echo $ver2 ?></h1>

  <h3>Installer</h3>
  <a href="/download/jppf_ws-4.0.jnlp">Start the web installer by clicking here</a> (requires Java Web Start 1.5 or later)<br>

  <h3>Deployable module binaries</h3>
  <a href="<?php echo $base . 'JPPF-' . $ver2 . '-driver.zip/download'; ?>">JPPF server/driver distribution</a><br>
  <a href="<?php echo $base . 'JPPF-' . $ver2 . '-node.zip/download'; ?>">JPPF node distribution</a><br>
  <a href="<?php echo $base . 'JPPF-' . $ver2 . '-admin-ui.zip/download'; ?>">JPPF administration and monitoring console</a><br>
  <a href="<?php echo $base . 'JPPF-' . $ver2 . '-application-template.zip/download'; ?>">JPPF application template</a> (fully working, fully commented, to use as a starting point).<p>

  <h3>Source code and documentation</h3>
  <a href="<?php echo $base . 'JPPF-' . $ver2 . '-full-src.zip/download'; ?>">Full distribution with source code and required libraries</a><br>
  User Guide: <a href="/doc/v4">view online</a> or <a href="<?php echo $base . 'JPPF-' . $ver2 . '-User-Guide.zip/download'; ?>">download the PDF</a><br>
  API documentation: <a href="/api">browse online</a> or <a href="<?php echo $base . 'JPPF-' . $ver2 . '-api.zip/download'; ?>">download</a><p>

  <h3>Connectors and add-ons</h3>
  <a href="<?php echo $base . 'JPPF-' . $ver2 . '-j2ee-connector.zip/download'; ?>">J2EE Connector</a><br>

  <h3>Samples and tutorials</h3>
  <a href="<?php echo $base . 'JPPF-' . $ver2 . '-samples-pack.zip/download'; ?>">JPPF samples pack</a><br>
  Make sure to get started with our <a href="/doc/v4/index.php?title=A_first_taste_of_JPPF">online tutorial</a><br/>&nbsp;

  <hr>

  <h3>All JPPF releases</h3>
  All JPPF files can be found from <a href="http://sourceforge.net/projects/jppf-project/files/jppf-project"><b>this location</b></a>.<br/>&nbsp;

  <hr>

  <h3>Feeling curious or adventurous about JPPF's latest advances?</h3>
  <a href="http://sourceforge.net/projects/jppf-project/files/jppf-project/latest-unstable"><b>Try our latest (unstable) version - last update: 3/30/2014 JPPF 4.1 beta</b></a>.
  <p><span style="color: #FF4000"><b><i>Warning: this is not recommended for production environments</b></i></span>

$template{name="about-page-footer"}$
