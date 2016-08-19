<?php $currentPage="Screenshots" ?>
<html>
	  <head>
    <title>JPPF Screenshots
</title>
    <meta name="description" content="The open source grid computing solution">
    <meta name="keywords" content="JPPF, java, parallel computing, distributed computing, grid computing, parallel, distributed, cluster, grid, cloud, open source">
    <meta HTTP-EQUIV="Content-Type" content="text/html; charset=UTF-8">
    <link rel="shortcut icon" href="images/jppf-icon.ico" type="image/x-icon">
    <link rel="stylesheet" type="text/css" href="/jppf.css" title="Style">
  </head>
	<body>
		<div align="center">
		<div class="gwrapper" align="center">
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
        <td width="20"></td>
        <td width="191" align="left" valign="center"><a href="/"><img src="/images/logo2.gif" border="0" alt="JPPF" style="box-shadow: inset -3px -3px 5px #6D78B6"/></a></td>
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
<td class="<?php echo $cl; ?>">&nbsp;<a href="/doc/v5" class="<?php echo $cl; ?>">Documentation</a>&nbsp;</td>
<td style="width: 1px"></td>
              <?php $cl = (($currentPage == "Forums") ? "headerMenuItem2" : "headerMenuItem") . " " . "header_item_end"; ?>
<td class="<?php echo $cl; ?>">&nbsp;<a href="/forums" class="<?php echo $cl; ?>">Forums</a>&nbsp;</td>
<td style="width: 1px"></td>
            </tr>
          </table>
        </td>
        <td width="20"></td>
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
                <?php if ($currentPage == "Documentation") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/doc/v5" class="<?php echo $itemClass; ?>">&raquo; Documentation</a><br></div>
        <?php if ($currentPage == "v5.x") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/v5" class="<?php echo $itemClass; ?>">v5.x</a><br></div>
        <?php if ($currentPage == "v4.x") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/v4" class="<?php echo $itemClass; ?>">v4.x</a><br></div>
        <?php if ($currentPage == "v3.x") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/v3" class="<?php echo $itemClass; ?>">v3.x</a><br></div>
        <?php if ($currentPage == "Javadoc") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/api-5" class="<?php echo $itemClass; ?>">&raquo; Javadoc</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/api-5" class="<?php echo $itemClass; ?>">v5.x</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/api" class="<?php echo $itemClass; ?>">v4.x</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/api-3" class="<?php echo $itemClass; ?>">v3.x</a><br></div>
        <?php if ($currentPage == ".Net API") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/csdoc-5" class="<?php echo $itemClass; ?>">&raquo; .Net API</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/csdoc-5" class="<?php echo $itemClass; ?>">v5.x</a><br></div>
        <hr/>
        <?php if ($currentPage == "Issue tracker") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/tracker/tbg" class="<?php echo $itemClass; ?>">&raquo; Issue tracker</a><br></div>
        <?php if ($currentPage == "bugs") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/1/search/1" class="<?php echo $itemClass; ?>">bugs</a><br></div>
        <?php if ($currentPage == "features") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/9/search/1" class="<?php echo $itemClass; ?>">features</a><br></div>
        <?php if ($currentPage == "enhancements") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/2/search/1" class="<?php echo $itemClass; ?>">enhancements</a><br></div>
        <?php if ($currentPage == "next version") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/8/search/1" class="<?php echo $itemClass; ?>">next version</a><br></div>
        <?php if ($currentPage == "maintenance") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/22/search/1" class="<?php echo $itemClass; ?>">maintenance</a><br></div>
        <hr/>
        <?php if ($currentPage == "Press") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/press.php" class="<?php echo $itemClass; ?>">&raquo; Press</a><br></div>
        <?php if ($currentPage == "Release notes") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/release_notes.php?version=5.0" class="<?php echo $itemClass; ?>">&raquo; Release notes</a><br></div>
        <?php if ($currentPage == "Quotes") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/quotes.php" class="<?php echo $itemClass; ?>">&raquo; Quotes</a><br></div>
        <?php if ($currentPage == "Screenshots") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/screenshots.php?screenshot=&shotTitle=&picnum=0&height=" class="<?php echo $itemClass; ?>">&raquo; Screenshots</a><br></div>
        <?php if ($currentPage == "News") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/news.php" class="<?php echo $itemClass; ?>">&raquo; News</a><br></div>
        <hr/>
        <?php if ($currentPage == "Contacts") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/contacts.php" class="<?php echo $itemClass; ?>">&raquo; Contacts</a><br></div>
        <?php if ($currentPage == "Services") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/services.php" class="<?php echo $itemClass; ?>">&raquo; Services</a><br></div>
        <br/>
				</div>
				<div class="jppf_content">
