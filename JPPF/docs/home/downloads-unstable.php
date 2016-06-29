<?php
  $currentPage = "Download";
  $ver1 = "5.2-beta2";
  $base = "http://sourceforge.net/projects/jppf-project/files/jppf-project/latest-unstable/";
?>
<html>
	  <head>
    <title>JPPF Downloads - unstable
</title>
    <meta name="description" content="The open source grid computing solution">
    <meta name="keywords" content="JPPF, java, parallel computing, distributed computing, grid computing, parallel, distributed, cluster, grid, cloud, open source, android, .net">
    <meta HTTP-EQUIV="Content-Type" content="text/html; charset=UTF-8">
    <link rel="shortcut icon" href="images/jppf-icon.ico" type="image/x-icon">
    <link rel="stylesheet" type="text/css" href="/jppf.css" title="Style">
  </head>
	<body>
		<div align="center">
		<div class="gwrapper" align="center">
			<div style="display: none">JPPF, java, parallel computing, distributed computing, grid computing, parallel, distributed, cluster, grid, cloud, open source, android, .net</div>
    <?php
    if (!isset($currentPage)) {
      $currentPage = $_REQUEST["page"];
      if (($currentPage == NULL) || ($currentPage == "")) {
        $currentPage = "Home";
      }
    }
    if ($currentPage != "Forums") {
    ?>
    <div style="background-color: #E2E4F0; margin: 0px;height: 10px"><img src="/images/frame_top.gif"/></div>
    <?php
    }
    ?>
    <table width="100%" cellspacing="0" cellpadding="0" border="0" class="jppfheader" style="border-left: 1px solid #6D78B6; border-right: 1px solid #6D78B6">
      <tr style="height: 80px">
        <td width="15"></td>
        <td width="191" align="left" valign="center"><a href="/"><img src="/images/logo2.gif" border="0" alt="JPPF" style="box-shadow: 4px 4px 4px #6D78B6;"/></a></td>
        <td width="130" align="center"><h3 class="header_slogan">The open source<br>grid computing<br>solution</h3></td>
        <td width="80"></td>
        <td align="right">
          <table border="0" cellspacing="0" cellpadding="0" style="height: 30px; background-color:transparent;">
            <tr>
              <td style="width: 1px"></td>
              <?php $cl = (($currentPage == "Home") ? "headerMenuItem2" : "headerMenuItem") . " " . "header_item_start"; ?>
<td class="<?php echo $cl; ?>">&nbsp;<a href="/index.php" class="<?php echo $cl; ?>">Home</a>&nbsp;</td>
<td style="width: 1px"></td>
              <?php $cl = (($currentPage == "About") ? "headerMenuItem2" : "headerMenuItem") . " " . ""; ?>
<td class="<?php echo $cl; ?>">&nbsp;<a href="/about.php" class="<?php echo $cl; ?>">About</a>&nbsp;</td>
<td style="width: 1px"></td>
              <?php $cl = (($currentPage == "Features") ? "headerMenuItem2" : "headerMenuItem") . " " . ""; ?>
<td class="<?php echo $cl; ?>">&nbsp;<a href="/features.php" class="<?php echo $cl; ?>">Features</a>&nbsp;</td>
<td style="width: 1px"></td>
              <?php $cl = (($currentPage == "Download") ? "headerMenuItem2" : "headerMenuItem") . " " . ""; ?>
<td class="<?php echo $cl; ?>">&nbsp;<a href="/downloads.php" class="<?php echo $cl; ?>">Download</a>&nbsp;</td>
<td style="width: 1px"></td>
              <?php $cl = (($currentPage == "Documentation") ? "headerMenuItem2" : "headerMenuItem") . " " . ""; ?>
<td class="<?php echo $cl; ?>">&nbsp;<a href="/doc" class="<?php echo $cl; ?>">Documentation</a>&nbsp;</td>
<td style="width: 1px"></td>
              <?php $cl = (($currentPage == "Forums") ? "headerMenuItem2" : "headerMenuItem") . " " . "header_item_end"; ?>
