<?php $currentPage="Download" ?>
$template{name="about-page-header" title="Downloads"}$
  <table style="width: 100%"><tr>
    <td style="width: 59%"><h1 align="right">Downloads</h1></td>
    <td style="font-size: 10pt"><div align="right">Powered by<a href="https://www.ej-technologies.com/products/jprofiler/overview.html"> <img src="https://www.ej-technologies.com/images/product_banners/jprofiler_small.png"/><br>Java profiler</a></div></td>
  </tr></table>

  <div class="blockWithHighlightedTitle" style="vertical-align: middle">
    <table style="padding: 2px"><tr>
      <td style="width: 20px"><img src="images/icons/folder-download.png"></td>
      <td><h4>All JPPF releases:</h4></td>
      $template{name="download-link" label="Maven Central" link="https://search.maven.org/#search|ga|1|g:org.jppf"}$
      $template{name="download-link" label="Releases on Github" link="https://github.com/jppf-grid/JPPF/tags"}$
      $template{name="download-link" label="Older releases on SF.net" link="https://sourceforge.net/projects/jppf-project/files/jppf-project"}$
      $template{name="download-link" label="JPPF 6.1 alpha preview" link="downloads-unstable.php"}$
    </tr></table>
  </div>
  <br>

  <div class="column_left" style="text-align: justify; padding: 0px">
    <div class="blockWithHighlightedTitle">
      <?php
        $tag1 = "v_6_0_1";
        $ver1 = "6.0.1";
        $base = "https://github.com/jppf-grid/JPPF/releases/download/" . $tag1 . "/";
      ?>
      <a name="<?php echo $ver1 ?>"></a>
      $template{name="highlighted-title-with-icon" img="images/icons/download.png" title="JPPF <?php echo $ver1 ?>" heading="h1" style="margin: 10px 0px"}$
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

      <h3>Deployable .Net binaries</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-dotnet.zip'; ?>">.Net demo application</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-node-dotnet.zip'; ?>">.Net-enabled node distribution</a></li>
      </ul>

      <h3>Android Node</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-node-android-redist.zip'; ?>">Android node app binaries and dependencies</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-node-android-src.zip'; ?>">Full source as a Gradle/Android Studio project</a></li>
        <li><table cellpadding="3"><tr>
          <td valign="middle">
            <a href="<?php echo $base . 'JPPF-' . $ver1 . '-AndroidNode.apk'; ?>">You may also download the<br>APK directly to a device:</a>
          </td>
          <td>&nbsp;</td>
          <td valign="middle" style="white-space: nowrap">
            <a class="yhd2" href="<?php echo $base . 'JPPF-' . $ver1 . '-AndroidNode.apk'; ?>">
              <span style="vertical-align: top">Node APK</span>
            </a><br>
          </td>
        </tr></table></li>
      </ul>

      <h3>Source code and documentation</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo 'https://github.com/lolocohen/JPPF/archive/' . $tag1 . '.zip'; ?>">Full source code distribution</a></li>
        <li>User Guide: <a href="/doc/6.0">view online</a> or <a href="<?php echo $base . 'JPPF-' . $ver1 . '-User-Guide.zip'; ?>">download the PDF</a></li>
        <li>API documentation: <a href="/javadoc/6.0">browse online</a> or <a href="<?php echo $base . 'JPPF-' . $ver1 . '-api.zip'; ?>">download</a></li>
      </ul>

      <h3>Connectors and add-ons</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-j2ee-connector.zip'; ?>">J2EE Connector</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-jmxremote-nio.zip'; ?>">Standalone NIO-based JMX remote connector</a></li>
      </ul>

      <h3>Samples and tutorials</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-samples-pack.zip'; ?>">JPPF samples pack</a></li>
        <li>Make sure to get started with our <a href="/doc/6.0/index.php?title=A_first_taste_of_JPPF">online tutorial</a><br/></li>
      </ul>
    </div>
    <br>
  </div>

  <div class="column_right" style="text-align: justify; padding: 0px">
    <div class="blockWithHighlightedTitle">
      <?php
        $tag2 = "v_5_2_10";
        $ver2 = "5.2.10";
        $base = "https://github.com/jppf-grid/JPPF/releases/download/" . $tag2 . "/";
      ?>
      <a name="<?php echo $ver2 ?>"></a>
      $template{name="highlighted-title-with-icon" img="images/icons/download.png" title="JPPF <?php echo $ver2 ?>" heading="h1" style="margin: 10px 0px"}$
      <h3>Web Installer</h3>
      <a href="<?php echo '/download/' . $ver2 . '/JPPF-' . $ver2 . '-Web-Installer.jar'; ?>">Download the web installer jar</a> and run it by either:
      <ul class="list_nomargin">
        <li>double-clicking the downloaded file</li>
        <li>typing "<b>java -jar <?php echo 'JPPF-' . $ver2 . '-Web-Installer.jar'; ?></b>"</li>
      </ul>

      <h3>Deployable module binaries</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver2 . '-driver.zip'; ?>">JPPF server/driver distribution</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver2 . '-node.zip'; ?>">JPPF node distribution</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver2 . '-admin-ui.zip'; ?>">JPPF administration and monitoring console</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver2 . '-application-template.zip'; ?>">JPPF application template</a>.</li>
      </ul>

      <h3>Deployable .Net binaries</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver2 . '-dotnet.zip'; ?>">JPPF .Net demo application</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver2 . '-node-dotnet.zip'; ?>">JPPF .Net-enabled node distribution</a></li>
      </ul>

      <h3>Android Node</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver2 . '-node-android-redist.zip'; ?>">Android node app binaries and dependencies</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver2 . '-node-android-src.zip'; ?>">Full source as a Gradle/Android Studio project</a></li>
        <li style="padding: 5px 0px">
          <table cellpadding="0"><tr>
            <td valign="bottom">
              <a href="<?php echo $base . 'JPPF-' . $ver2 . '-AndroidNode.apk'; ?>">You may also download the<br>APK directly to a device:</a>
            </td>
            <td>&nbsp;</td>
            <td valign="middle" style="white-space: nowrap">
              <a class="yhd2" href="<?php echo $base . 'JPPF-' . $ver2 . '-AndroidNode.apk'; ?>">
               <span style="vertical-align: top">Node APK</span>
              </a><br>
            </td>
          </tr></table>
        </li>
      </ul>

      <h3>Sources and documentation</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo 'https://github.com/lolocohen/JPPF/archive/' . $tag2 . '.zip'; ?>">Full source code distribution</a></li>
        <li>User Guide: <a href="/doc/5.2">view online</a> or <a href="<?php echo $base . 'JPPF-' . $ver2 . '-User-Guide.zip'; ?>">download the PDF</a></li>
        <li>API documentation: <a href="/javadoc/5.2">browse online</a> or <a href="<?php echo $base . 'JPPF-' . $ver2 . '-api.zip'; ?>">download</a></li>
      </ul>

      <h3>Connectors and add-ons</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver2 . '-j2ee-connector.zip/download'; ?>">J2EE Connector</a></li>
      </ul>

      <h3>Samples and tutorials</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver2 . '-samples-pack.zip/download'; ?>">JPPF samples pack</a></li>
        <li>Make sure to get started with our <a href="/doc/v5/index.php?title=A_first_taste_of_JPPF">online tutorial</a></li>
      </ul>
    </div>
    <br>
  </div>
  <br>

$template{name="about-page-footer"}$
