<?php
  $currentPage = "Download";
  $tag1 = "v_6_0_beta";
  $ver1 = "6.0-beta";
  $base = "https://github.com/lolocohen/JPPF/releases/download/" . $tag1 . "/";
?>
$template{name="about-page-header" title="Downloads - unstable"}$
  <h1 align="center">Downloads - JPPF 6.0 alpha 4 preview</h1>

  <div class="column_left" style="text-align: justify; padding: 0px">
    <div class="blockWithHighlightedTitle">
      <a name="<?php echo $ver1 ?>"></a>
      $template{name="highlighted-title-with-icon" img="images/icons/download.png" title="JPPF <?php echo $ver1 ?>" heading="h1" style="margin: 10px 0px"}$
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
            <a class="yhd" href="<?php echo $base . 'JPPF-' . $ver1 . '-AndroidNode.apk'; ?>">
              <span style="vertical-align: top">Node APK</span>
            </a><br>
          </td>
        </tr></table></li>
      </ul>
      <div style="height: 5px"></div>

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

  <div class="column_right" style="text-align: justify; padding: 0px;">
    <div class="blockWithHighlightedTitle" style="padding-right: 10px">
      $template{name="highlighted-title-with-icon" img="images/icons/warning.png" title="Please note" heading="h2"}$
      <p style="font-style: italic; font-weight: bold">JPPF <?php echo $ver1 ?> is a preview release and is not intended for deployment in production.
      <p>You are welcome to try it and provide feedback in our <a href="/forums">user forums</a>, as well as register bugs or enhancement requests in our <a href="/tracker/tbg/jppf/issues/find/saved_search/8/search/1">issue tracker</a>,
      so we have a chance to improve it before the final release.
    </div>
    <br><br>
    <div class="blockWithHighlightedTitle">
      $template{name="highlighted-title-with-icon" img="images/icons/documentation.png" title="Documentation preview" heading="h3"}$
      <p>Doc preview for the major new features:
      <ul class="samplesList">
        <li><a href="/doc/6.0">JPPF 6.0 User Guide</a></li>
        <li><a href="/javadoc/6.0">JPPF 6.0 Javadoc</a></li>
        <li><a href="/csdoc/6.0">JPPF 6.0 C-Sharp Doc</a></li>
      </ul>
    </div>
    <br><br>
    <div class="blockWithHighlightedTitle">
      $template{name="highlighted-title-with-icon" img="images/icons/folder-download.png" title="JPPF 6.0 roadmap" heading="h3"}$
      <ul class="samplesList">
        <li>The current state of the 6.0 milestone can be found <a href="https://www.jppf.org/tracker/tbg/jppf/issues/find/saved_search/8/search/1"><b>here</b></a>.<br/></li>
        <li><a href="https://www.jppf.org/tracker/tbg/jppf/issues/find/saved_search/18/search/1">Open issues</a></li>
        <li><a href="https://www.jppf.org/tracker/tbg/jppf/issues/find/saved_search/7/search/1">Closed issues</a></li>
      </ul>
    </div>
    <br style="margin-top: 15px">
    <div class="blockWithHighlightedTitle">
      $template{name="highlighted-title-with-icon" img="images/icons/folder-download.png" title="All JPPF releases" heading="h3"}$
      <br>All JPPF files can be found from <a href="https://sourceforge.net/projects/jppf-project/files/jppf-project"><b>this location</b></a>.<br/>&nbsp;
    </div>
    <br>
  </div>

$template{name="about-page-footer"}$
