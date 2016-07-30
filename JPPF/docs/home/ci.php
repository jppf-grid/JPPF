<?php $currentPage="continuous integration" ?>
<html lang="en" xml:lang="en" xmlns="http://www.w3.org/1999/xhtml">
	  <head>
    <title>JPPF JPPF continuous integration
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
        <?php if ($currentPage == "v5.2 (beta)") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/5.2" class="<?php echo $itemClass; ?>">v5.2 (beta)</a><br></div>
        <?php if ($currentPage == "v5.1") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/5.1" class="<?php echo $itemClass; ?>">v5.1</a><br></div>
        <?php if ($currentPage == "v4.2") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/4.2" class="<?php echo $itemClass; ?>">v4.2</a><br></div>
        <?php if ($currentPage == "All Javadoc") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/doc/#javadoc" class="<?php echo $itemClass; ?>">&raquo; All Javadoc</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/javadoc/5.2" class="<?php echo $itemClass; ?>">v5.2 (beta)</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/javadoc/5.1" class="<?php echo $itemClass; ?>">v5.1</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/javadoc/5.0" class="<?php echo $itemClass; ?>">v5.0</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/javadoc/4.2" class="<?php echo $itemClass; ?>">v4.2</a><br></div>
        <?php if ($currentPage == "All .Net APIs") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/doc#csdoc" class="<?php echo $itemClass; ?>">&raquo; All .Net APIs</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/csdoc/5.2" class="<?php echo $itemClass; ?>">v5.2 (beta)</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/csdoc/5.1" class="<?php echo $itemClass; ?>">v5.1</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/csdoc/5.0" class="<?php echo $itemClass; ?>">v5.0</a><br></div>
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
  <h1 align="center">JPPF continuous integration</h1>
  <div class="column_left" style="text-align: justify">
    <div class="blockWithHighlightedTitle" align='center'>
  <table><tr><td align='left'>
  <h2><img src='images/icons/monitoring.png' class='titleWithIcon'/>JPPF trunk</h2>
    <table cellpadding='3px' cellspacing='0'>
      <tr>
        <th align='center' valign='top' style='border: 1px solid #6D78B6; border-right: 0px'>Build #</th>
        <th align='center' valign='top' style='border: 1px solid #6D78B6; border-right: 0px'>Start</th>
        <th align='center' valign='top' style='border: 1px solid #6D78B6; border-right: 0px'>Duration</th>
        <th align='center' style='border: 1px solid #6D78B6;'>Tests</th>
      </tr>
      <tr>
        <td align='left' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'><img width='16' height='16' src='http://www.jppf.org/images/icons/bug1.png'/> 203</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>2016-07-13 18:10:00</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>00:00:05.727</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px;'>N/A</td>
      </tr>
      <tr>
        <td align='left' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'><img width='16' height='16' src='http://www.jppf.org/images/icons/default.png'/> 202</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>2016-07-13 16:10:00</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>00:14:06.934</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px;'> 309 /    0 /    3</td>
      </tr>
      <tr>
        <td align='left' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'><img width='16' height='16' src='http://www.jppf.org/images/icons/default.png'/> 201</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>2016-07-13 14:10:00</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>00:14:36.544</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px;'> 309 /    0 /    3</td>
      </tr>
      <tr>
        <td align='left' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'><img width='16' height='16' src='http://www.jppf.org/images/icons/default.png'/> 200</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>2016-07-13 12:10:00</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>00:19:02.439</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px;'> 309 /    0 /    3</td>
      </tr>
      <tr>
        <td align='left' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'><img width='16' height='16' src='http://www.jppf.org/images/icons/default.png'/> 199</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>2016-07-13 10:10:00</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>00:13:24.700</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px;'> 309 /    0 /    3</td>
      </tr>
      <tr>
        <td align='left' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'><img width='16' height='16' src='http://www.jppf.org/images/icons/default.png'/> 198</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>2016-07-13 08:10:00</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>00:12:37.039</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px;'> 309 /    0 /    3</td>
      </tr>
      <tr>
        <td align='left' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'><img width='16' height='16' src='http://www.jppf.org/images/icons/default.png'/> 197</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>2016-07-13 06:10:00</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>00:12:24.807</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px;'> 309 /    0 /    3</td>
      </tr>
      <tr>
        <td align='left' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'><img width='16' height='16' src='http://www.jppf.org/images/icons/bug1.png'/> 196</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>2016-07-13 04:10:00</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>00:00:01.774</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px;'>N/A</td>
      </tr>
      <tr>
        <td align='left' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'><img width='16' height='16' src='http://www.jppf.org/images/icons/bug1.png'/> 195</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>2016-07-13 02:10:00</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>00:00:00.868</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px;'>N/A</td>
      </tr>
    </table>
  </td></tr></table><br></div><br>

