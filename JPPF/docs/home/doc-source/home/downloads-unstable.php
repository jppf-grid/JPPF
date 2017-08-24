<?php
  $currentPage = "Download";
  $ver1 = "6.0-alpha-2";
  $base = "http://sourceforge.net/projects/jppf-project/files/jppf-project/latest-unstable/";
?>
$template{name="about-page-header" title="Downloads - unstable"}$
  <h1 align="center">Downloads - JPPF 6.0 alpha 2 preview</h1>

  <div class="column_left" style="text-align: justify; padding: 0px">
    <div class="blockWithHighlightedTitle">
      <a name="<?php echo $ver1 ?>"></a>
      $template{name="highlighted-title-with-icon" img="images/icons/download.png" title="JPPF <?php echo $ver1 ?>" heading="h1" style="margin: 10px 0px"}$
      <h3>Deployable module binaries</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-driver.zip/download'; ?>">JPPF server/driver distribution</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-node.zip/download'; ?>">JPPF node distribution</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-admin-ui.zip/download'; ?>">JPPF administration and monitoring console</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-application-template.zip/download'; ?>">JPPF application template</a></li>
      </ul>

      <h3>Deployable .Net binaries</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-dotnet.zip/download'; ?>">JPPF .Net demo application</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-node-dotnet.zip/download'; ?>">JPPF .Net-enabled node distribution</a></li>
      </ul>

      <h3>Android Node</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-node-android-redist.zip/download'; ?>">Android node app binaries and dependencies</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-node-android-src.zip/download'; ?>">Full source as a Gradle/Android Studio project</a></li>
        <li><table cellpadding="3"><tr>
          <td valign="middle">
            <a href="<?php echo $base . 'JPPF-' . $ver1 . '-AndroidNode.apk/download'; ?>">You may also download the<br>APK directly to a device:</a>
          </td>
          <td>&nbsp;</td>
          <td valign="middle" style="white-space: nowrap">
            <a class="yhd" href="<?php echo $base . 'JPPF-' . $ver1 . '-AndroidNode.apk/download'; ?>">
              <span style="vertical-align: top">Node APK</span>
            </a><br>
          </td>
        </tr></table></li>
      </ul>
      <div style="height: 5px"></div>

      <h3>Source code and documentation</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-full-src.zip/download'; ?>">Full distribution with source code and required libraries</a></li>
        <li>User Guide: <a href="/doc/v5">view online</a> or <a href="<?php echo $base . 'JPPF-' . $ver1 . '-User-Guide.zip/download'; ?>">download the PDF</a></li>
        <li>API documentation: <a href="/api-5">browse online</a> or <a href="<?php echo $base . 'JPPF-' . $ver1 . '-api.zip/download'; ?>">download</a></li>
      </ul>

      <h3>Connectors and add-ons</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-j2ee-connector.zip/download'; ?>">J2EE Connector</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-admin-web.zip/download'; ?>">Web administration console</a></li>
      </ul>

      <h3>Samples and tutorials</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-samples-pack.zip/download'; ?>">JPPF samples pack</a></li>
        <li>Make sure to get started with our <a href="/doc/v5/index.php?title=A_first_taste_of_JPPF">online tutorial</a><br/></li>
      </ul>
    </div>
    <br>
  </div>

  <div class="column_right" style="text-align: justify; padding: 0px;">
    <div class="blockWithHighlightedTitle">
      $template{name="highlighted-title-with-icon" img="images/icons/warning.png" title="Please note" heading="h2"}$
      <p style="font-style: italic; font-weight: bold">JPPF <?php echo $ver1 ?> is a preview release and is not intended for deployment in production.
      <p>You are welcome to try it and provide feedback in our <a href="/forums">user forums</a>, as well as register bugs or enhancement requests in our <a href="/tracker/tbg/jppf/issues/find/saved_search/8/search/1">issue tracker</a>,
      so we have a chance to improve it before the final release.
    </div>
    <br>
    <div class="blockWithHighlightedTitle">
      $template{name="highlighted-title-with-icon" img="images/icons/documentation.png" title="Documentation preview" heading="h3"}$
      <p>Doc preview for the major new features:
      <ul class="samplesList">
        <li><a href="/doc/6.0">JPPF 6.0 User Guide</a></li>
        <li><a href="/javadoc/6.0">JPPF 6.0 Javadoc</a></li>
        <li><a href="/csdoc/6.0">JPPF 6.0 C-Sharp Doc</a></li>
      </ul>
    </div>
    <br>
    <div class="blockWithHighlightedTitle">
      $template{name="highlighted-title-with-icon" img="images/icons/folder-download.png" title="All JPPF releases" heading="h3"}$
      <br>All JPPF files can be found from <a href="http://sourceforge.net/projects/jppf-project/files/jppf-project"><b>this location</b></a>.<br/>&nbsp;
    </div>
    <br>
  </div>

$template{name="about-page-footer"}$