<?php
	$screenshot = $_REQUEST["screenshot"];
	if (($screenshot == NULL) || ($screenshot == "")) {
		$screenshot = 'AlertThresholds.gif
';
	}
	$shot_title = $_REQUEST["shotTitle"];
	if (($shot_title == NULL) || ($shot_title == "")) {
		//$shot_title = "Screenshot";
		$shot_title = 'Alert Thresholds
';
	}
?>
<div align="center">
  <br>
  <div class="blockWithHighlightedTitle" style="padding: 5px; max-height: 140px">
    					<table align="center" border="0" cellspacing="0" cellpadding="4">
						<tr>
							<td align="center">
  <a name="pic_0"></a>
  <a href="screenshots.php?screenshot=AlertThresholds.gif&shotTitle=Alert Thresholds&height=684&picnum=0" target="_parent">
    <img src="screenshots/_th_AlertThresholds.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_1"></a>
  <a href="screenshots.php?screenshot=Charts-01.gif&shotTitle=Charts 01&height=866&picnum=1" target="_parent">
    <img src="screenshots/_th_Charts-01.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_2"></a>
  <a href="screenshots.php?screenshot=Charts-02.gif&shotTitle=Charts 02&height=866&picnum=2" target="_parent">
    <img src="screenshots/_th_Charts-02.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_3"></a>
  <a href="screenshots.php?screenshot=Charts-03.gif&shotTitle=Charts 03&height=866&picnum=3" target="_parent">
    <img src="screenshots/_th_Charts-03.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_4"></a>
  <a href="screenshots.php?screenshot=Charts-04.gif&shotTitle=Charts 04&height=866&picnum=4" target="_parent">
    <img src="screenshots/_th_Charts-04.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_5"></a>
  <a href="screenshots.php?screenshot=ChartsConfiguration-01.gif&shotTitle=Charts Configuration 01&height=866&picnum=5" target="_parent">
    <img src="screenshots/_th_ChartsConfiguration-01.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_6"></a>
  <a href="screenshots.php?screenshot=DeadlockMonitoring.gif&shotTitle=Deadlock Monitoring&height=973&picnum=6" target="_parent">
    <img src="screenshots/_th_DeadlockMonitoring.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_7"></a>
  <a href="screenshots.php?screenshot=Docking-1.gif&shotTitle=Docking 1&height=332&picnum=7" target="_parent">
    <img src="screenshots/_th_Docking-1.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_8"></a>
  <a href="screenshots.php?screenshot=Docking-2.gif&shotTitle=Docking 2&height=316&picnum=8" target="_parent">
    <img src="screenshots/_th_Docking-2.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_9"></a>
  <a href="screenshots.php?screenshot=Docking-3.gif&shotTitle=Docking 3&height=513&picnum=9" target="_parent">
    <img src="screenshots/_th_Docking-3.jpg" border="0" alt="screenshot"/>
  </a>