<td class="<?php echo $cl; ?>">&nbsp;<a href="/forums" class="<?php echo $cl; ?>">Forums</a>&nbsp;</td>
<td style="width: 1px"></td>
            </tr>
          </table>
        </td>
        <td width="15"></td>
      </tr>
    </table>
			<table border="0" cellspacing="0" cellpadding="5" width="100%px" style="border: 1px solid #6D78B6; border-top: 8px solid #6D78B6;">
			<tr>
				<td style="background-color: #FFFFFF">
				<div class="sidebar">
					        <?php if ($currentPage == "Home") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/" class="<?php echo $itemClass; ?>">&raquo; Home</a><br></div>
        <?php if ($currentPage == "About") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/about.php" class="<?php echo $itemClass; ?>">&raquo; About</a><br></div>
        <?php if ($currentPage == "Download") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/downloads.php" class="<?php echo $itemClass; ?>">&raquo; Download</a><br></div>
        <?php if ($currentPage == "Features") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/features.php" class="<?php echo $itemClass; ?>">&raquo; Features</a><br></div>
        <?php if ($currentPage == "Patches") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/patches.php" class="<?php echo $itemClass; ?>">&raquo; Patches</a><br></div>
        <?php if ($currentPage == "Samples") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/samples-pack/index.php" class="<?php echo $itemClass; ?>">&raquo; Samples</a><br></div>
        <?php if ($currentPage == "License") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/license.php" class="<?php echo $itemClass; ?>">&raquo; License</a><br></div>
        <hr/>
                <?php if ($currentPage == "All docs") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/doc" class="<?php echo $itemClass; ?>">&raquo; All docs</a><br></div>
        <?php if ($currentPage == "v5.1") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/5.1" class="<?php echo $itemClass; ?>">v5.1</a><br></div>
        <?php if ($currentPage == "v4.2") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/4.2" class="<?php echo $itemClass; ?>">v4.2</a><br></div>
        <?php if ($currentPage == "v3.3") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/3.3" class="<?php echo $itemClass; ?>">v3.3</a><br></div>
        <?php if ($currentPage == "All Javadoc") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/doc/#javadoc" class="<?php echo $itemClass; ?>">&raquo; All Javadoc</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/javadoc/5.1" class="<?php echo $itemClass; ?>">v5.1</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/javadoc/4.2" class="<?php echo $itemClass; ?>">v4.2</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/javadoc/3.3" class="<?php echo $itemClass; ?>">v3.3</a><br></div>
        <?php if ($currentPage == "All .Net APIs") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/doc#csdoc" class="<?php echo $itemClass; ?>">&raquo; All .Net APIs</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/csdoc/5.1" class="<?php echo $itemClass; ?>">v5.x</a><br></div>
        <hr/>
        <?php if ($currentPage == "Issue tracker") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/tracker/tbg" class="<?php echo $itemClass; ?>">&raquo; Issue tracker</a><br></div>
        <?php if ($currentPage == "bugs") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/1/search/1" class="<?php echo $itemClass; ?>">bugs</a><br></div>
        <?php if ($currentPage == "features") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/9/search/1" class="<?php echo $itemClass; ?>">features</a><br></div>
        <?php if ($currentPage == "enhancements") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/2/search/1" class="<?php echo $itemClass; ?>">enhancements</a><br></div>
        <?php if ($currentPage == "next version") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/8/search/1" class="<?php echo $itemClass; ?>">next version</a><br></div>
        <?php if ($currentPage == "maintenance") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/22/search/1" class="<?php echo $itemClass; ?>">maintenance</a><br></div>
        <hr/>
        <?php if ($currentPage == "Press") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/press.php" class="<?php echo $itemClass; ?>">&raquo; Press</a><br></div>
        <?php if ($currentPage == "Release notes") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/release_notes.php?version=5.1" class="<?php echo $itemClass; ?>">&raquo; Release notes</a><br></div>
        <?php if ($currentPage == "Quotes") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/quotes.php" class="<?php echo $itemClass; ?>">&raquo; Quotes</a><br></div>
        <?php if ($currentPage == "Screenshots") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/screenshots" class="<?php echo $itemClass; ?>">&raquo; Screenshots</a><br></div>
        <?php if ($currentPage == "News") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/news.php" class="<?php echo $itemClass; ?>">&raquo; News</a><br></div>
        <hr/>
        <?php if ($currentPage == "Contacts") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/contacts.php" class="<?php echo $itemClass; ?>">&raquo; Contacts</a><br></div>
        <?php if ($currentPage == "Services") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/services.php" class="<?php echo $itemClass; ?>">&raquo; Services</a><br></div>
        <br/>
				</div>
				<div class="jppf_content">
  <h1 align="center">Downloads - JPPF 5.2 beta 2 preview</h1>
  <div class="column_left" style="text-align: justify; padding: 0px">
    <div class="blockWithHighlightedTitle">
      <a name="<?php echo $ver1 ?>"></a>
      <div align="left" style="border-bottom: solid 1px #B5C0E0; padding: 0px; margin-left: -5px; margin-right: -8px">
  <h2>&nbsp;<img src="images/icons/download.png" class="titleWithIcon"/>JPPF <?php echo $ver1 ?></h2>
