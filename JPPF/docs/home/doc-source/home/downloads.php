<?php $currentPage="Download" ?>
$template{name="about-page-header" title="Downloads"}$
  <h1 align="center">Downloads</h1>

  <div class="column_left" style="text-align: justify; padding: 0px">
    <div class="blockWithHighlightedTitle">
      <?php
        $ver1 = "5.2.4";
        $base = "http://sourceforge.net/projects/jppf-project/files/jppf-project/jppf%20" . $ver1 . "/";
      ?>
      <a name="<?php echo $ver1 ?>"></a>
      $template{name="highlighted-title-with-icon" img="images/icons/download.png" title="JPPF <?php echo $ver1 ?>" heading="h2"}$
      <h3>Web Installer</h3>
      <a href="<?php echo '/download/' . $ver1 . '/JPPF-' . $ver1 . '-Web-Installer.jar'; ?>">Download the web installer jar</a> and run it by either:
      <ul class="samplesList" style="margin-top: 0pt">
        <li>double-clicking the downloaded file</li>
        <li>typing "<b>java -jar <?php echo 'JPPF-' . $ver1 . '-Web-Installer.jar'; ?></b>"</li>
      </ul>

      <h3>Deployable module binaries</h3>
      <a href="<?php echo $base . 'JPPF-' . $ver1 . '-driver.zip/download'; ?>">JPPF server/driver distribution</a><br>
      <a href="<?php echo $base . 'JPPF-' . $ver1 . '-node.zip/download'; ?>">JPPF node distribution</a><br>
      <a href="<?php echo $base . 'JPPF-' . $ver1 . '-admin-ui.zip/download'; ?>">JPPF administration and monitoring console</a><br>
      <a href="<?php echo $base . 'JPPF-' . $ver1 . '-application-template.zip/download'; ?>">JPPF application template</a>.<p>

      <h3>Deployable .Net binaries</h3>
      <a href="<?php echo $base . 'JPPF-' . $ver1 . '-dotnet.zip/download'; ?>">JPPF .Net demo application</a><br>
      <a href="<?php echo $base . 'JPPF-' . $ver1 . '-node-dotnet.zip/download'; ?>">JPPF .Net-enabled node distribution</a><br>

      <h3>Android Node</h3>
      <a href="<?php echo $base . 'JPPF-' . $ver1 . '-node-android-redist.zip/download'; ?>">Android node app binaries and dependencies</a><br>
      <a href="<?php echo $base . 'JPPF-' . $ver1 . '-node-android-src.zip/download'; ?>">Full source as a Gradle/Android Studio project</a><br>
      <div style="height: 10px"></div>
      <table cellpadding="3"><tr>
        <td valign="middle">
          <a href="<?php echo $base . 'JPPF-' . $ver1 . '-AndroidNode.apk/download'; ?>">You may also<br>
          download the APK<br>
          directly to a device:</a>
        </td>
        <td valign="bottom">
          <a class="yhd" href="<?php echo $base . 'JPPF-' . $ver1 . '-AndroidNode.apk/download'; ?>">
            <img style="vertical-align: middle" src="images/jppf_icon_48x48.png"/><span style="vertical-align: middle">&nbsp;Node APK</span>
          </a><br>
        </td>
      </tr></table>
      <div style="height: 5px"></div>

      <h3>Source code and documentation</h3>
      <a href="<?php echo $base . 'JPPF-' . $ver1 . '-full-src.zip/download'; ?>">Full distribution with source code and required libraries</a><br>
      User Guide: <a href="/doc/v5">view online</a> or <a href="<?php echo $base . 'JPPF-' . $ver1 . '-User-Guide.zip/download'; ?>">download the PDF</a><br>
      API documentation: <a href="/api-5">browse online</a> or <a href="<?php echo $base . 'JPPF-' . $ver1 . '-api.zip/download'; ?>">download</a><p>

      <h3>Connectors and add-ons</h3>
      <a href="<?php echo $base . 'JPPF-' . $ver1 . '-j2ee-connector.zip/download'; ?>">J2EE Connector</a><br>

      <h3>Samples and tutorials</h3>
      <a href="<?php echo $base . 'JPPF-' . $ver1 . '-samples-pack.zip/download'; ?>">JPPF samples pack</a><br>
      Make sure to get started with our <a href="/doc/v5/index.php?title=A_first_taste_of_JPPF">online tutorial</a><br/>&nbsp;
    </div>
    <br>
    <div class="blockWithHighlightedTitle">
      $template{name="highlighted-title-with-icon" img="images/icons/folder-download.png" title="All JPPF releases" heading="h3"}$
      <br>All JPPF files can be found from <a href="http://sourceforge.net/projects/jppf-project/files/jppf-project"><b>this location</b></a>.<br/>&nbsp;
    </div>
    <br>
  </div>

  <div class="column_right" style="text-align: justify; padding: 0px;">
    <div class="blockWithHighlightedTitle">
      <?php
        $ver1 = "5.1.5";
        $base = "http://sourceforge.net/projects/jppf-project/files/jppf-project/jppf%20" . $ver1 . "/";
      ?>
      <a name="<?php echo $ver1 ?>"></a>
      $template{name="highlighted-title-with-icon" img="images/icons/download.png" title="JPPF <?php echo $ver1 ?>" heading="h2"}$
      <h3>Web Installer</h3>
      <a href="<?php echo '/download/' . $ver1 . '/JPPF-' . $ver1 . '-Web-Installer.jar'; ?>">Download the web installer jar</a> and run it by either:
      <ul class="samplesList" style="margin-top: 0pt">
        <li>double-clicking the downloaded file</li>
        <li>typing "<b>java -jar <?php echo 'JPPF-' . $ver1 . '-Web-Installer.jar'; ?></b>"</li>
      </ul>

      <h3>Deployable module binaries</h3>
      <a href="<?php echo $base . 'JPPF-' . $ver1 . '-driver.zip/download'; ?>">JPPF server/driver distribution</a><br>
      <a href="<?php echo $base . 'JPPF-' . $ver1 . '-node.zip/download'; ?>">JPPF node distribution</a><br>
      <a href="<?php echo $base . 'JPPF-' . $ver1 . '-admin-ui.zip/download'; ?>">JPPF administration and monitoring console</a><br>
      <a href="<?php echo $base . 'JPPF-' . $ver1 . '-application-template.zip/download'; ?>">JPPF application template</a>.<p>

      <h3>Deployable .Net binaries</h3>
      <a href="<?php echo $base . 'JPPF-' . $ver1 . '-dotnet.zip/download'; ?>">JPPF .Net demo application</a><br>
      <a href="<?php echo $base . 'JPPF-' . $ver1 . '-node-dotnet.zip/download'; ?>">JPPF .Net-enabled node distribution</a><br>

      <h3>Android Node</h3>
      <a href="<?php echo $base . 'JPPF-' . $ver1 . '-node-android-redist.zip/download'; ?>">Android node app binaries and dependencies</a><br>
      <a href="<?php echo $base . 'JPPF-' . $ver1 . '-node-android-src.zip/download'; ?>">Full source as a Gradle/Android Studio project</a><br>
      <div style="height: 10px"></div>
      <table cellpadding="3"><tr>
        <td valign="middle">
          <a href="<?php echo $base . 'JPPF-' . $ver1 . '-AndroidNode.apk/download'; ?>">You may also<br>
          download the APK<br>
          directly to a device:</a>
        </td>
        <td valign="bottom">
          <a class="yhd" href="<?php echo $base . 'JPPF-' . $ver1 . '-AndroidNode.apk/download'; ?>">
            <img style="vertical-align: middle" src="images/jppf_icon_48x48.png"/><span style="vertical-align: middle">&nbsp;Node APK</span>
          </a><br>
        </td>
      </tr></table>
      <div style="height: 5px"></div>

      <h3>Source code and documentation</h3>
      <a href="<?php echo $base . 'JPPF-' . $ver1 . '-full-src.zip/download'; ?>">Full distribution with source code and required libraries</a><br>
      User Guide: <a href="/doc/v5">view online</a> or <a href="<?php echo $base . 'JPPF-' . $ver1 . '-User-Guide.zip/download'; ?>">download the PDF</a><br>
      API documentation: <a href="/api-5">browse online</a> or <a href="<?php echo $base . 'JPPF-' . $ver1 . '-api.zip/download'; ?>">download</a><p>

      <h3>Connectors and add-ons</h3>
      <a href="<?php echo $base . 'JPPF-' . $ver1 . '-j2ee-connector.zip/download'; ?>">J2EE Connector</a><br>

      <h3>Samples and tutorials</h3>
      <a href="<?php echo $base . 'JPPF-' . $ver1 . '-samples-pack.zip/download'; ?>">JPPF samples pack</a><br>
      Make sure to get started with our <a href="/doc/v5/index.php?title=A_first_taste_of_JPPF">online tutorial</a><br/>&nbsp;
    </div>
    <br>
    <div class="blockWithHighlightedTitle">
      $template{name="highlighted-title-with-icon" img="images/icons/emblem-what.png" title="Feeling curious or adventurous?" heading="h3"}$
      <br><b>Try our latest (unstable) version</b><br>last update: <a href="/downloads-unstable.php" style="font-weight: 900">JPPF 6.0 alpha</a>.
      <br><br>
      <p align="center"><span class="yh" style="color: black; background-color: yellow">Not recommended for production environments</span><br>&nbsp;
    </div>
    <br>
  </div>

$template{name="about-page-footer"}$