</td>
						</tr>
						<tr>
							<td align="center">
  <a name="pic_10"></a>
  <a href="screenshots.php?screenshot=DriverSystemInformation.gif&shotTitle=Driver System Information&height=951&picnum=10" target="_parent">
    <img src="screenshots/_th_DriverSystemInformation.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_11"></a>
  <a href="screenshots.php?screenshot=EmbeddedConsole.gif&shotTitle=Embedded Console&height=759&picnum=11" target="_parent">
    <img src="screenshots/_th_EmbeddedConsole.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_12"></a>
  <a href="screenshots.php?screenshot=ExportConsoleSettings.gif&shotTitle=Export Console Settings&height=715&picnum=12" target="_parent">
    <img src="screenshots/_th_ExportConsoleSettings.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_13"></a>
  <a href="screenshots.php?screenshot=FractalsSample-2.gif&shotTitle=Fractals Sample 2&height=763&picnum=13" target="_parent">
    <img src="screenshots/_th_FractalsSample-2.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_14"></a>
  <a href="screenshots.php?screenshot=FractalsSample.gif&shotTitle=Fractals Sample&height=756&picnum=14" target="_parent">
    <img src="screenshots/_th_FractalsSample.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_15"></a>
  <a href="screenshots.php?screenshot=GraphView.gif&shotTitle=Graph View&height=920&picnum=15" target="_parent">
    <img src="screenshots/_th_GraphView.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_16"></a>
  <a href="screenshots.php?screenshot=JobPriority.gif&shotTitle=Job Priority&height=710&picnum=16" target="_parent">
    <img src="screenshots/_th_JobPriority.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_17"></a>
  <a href="screenshots.php?screenshot=LoadBalancerSettings.gif&shotTitle=Load Balancer Settings&height=541&picnum=17" target="_parent">
    <img src="screenshots/_th_LoadBalancerSettings.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_18"></a>
  <a href="screenshots.php?screenshot=MandelbrotNode.gif&shotTitle=Mandelbrot Node&height=860&picnum=18" target="_parent">
    <img src="screenshots/_th_MandelbrotNode.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_19"></a>
  <a href="screenshots.php?screenshot=ManyJobs-01.gif&shotTitle=Many Jobs 01&height=945&picnum=19" target="_parent">
    <img src="screenshots/_th_ManyJobs-01.jpg" border="0" alt="screenshot"/>
  </a>
</td>
						</tr>
						<tr>
							<td align="center">
  <a name="pic_20"></a>
  <a href="screenshots.php?screenshot=ManyJobs-02.gif&shotTitle=Many Jobs 02&height=945&picnum=20" target="_parent">
    <img src="screenshots/_th_ManyJobs-02.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_21"></a>
  <a href="screenshots.php?screenshot=ManyJobs-03.gif&shotTitle=Many Jobs 03&height=945&picnum=21" target="_parent">
    <img src="screenshots/_th_ManyJobs-03.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_22"></a>
  <a href="screenshots.php?screenshot=NodeProvisioning.gif&shotTitle=Node Provisioning&height=717&picnum=22" target="_parent">
    <img src="screenshots/_th_NodeProvisioning.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_23"></a>
  <a href="screenshots.php?screenshot=NodeSystemInformation.gif&shotTitle=Node System Information&height=950&picnum=23" target="_parent">
    <img src="screenshots/_th_NodeSystemInformation.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_24"></a>
  <a href="screenshots.php?screenshot=NodeThreads-01.gif&shotTitle=Node Threads 01&height=730&picnum=24" target="_parent">
    <img src="screenshots/_th_NodeThreads-01.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_25"></a>
  <a href="screenshots.php?screenshot=NodeTrayAddon-01.gif&shotTitle=Node Tray Addon 01&height=158&picnum=25" target="_parent">
    <img src="screenshots/_th_NodeTrayAddon-01.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_26"></a>
  <a href="screenshots.php?screenshot=NodeTrayAddon-02.gif&shotTitle=Node Tray Addon 02&height=161&picnum=26" target="_parent">
    <img src="screenshots/_th_NodeTrayAddon-02.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_27"></a>
  <a href="screenshots.php?screenshot=NodeTrayAddon-03.gif&shotTitle=Node Tray Addon 03&height=192&picnum=27" target="_parent">
    <img src="screenshots/_th_NodeTrayAddon-03.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_28"></a>
  <a href="screenshots.php?screenshot=PluggableAndHiddenViews-01.gif&shotTitle=Pluggable And Hidden Views 01&height=798&picnum=28" target="_parent">
    <img src="screenshots/_th_PluggableAndHiddenViews-01.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_29"></a>
  <a href="screenshots.php?screenshot=PluggableAndHiddenViews-02.gif&shotTitle=Pluggable And Hidden Views 02&height=798&picnum=29" target="_parent">
    <img src="screenshots/_th_PluggableAndHiddenViews-02.jpg" border="0" alt="screenshot"/>
  </a>
