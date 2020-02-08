<?php $currentPage="Download" ?>
$template{name="about-page-header" title="Downloads"}$
  <table style="width: 100%"><tr>
    <td style="width: 59%"><h1 align="right">Downloads</h1></td>
    <td style="font-size: 10pt"><div align="right">Powered by<a href="https://www.ej-technologies.com/products/jprofiler/overview.html"> <img src="https://www.ej-technologies.com/images/product_banners/jprofiler_small.png"/><br>Java profiler</a></div></td>
  </tr></table>

  <div class="blockWithHighlightedTitle" style="vertical-align: middle">
    <div style="margin: 10px 5px">
      <img src="images/icons/folder-download.png" width="20"/>
      <h4 style="display: inline">All JPPF releases:</h4>
      <ul class="inline">
        $template{name="download-link" label="Maven Central" link="https://search.maven.org/#search|ga|1|g:org.jppf"}$
        $template{name="download-link" label="Releases on Github" link="https://github.com/jppf-grid/JPPF/releases"}$
        $template{name="download-link" label="Older releases on SF.net" link="https://sourceforge.net/projects/jppf-project/files/jppf-project"}$
      </ul>
    </div>
  </div>
  <br>

  <div class="column_left" style="text-align: justify; padding: 0px; font-size: 11pt">
    <div class="blockWithHighlightedTitle">
      <?php
        $tag1 = "v_6_2";
        $ver1 = "6.2";
        $base = "https://github.com/jppf-grid/JPPF/releases/download/" . $tag1 . "/";
      ?>
      <a name="<?php echo $ver1 ?>"></a>
      $template{name="highlighted-title-with-icon" img="images/icons/download.png" title="JPPF <?php echo $ver1 ?>" heading="h1" style="margin: 10px 0px"}$

      $template{name="highlighted-title-with-icon" img="images/icons/warning.png" title="Requires Java 8 or later" heading="h2"}$

      <h3>Web Installer</h3>
      <a href="<?php echo '/download/' . $ver1 . '/JPPF-' . $ver1 . '-Web-Installer.jar'; ?>">Download the web installer jar</a> and run it by either:
      <ul class="list_nomargin">
        <li>double-clicking the downloaded file</li>
        <li>typing "<b>java -jar <?php echo 'JPPF-' . $ver1 . '-Web-Installer.jar'; ?></b>"</li>
      </ul>

      <h3>Deployable JPPF binaries</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-driver.zip'; ?>">Server/driver distribution</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-node.zip'; ?>">Node distribution</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-application-template.zip'; ?>">Application template</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-admin-ui.zip'; ?>">Desktop administration and monitoring console</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-admin-web.zip'; ?>">Web administration and monitoring console</a></li>
      </ul>

      <h3>Source code and documentation</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo 'https://github.com/jppf-grid/JPPF/archive/' . $tag1 . '.zip'; ?>">Full source code distribution</a></li>
        <li>User Guide: <a href="/doc/6.1">view online</a> or <a href="<?php echo $base . 'JPPF-' . $ver1 . '-User-Guide.zip'; ?>">download the PDF</a></li>
        <li>API documentation: <a href="/javadoc/6.1">browse online</a> or <a href="<?php echo $base . 'JPPF-' . $ver1 . '-api.zip'; ?>">download</a></li>
      </ul>

      <h3>Connectors and add-ons</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-j2ee-connector.zip'; ?>">J2EE Connector</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-jmxremote-nio.zip'; ?>">Standalone NIO-based JMX remote connector</a></li>
      </ul>

      <h3>Samples and tutorials</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-samples-pack.zip'; ?>">JPPF samples pack</a></li>
        <li>Make sure to get started with our <a href="/doc/6.2/index.php?title=A_first_taste_of_JPPF">online tutorial</a><br/></li>
      </ul>
    </div>
    <br>

    <!--
    <div class="blockWithHighlightedTitle">
      $template{name="highlighted-title-with-icon" img="images/icons/download.png" title="JPPF 6.2 beta preview" heading="h2" style="margin: 10px 0px; font-weight: bold"}$
      <div style="height: 5px"></div>
      <ul class="list_nomargin">
        <li><a href="release_notes.php?version=6.2-beta">Release notes</a></li>
        <li><a href="downloads-unstable.php">Download</a></li>
      </ul>
    </div>
    -->
    <div style="height: 5px"></div>
  </div>

  <div class="column_right" style="text-align: justify; padding: 0px">
    <div class="blockWithHighlightedTitle">
      <?php
        $tag1 = "v_6_1_4";
        $ver1 = "6.1.4";
        $base = "https://github.com/jppf-grid/JPPF/releases/download/" . $tag1 . "/";
      ?>
      <a name="<?php echo $ver1 ?>"></a>
      $template{name="highlighted-title-with-icon" img="images/icons/download.png" title="JPPF <?php echo $ver1 ?>" heading="h1" style="margin: 10px 0px"}$

      $template{name="highlighted-title-with-icon" img="images/icons/warning.png" title="Requires Java 8 or later" heading="h2"}$

      <h3>Web Installer</h3>
      <a href="<?php echo '/download/' . $ver1 . '/JPPF-' . $ver1 . '-Web-Installer.jar'; ?>">Download the web installer jar</a> and run it by either:
      <ul class="list_nomargin">
        <li>double-clicking the downloaded file</li>
        <li>typing "<b>java -jar <?php echo 'JPPF-' . $ver1 . '-Web-Installer.jar'; ?></b>"</li>
      </ul>

      <h3>Deployable JPPF binaries</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-driver.zip'; ?>">Server/driver distribution</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-node.zip'; ?>">Node distribution</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-application-template.zip'; ?>">Application template</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-admin-ui.zip'; ?>">Desktop administration and monitoring console</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-admin-web.zip'; ?>">Web administration and monitoring console</a></li>
      </ul>

      <h3>Source code and documentation</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo 'https://github.com/jppf-grid/JPPF/archive/' . $tag1 . '.zip'; ?>">Full source code distribution</a></li>
        <li>User Guide: <a href="/doc/6.1">view online</a> or <a href="<?php echo $base . 'JPPF-' . $ver1 . '-User-Guide.zip'; ?>">download the PDF</a></li>
        <li>API documentation: <a href="/javadoc/6.1">browse online</a> or <a href="<?php echo $base . 'JPPF-' . $ver1 . '-api.zip'; ?>">download</a></li>
      </ul>

      <h3>Connectors and add-ons</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-j2ee-connector.zip'; ?>">J2EE Connector</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-jmxremote-nio.zip'; ?>">Standalone NIO-based JMX remote connector</a></li>
      </ul>

      <h3>Samples and tutorials</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-samples-pack.zip'; ?>">JPPF samples pack</a></li>
        <li>Make sure to get started with our <a href="/doc/6.2/index.php?title=A_first_taste_of_JPPF">online tutorial</a><br/></li>
      </ul>
    </div>
    <br>

    <!--
    <div class="blockWithHighlightedTitle">
      $template{name="highlighted-title-with-icon" img="images/icons/download.png" title="JPPF 6.2 beta preview" heading="h2" style="margin: 10px 0px; font-weight: bold"}$
      <div style="height: 5px"></div>
      <ul class="list_nomargin">
        <li><a href="release_notes.php?version=6.2-beta">Release notes</a></li>
        <li><a href="downloads-unstable.php">Download</a></li>
      </ul>
    </div>
    -->
    <div style="height: 5px"></div>
  </div>
  <br>

$template{name="about-page-footer"}$