</div>
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
  </div>
  <div class="column_right" style="text-align: justify; padding: 0px;">
    <div class="blockWithHighlightedTitle">
      <div align="left" style="border-bottom: solid 1px #B5C0E0; padding: 0px; margin-left: -5px; margin-right: -8px">
  <h2>&nbsp;<img src="images/icons/warning.png" class="titleWithIcon"/>Please note</h2>
</div>
      <p style="font-style: italic; font-weight: bold">JPPF 5.2 beta 2 is a preview release and is not intended for deployment in production.
      <p>You are welcome to try it and provide feedback in our <a href="/forums">user forums</a>, as well as register bugs or enhancement requests in our <a href="/tracker/tbg/jppf/issues/find/saved_search/8/search/1">issue tracker</a>,
      so we have a chance to improve it before the final release.
    </div>
    <br>
    <div class="blockWithHighlightedTitle">
      <div align="left" style="border-bottom: solid 1px #B5C0E0; padding: 0px; margin-left: -5px; margin-right: -8px">
  <h3>&nbsp;<img src="images/icons/documentation.png" class="titleWithIcon"/>Documentation preview</h3>
</div>
      <p>Doc preview for the major new features:
      <ul class="samplesList">
        <li><a href="/doc/5.2">JPPF 5.2 User Guide</a></li>
        <li><a href="/javadoc/5.2">JPPF 5.2 Javadoc</a></li>
        <li><a href="/csdoc/5.2">JPPF 5.2 C-Sharp Doc</a></li>
      </ul>
    </div>
    <br>
    <div class="blockWithHighlightedTitle">
      <div align="left" style="border-bottom: solid 1px #B5C0E0; padding: 0px; margin-left: -5px; margin-right: -8px">
  <h3>&nbsp;<img src="images/icons/folder-download.png" class="titleWithIcon"/>All JPPF releases</h3>
</div>
      <br>All JPPF files can be found from <a href="http://sourceforge.net/projects/jppf-project/files/jppf-project"><b>this location</b></a>.<br/>&nbsp;
    </div>
    <br>
  </div>
</div>
				</td>
				</tr>
			</table>
			<table border="0" cellspacing="0" cellpadding="0" width="100%" class="jppffooter">
      <tr><td colspan="*" style="height: 10px"></td></tr>
      <tr>
        <td align="center" style="font-size: 9pt; color: #6D78B6">
          <a href="http://sourceforge.net/donate/index.php?group_id=135654"><img src="http://images.sourceforge.net/images/project-support.jpg" width="88" height="32" border="0" alt="Support This Project" /></a>
        </td>
        <td align="middle" valign="middle" style="font-size: 9pt; color: #6D78B6">Copyright &copy; 2005-2016 JPPF.org</td>
        <td align="middle" valign="center">
          <!-- Google+ button -->
          <!--
          <div class="g-plusone" data-href="http://www.jppf.org" data-annotation="bubble" data-size="small" data-width="300"></div>
          <script type="text/javascript">
            (function() {
              var po = document.createElement('script'); po.type = 'text/javascript'; po.async = true;
              po.src = 'https://apis.google.com/js/platform.js';
              var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(po, s);
            })();
          </script>
          -->
          <!-- Twitter share button -->
          <a href="https://twitter.com/share" class="twitter-share-button" data-url="http://www.jppf.org" data-via="jppfgrid" data-count="horizontal" data-dnt="true">Tweet</a>
          <script>!function(d,s,id){var js,fjs=d.getElementsByTagName(s)[0],p=/^http:/.test(d.location)?'http':'https';if(!d.getElementById(id)){js=d.createElement(s);js.id=id;js.src=p+'://platform.twitter.com/widgets.js';fjs.parentNode.insertBefore(js,fjs);}}(document, 'script', 'twitter-wjs');</script>
          <!-- Facebook Like button -->
          <iframe src="http://www.facebook.com/plugins/like.php?href=http%3A%2F%2Fwww.jppf.org&amp;layout=button_count&amp;show_faces=true&amp;width=40&amp;action=like&amp;colorscheme=light&amp;height=20" scrolling="no" frameborder="0"
            class="like" allowTransparency="true"></iframe>
        </td>
        <td align="right">
          <a href="http://sourceforge.net/projects/jppf-project">
            <img src="http://sflogo.sourceforge.net/sflogo.php?group_id=135654&type=10" width="80" height="15" border="0"
              alt="Get JPPF at SourceForge.net. Fast, secure and Free Open Source software downloads"/>
          </a>
        </td>
        <td style="width: 10px"></td>
      </tr>
      <tr><td colspan="*" style="height: 10px"></td></tr>
    </table>
  <!--</div>-->
  <div style="background-color: #E2E4F0; width: 100%;"><img src="/images/frame_bottom.gif" border="0"/></div>
		</div>
		</div>
	</body>
</html>