</td>
						</tr>
						<tr>
							<td align="center">
  <a name="pic_30"></a>
  <a href="screenshots.php?screenshot=PluggableView.gif&shotTitle=Pluggable View&height=798&picnum=30" target="_parent">
    <img src="screenshots/_th_PluggableView.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_31"></a>
  <a href="screenshots.php?screenshot=ProteinSequenceAlignment.gif&shotTitle=Protein Sequence Alignment&height=652&picnum=31" target="_parent">
    <img src="screenshots/_th_ProteinSequenceAlignment.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_32"></a>
  <a href="screenshots.php?screenshot=RuntimeMonitoring.gif&shotTitle=Runtime Monitoring&height=491&picnum=32" target="_parent">
    <img src="screenshots/_th_RuntimeMonitoring.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_33"></a>
  <a href="screenshots.php?screenshot=ServerStats-01.gif&shotTitle=Server Stats 01&height=943&picnum=33" target="_parent">
    <img src="screenshots/_th_ServerStats-01.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_34"></a>
  <a href="screenshots.php?screenshot=ServerStats-02.gif&shotTitle=Server Stats 02&height=651&picnum=34" target="_parent">
    <img src="screenshots/_th_ServerStats-02.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_35"></a>
  <a href="screenshots.php?screenshot=Topologies.gif&shotTitle=Topologies&height=535&picnum=35" target="_parent">
    <img src="screenshots/_th_Topologies.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_36"></a>
  <a href="screenshots.php?screenshot=Topology-Health.gif&shotTitle=Topology  Health&height=963&picnum=36" target="_parent">
    <img src="screenshots/_th_Topology-Health.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_37"></a>
  <a href="screenshots.php?screenshot=Topology-TreeView.gif&shotTitle=Topology  Tree View&height=893&picnum=37" target="_parent">
    <img src="screenshots/_th_Topology-TreeView.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_38"></a>
  <a href="screenshots.php?screenshot=UpdateNodeConfiguration.gif&shotTitle=Update Node Configuration&height=709&picnum=38" target="_parent">
    <img src="screenshots/_th_UpdateNodeConfiguration.jpg" border="0" alt="screenshot"/>
  </a>
</td>
							<td align="center">
  <a name="pic_39"></a>
  <a href="screenshots.php?screenshot=VisibleColumns.gif&shotTitle=Visible Columns&height=771&picnum=39" target="_parent">
    <img src="screenshots/_th_VisibleColumns.jpg" border="0" alt="screenshot"/>
  </a>
</td>
						</tr>
					</table>
  </div>
  <br>
  <div class="blockWithHighlightedTitle" style="padding-left: 5px; padding-right: 5px; padding-bottom: 10px;">
    <div>
					<h3 align="center"><?php echo $shot_title; ?></h3>
    <div  style="margin: 1px">
      <img src="screenshots/<?php echo $screenshot; ?>" border="0" alt="screenshot"/>
    </div>
    </div>
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
        <td align="center" style="font-size: 9pt; color: #6D78B6">Copyright &copy; 2005-2015 JPPF.org</td>
        <td align="center" valign="center">
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
