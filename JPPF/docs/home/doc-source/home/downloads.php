<?php $currentPage="Download" ?>
$template{name="about-page-header" title="Downloads"}$
  <h1 align="center">Downloads</h1>

  <div class="column_left" style="text-align: justify; padding: 0px">
    <div style="border: solid 1px #9B9DFD; border-radius: 10px; padding: 5px">
      <?php
        $ver1 = "5.0";
        $base = "http://sourceforge.net/projects/jppf-project/files/jppf-project/jppf%20" . $ver1 . "/";
      ?>
      <a name="<?php echo $ver1 ?>"></a>
      <div style="border-bottom: solid 1px #9B9DFD">
      $template{name="title-with-icon" img="images/icons/download.png" title="JPPF <?php echo $ver1 ?>" heading="h2"}$
      </div>
      <h3>Installer</h3>
      <a href="/download/jppf_ws-5.0.jnlp">Start the web installer by clicking here</a> (requires Java Web Start 1.5 or later)<br>

      <h3>Deployable module binaries</h3>
      <a href="<?php echo $base . 'JPPF-' . $ver1 . '-driver.zip/download'; ?>">JPPF server/driver distribution</a><br>
      <a href="<?php echo $base . 'JPPF-' . $ver1 . '-node.zip/download'; ?>">JPPF node distribution</a><br>
      <a href="<?php echo $base . 'JPPF-' . $ver1 . '-admin-ui.zip/download'; ?>">JPPF administration and monitoring console</a><br>
      <a href="<?php echo $base . 'JPPF-' . $ver1 . '-application-template.zip/download'; ?>">JPPF application template</a> (fully working, fully commented, to use as a starting point).<p>

      <h3>Deployable .Net binaries</h3>
      <a href="<?php echo $base . 'JPPF-' . $ver1 . '-dotnet.zip/download'; ?>">JPPF .Net demo application</a><br>
      <a href="<?php echo $base . 'JPPF-' . $ver1 . '-node-dotnet.zip/download'; ?>">JPPF .Net-enabled node distribution</a><br>

      <h3>Source code and documentation</h3>
      <a href="<?php echo $base . 'JPPF-' . $ver1 . '-full-src.zip/download'; ?>">Full distribution with source code and required libraries</a><br>
      User Guide: <a href="/doc/v4">view online</a> or <a href="<?php echo $base . 'JPPF-' . $ver1 . '-User-Guide.zip/download'; ?>">download the PDF</a><br>
      API documentation: <a href="/api">browse online</a> or <a href="<?php echo $base . 'JPPF-' . $ver1 . '-api.zip/download'; ?>">download</a><p>

      <h3>Connectors and add-ons</h3>
      <a href="<?php echo $base . 'JPPF-' . $ver1 . '-j2ee-connector.zip/download'; ?>">J2EE Connector</a><br>

      <h3>Samples and tutorials</h3>
      <a href="<?php echo $base . 'JPPF-' . $ver1 . '-samples-pack.zip/download'; ?>">JPPF samples pack</a><br>
      Make sure to get started with our <a href="/doc/v4/index.php?title=A_first_taste_of_JPPF">online tutorial</a><br/>&nbsp;
    </div>
    <br>
    <div style="border: solid 1px #9B9DFD; border-radius: 10px; padding: 5px">
      $template{name="title-with-icon" img="images/icons/folder-download.png" title="All JPPF releases" heading="h3"}$
      All JPPF files can be found from <a href="http://sourceforge.net/projects/jppf-project/files/jppf-project"><b>this location</b></a>.<br/>&nbsp;
    </div>
  </div>

  <div class="column_right" style="text-align: justify; padding: 0px;">
    <div style="border: solid 1px #9B9DFD; border-radius: 10px; padding: 5px">
      <?php
        $ver1 = "4.2.6";
        $base = "http://sourceforge.net/projects/jppf-project/files/jppf-project/jppf%20" . $ver1 . "/";
      ?>
      <a name="<?php echo $ver1 ?>"></a>

      <div style="border-bottom: solid 1px #9B9DFD">
      $template{name="title-with-icon" img="images/icons/download.png" title="JPPF <?php echo $ver1 ?>" heading="h2"}$
      </div>

      <h3>Installer</h3>
      <a href="/download/jppf_ws-4.2.jnlp">Start the web installer by clicking here</a> (requires Java Web Start 1.5 or later)<br>

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
    </div>
    <br><br>
    <div style="border: solid 1px #9B9DFD; border-radius: 10px; padding: 5px">
      $template{name="title-with-icon" img="images/icons/curious2.png" title="Feeling curious or adventurous?" heading="h3"}$
      <b>Try our latest (unstable) version</b><br>last update: <a href="http://sourceforge.net/projects/jppf-project/files/jppf-project/jppf 5.0 rc1" style="font-weight: 900">2/12/2015 JPPF 5.0 RC1</a>.
      <p>
      <div align="center" style="background-color: yellow; padding: 5px; margin: 5px; border-radius: 10px"><b><i>Not recommended for production environments</b></i></div>
    </div>
  </div>

$template{name="about-page-footer"}$
