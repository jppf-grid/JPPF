<?php $currentPage="Download" ?>
$template{name="about-page-header" title="Downloads"}$
  <h1 align="center">Downloads</h1>

  <div class="column_left" style="text-align: justify; padding: 0px">
    <div class="blockWithHighlightedTitle">
      <?php
        $ver1 = "5.2.8";
        $base = "http://sourceforge.net/projects/jppf-project/files/jppf-project/jppf%20" . $ver1 . "/";
      ?>
      <a name="<?php echo $ver1 ?>"></a>
      $template{name="highlighted-title-with-icon" img="images/icons/download.png" title="JPPF <?php echo $ver1 ?>" heading="h1" style="margin: 10px 0px"}$
      <h2>Web Installer</h2>
      <a href="<?php echo '/download/' . $ver1 . '/JPPF-' . $ver1 . '-Web-Installer.jar'; ?>">Download the web installer jar</a> and run it by either:
      <ul class="list_nomargin">
        <li>double-clicking the downloaded file</li>
        <li>typing "<b>java -jar <?php echo 'JPPF-' . $ver1 . '-Web-Installer.jar'; ?></b>"</li>
      </ul>

      <h2>Deployable module binaries</h2>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-driver.zip/download'; ?>">JPPF server/driver distribution</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-node.zip/download'; ?>">JPPF node distribution</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-admin-ui.zip/download'; ?>">JPPF administration and monitoring console</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-application-template.zip/download'; ?>">JPPF application template</a>.</li>
      </ul>

      <h2>Deployable .Net binaries</h2>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-dotnet.zip/download'; ?>">JPPF .Net demo application</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-node-dotnet.zip/download'; ?>">JPPF .Net-enabled node distribution</a></li>
      </ul>

      <h2>Android Node</h2>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-node-android-redist.zip/download'; ?>">Android node app binaries and dependencies</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-node-android-src.zip/download'; ?>">Full source as a Gradle/Android Studio project</a></li>
        <li style="padding: 5px 0px">
          <table cellpadding="0"><tr>
            <td valign="bottom">
              <a href="<?php echo $base . 'JPPF-' . $ver1 . '-AndroidNode.apk/download'; ?>">You may also download the<br>APK directly to a device:</a>
            </td>
            <td>&nbsp;</td>
            <td valign="middle" style="white-space: nowrap">
              <a class="yhd" href="<?php echo $base . 'JPPF-' . $ver1 . '-AndroidNode.apk/download'; ?>">
               <span style="vertical-align: top">Node APK</span>
              </a><br>
            </td>
          </tr></table>
        </li>
      </ul>

      <h2>Sources and documentation</h2>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-full-src.zip/download'; ?>">Full distribution with source code and required libraries</a></li>
        <li>User Guide: <a href="/doc/5.2">view online</a> or <a href="<?php echo $base . 'JPPF-' . $ver1 . '-User-Guide.zip/download'; ?>">download the PDF</a></li>
        <li>API documentation: <a href="/api-5">browse online</a> or <a href="<?php echo $base . 'JPPF-' . $ver1 . '-api.zip/download'; ?>">download</a></li>
      </ul>

      <h2>Connectors and add-ons</h2>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-j2ee-connector.zip/download'; ?>">J2EE Connector</a></li>
      </ul>

      <h2>Samples and tutorials</h2>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-samples-pack.zip/download'; ?>">JPPF samples pack</a></li>
        <li>Make sure to get started with our <a href="/doc/v5/index.php?title=A_first_taste_of_JPPF">online tutorial</a></li>
      </ul>
    </div>
    <br>
    <div class="blockWithHighlightedTitle">
      $template{name="highlighted-title-with-icon" img="images/icons/folder-download.png" title="All JPPF releases" heading="h2"}$
      <br>All JPPF files can be found from <a href="http://sourceforge.net/projects/jppf-project/files/jppf-project"><b>this location</b></a>.<br/>&nbsp;
    </div>
    <br>
  </div>

  <div class="column_right" style="text-align: justify; padding: 0px;">
    <div class="blockWithHighlightedTitle">
      <?php
        $ver1 = "5.1.6";
        $base = "http://sourceforge.net/projects/jppf-project/files/jppf-project/jppf%20" . $ver1 . "/";
      ?>
      <a name="<?php echo $ver1 ?>"></a>
      $template{name="highlighted-title-with-icon" img="images/icons/download.png" title="JPPF <?php echo $ver1 ?>" heading="h1" style="margin: 10px 0px"}$
      <h2>Web Installer</h2>
      <a href="<?php echo '/download/' . $ver1 . '/JPPF-' . $ver1 . '-Web-Installer.jar'; ?>">Download the web installer jar</a> and run it by either:
      <ul class="samplesList" style="margin-top: 0pt">
        <li>double-clicking the downloaded file</li>
        <li>typing "<b>java -jar <?php echo 'JPPF-' . $ver1 . '-Web-Installer.jar'; ?></b>"</li>
      </ul>

      <h2>Deployable module binaries</h2>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-driver.zip/download'; ?>">JPPF server/driver distribution</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-node.zip/download'; ?>">JPPF node distribution</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-admin-ui.zip/download'; ?>">JPPF administration and monitoring console</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-application-template.zip/download'; ?>">JPPF application template</a></li>
      </ul>

      <h2>Deployable .Net binaries</h2>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-dotnet.zip/download'; ?>">JPPF .Net demo application</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-node-dotnet.zip/download'; ?>">JPPF .Net-enabled node distribution</a></li>
      </ul>

      <h2>Android Node</h2>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-node-android-redist.zip/download'; ?>">Android node app binaries and dependencies</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-node-android-src.zip/download'; ?>">Full source as a Gradle/Android Studio project</a></li>
        <li style="padding: 5px 0px">
          <table cellpadding="0"><tr>
            <td valign="middle">
              <a href="<?php echo $base . 'JPPF-' . $ver1 . '-AndroidNode.apk/download'; ?>">You may also download the<br>APK directly to a device:</a>
            </td>
            <td>&nbsp;</td>
            <td valign="middle" style="white-space: nowrap">
              <a class="yhd" href="<?php echo $base . 'JPPF-' . $ver1 . '-AndroidNode.apk/download'; ?>">
               <span style="vertical-align: top">Node APK</span>
              </a><br>
            </td>
          </tr></table>
        </li>
      </ul>

      <h2>Sources and documentation</h2>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-full-src.zip/download'; ?>">Full distribution with source code and required libraries</a></li>
        <li>User Guide: <a href="/doc/v5">view online</a> or <a href="<?php echo $base . 'JPPF-' . $ver1 . '-User-Guide.zip/download'; ?>">download the PDF</a></li>
        <li>API documentation: <a href="/api-5">browse online</a> or <a href="<?php echo $base . 'JPPF-' . $ver1 . '-api.zip/download'; ?>">download</a></li>
      </ul>

      <h2>Connectors and add-ons</h2>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-j2ee-connector.zip/download'; ?>">J2EE Connector</a></li>
      </ul>

      <h2>Samples and tutorials</h2>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-samples-pack.zip/download'; ?>">JPPF samples pack</a></li>
        <li>Make sure to get started with our <a href="/doc/v5/index.php?title=A_first_taste_of_JPPF">online tutorial</a></li>
      </ul>
    </div>
    <br>
    <div class="blockWithHighlightedTitle">
      $template{name="highlighted-title-with-icon" img="images/icons/emblem-what.png" title="Curious or adventurous?" heading="h2"}$
      <div style="height: 5px"></div>
      <b>Try our latest (unstable) version</b>: <a href="/downloads-unstable.php" style="font-weight: 900">JPPF 6.0 alpha 3</a>.<br>
      <div style="height: 5px"></div>
      <p align="left"><span class="yh" style="color: black; background-color: yellow">Not recommended for production environments</span><br>&nbsp;
    </div>
    <br>
  </div>

$template{name="about-page-footer"}$
