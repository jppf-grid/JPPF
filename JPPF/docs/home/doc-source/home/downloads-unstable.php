<?php
  $currentPage = "download-unstable";
  $tag1 = "v_6_1_alpha_2";
  $ver1 = "6.1-alpha-2";
  $base = "https://github.com/jppf-grid/JPPF/releases/download/" . $tag1 . "/";
?>
$template{name="about-page-header" title="Downloads - unstable"}$
  <h1 align="center">Downloads - JPPF 6.1 alpha 2 preview</h1>

  <div class="blockWithHighlightedTitle" style="vertical-align: middle">
    <table style="padding: 2px"><tr>
      <td style="width: 20px"><img src="images/icons/folder-download.png"></td>
      <td><h4>All JPPF releases:</h4></td>
      $template{name="download-link" label="Maven Central" link="https://search.maven.org/#search|ga|1|g:org.jppf"}$
      $template{name="download-link" label="Releases on Github" link="https://github.com/jppf-grid/JPPF/tags"}$
      $template{name="download-link" label="Older releases on SF.net" link="https://sourceforge.net/projects/jppf-project/files/jppf-project"}$
    </tr></table>
  </div>
  <br>

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

      <h3>Source code and documentation</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo 'https://github.com/lolocohen/JPPF/archive/' . $tag1 . '.zip'; ?>">Full source code distribution</a></li>
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
        <li>Make sure to get started with our <a href="/doc/6.1/index.php?title=A_first_taste_of_JPPF">online tutorial</a><br/></li>
      </ul>
    </div>
    <br>
  </div>

  <div class="column_right" style="text-align: justify; padding: 0px;">
    <div class="blockWithHighlightedTitle" style="padding-right: 10px">
      $template{name="highlighted-title-with-icon" img="images/icons/warning.png" title="Please note" heading="h2"}$
      <h4 style="font-style: italic; font-weight: 900">JPPF <?php echo $ver1 ?> is a preview release and is not intended for deployment in production</h4>
      <p>You are welcome to try it and provide feedback in our <a href="/forums">user forums</a>, as well as register bugs or enhancement requests in our <a href="/tracker/tbg/jppf/issues/find/saved_search/8/search/1">issue tracker</a>,
      so we have a chance to improve it before the final release.
    </div>
    <div style="height: 8px"></div>
    <div class="blockWithHighlightedTitle">
      $template{name="highlighted-title-with-icon" img="images/icons/documentation.png" title="Documentation preview" heading="h3"}$
      <p>Doc preview for the major new features:
      <!--
      <ul class="samplesList">
        <li><a href="/doc/6.1">JPPF 6.1 User Guide</a></li>
        <li><a href="/javadoc/6.1">JPPF 6.1 Javadoc</a></li>
      </ul>
      -->

      <table style="width: 100%"><tr>
        <td valign="top">
          <ul class="samplesList">
            <li><a href="/doc/6.1">JPPF 6.1 User Guide</a></li>
            <li><a href="/javadoc/6.1">JPPF 6.1 Javadoc</a></li>
          </ul>
        </td>
        <td valign="top">
          <ul class="samplesList">
            <li><a href="/release_notes.php?version=6.1-alpha-2">Release notes</a></li>
          </ul>
        </td>
      </tr></table>

    </div>
    <div style="height: 8px"></div>
    <div class="blockWithHighlightedTitle">
      $template{name="highlighted-title-with-icon" img="images/icons/folder-download.png" title="JPPF 6.1 roadmap" heading="h3"}$
      <ul class="samplesList">
        <li>The current state of the 6.1 milestone can be found <a href="https://www.jppf.org/tracker/tbg/jppf/issues/find/saved_search/8/search/1"><b>here</b></a>.<br/></li>
        <li><a href="https://www.jppf.org/tracker/tbg/jppf/issues/find/saved_search/18/search/1">Open issues</a> and <a href="https://www.jppf.org/tracker/tbg/jppf/issues/find/saved_search/7/search/1">closed issues</a></li>
      </ul>
    </div>
    <div style="height: 6px"></div>
  </div>

$template{name="about-page-footer"}$