<div class="blockWithHighlightedTitle" align='center'>
  <table><tr><td align='left'>
  <h2><img src='images/icons/monitoring.png' class='titleWithIcon'/>JPPF 5.1</h2>
    <table cellpadding='3px' cellspacing='0'>
      <tr>
        <th align='center' valign='top' style='border: 1px solid #6D78B6; border-right: 0px'>Build #</th>
        <th align='center' valign='top' style='border: 1px solid #6D78B6; border-right: 0px'>Start</th>
        <th align='center' valign='top' style='border: 1px solid #6D78B6; border-right: 0px'>Duration</th>
        <th align='center' style='border: 1px solid #6D78B6;'>Tests</th>
      </tr>
      <tr>
        <td align='left' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'><img width='16' height='16' src='http://www.jppf.org/images/icons/default.png'/> 203</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>2016-07-13 12:10:00</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>00:19:02.439</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px;'> 309 /    0 /    3</td>
      </tr>
      <tr>
        <td align='left' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'><img width='16' height='16' src='http://www.jppf.org/images/icons/default.png'/> 202</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>2016-07-13 16:10:00</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>00:14:06.934</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px;'> 309 /    0 /    3</td>
      </tr>
      <tr>
        <td align='left' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'><img width='16' height='16' src='http://www.jppf.org/images/icons/default.png'/> 201</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>2016-07-13 14:10:00</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>00:14:36.544</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px;'> 309 /    0 /    3</td>
      </tr>
      <tr>
        <td align='left' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'><img width='16' height='16' src='http://www.jppf.org/images/icons/bug1.png'/> 200</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>2016-07-13 18:10:00</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>00:00:05.727</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px;'>N/A</td>
      </tr>
      <tr>
        <td align='left' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'><img width='16' height='16' src='http://www.jppf.org/images/icons/default.png'/> 199</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>2016-07-13 10:10:00</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>00:13:24.700</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px;'> 309 /    0 /    3</td>
      </tr>
      <tr>
        <td align='left' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'><img width='16' height='16' src='http://www.jppf.org/images/icons/default.png'/> 198</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>2016-07-13 08:10:00</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>00:12:37.039</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px;'> 309 /    0 /    3</td>
      </tr>
      <tr>
        <td align='left' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'><img width='16' height='16' src='http://www.jppf.org/images/icons/default.png'/> 197</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>2016-07-13 06:10:00</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>00:12:24.807</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px;'> 309 /    0 /    3</td>
      </tr>
      <tr>
        <td align='left' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'><img width='16' height='16' src='http://www.jppf.org/images/icons/bug1.png'/> 196</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>2016-07-13 04:10:00</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>00:00:01.774</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px;'>N/A</td>
      </tr>
      <tr>
        <td align='left' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'><img width='16' height='16' src='http://www.jppf.org/images/icons/bug1.png'/> 195</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>2016-07-13 02:10:00</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>00:00:00.868</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px;'>N/A</td>
      </tr>
    </table>
  </td></tr></table><br></div><br>


    <br>
  </div>
  <div class="column_right" style="text-align: justify">
    <div class="blockWithHighlightedTitle" align='center'>
  <table><tr><td align='left'>
  <h2><img src='images/icons/monitoring.png' class='titleWithIcon'/>JPPF 5.2</h2>
    <table cellpadding='3px' cellspacing='0'>
      <tr>
        <th align='center' valign='top' style='border: 1px solid #6D78B6; border-right: 0px'>Build #</th>
        <th align='center' valign='top' style='border: 1px solid #6D78B6; border-right: 0px'>Start</th>
        <th align='center' valign='top' style='border: 1px solid #6D78B6; border-right: 0px'>Duration</th>
        <th align='center' style='border: 1px solid #6D78B6;'>Tests</th>
      </tr>
      <tr>
        <td align='left' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'><img width='16' height='16' src='http://www.jppf.org/images/icons/default.png'/> 203</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>2016-07-13 12:10:00</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>00:19:02.439</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px;'> 309 /    0 /    3</td>
      </tr>
      <tr>
        <td align='left' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'><img width='16' height='16' src='http://www.jppf.org/images/icons/default.png'/> 202</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>2016-07-13 16:10:00</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>00:14:06.934</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px;'> 309 /    0 /    3</td>
      </tr>
      <tr>
        <td align='left' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'><img width='16' height='16' src='http://www.jppf.org/images/icons/default.png'/> 201</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>2016-07-13 14:10:00</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>00:14:36.544</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px;'> 309 /    0 /    3</td>
      </tr>
      <tr>
        <td align='left' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'><img width='16' height='16' src='http://www.jppf.org/images/icons/bug1.png'/> 200</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>2016-07-13 18:10:00</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>00:00:05.727</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px;'>N/A</td>
      </tr>
      <tr>
        <td align='left' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'><img width='16' height='16' src='http://www.jppf.org/images/icons/default.png'/> 199</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>2016-07-13 10:10:00</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>00:13:24.700</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px;'> 309 /    0 /    3</td>
      </tr>
      <tr>
        <td align='left' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'><img width='16' height='16' src='http://www.jppf.org/images/icons/default.png'/> 198</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>2016-07-13 08:10:00</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>00:12:37.039</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px;'> 309 /    0 /    3</td>
      </tr>
      <tr>
        <td align='left' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'><img width='16' height='16' src='http://www.jppf.org/images/icons/default.png'/> 197</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>2016-07-13 06:10:00</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>00:12:24.807</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px;'> 309 /    0 /    3</td>
      </tr>
      <tr>
        <td align='left' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'><img width='16' height='16' src='http://www.jppf.org/images/icons/bug1.png'/> 196</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>2016-07-13 04:10:00</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>00:00:01.774</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px;'>N/A</td>
      </tr>
      <tr>
        <td align='left' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'><img width='16' height='16' src='http://www.jppf.org/images/icons/bug1.png'/> 195</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>2016-07-13 02:10:00</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px; border-right: 0px;'>00:00:00.868</td>
        <td align='right' valign='bottom' style='border: 1px solid #6D78B6; border-top: 0px;'>N/A</td>
      </tr>
    </table>
  </td></tr></table><br></div><br>


    <br>
  </div>
  <br>
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
