<?php $currentPage="Screenshots" ?>
<html>
		<head>
		<title>JPPF Screenshots
</title>
		<meta name="description" content="An open-source, Java-based, framework for parallel computing.">
		<meta name="keywords" content="JPPF, java, parallel computing, distributed computing, grid computing, parallel, distributed, cluster, grid, gloud, open source">
		<meta HTTP-EQUIV="Content-Type" content="text/html; charset=UTF-8">
		<link rel="shortcut icon" href="images/jppf-icon.ico" type="image/x-icon">
		<link rel="stylesheet" type="text/css" href="/jppf.css" title="Style">
	</head>
	<body>
		<div align="center">
		<div class="gwrapper" align="center">
			<?php
		if (!isset($currentPage))
		{
			$currentPage = $_REQUEST["page"];
			if (($currentPage == NULL) || ($currentPage == ""))
			{
				$currentPage = "Home";
			}
		}
		if ($currentPage != "Forums")
		{
		?>
		<div style="background-color: #E2E4F0; margin: 0px;height: 10px"><img src="/images/frame_top.gif"/></div>
		<?php
		}
		?>
		<table width="100%" cellspacing="0" cellpadding="0" border="0" class="jppfheader" style="border-left: 1px solid #6D78B6; border-right: 1px solid #6D78B6">
			<tr style="height: 80px">
				<td width="20"></td>
				<td width="400" align="left" valign="center"><a href="/"><img src="/images/logo2.gif" border="0" alt="JPPF"/></a></td>
				<td align="right">
					<table border="0" cellspacing="0" cellpadding="0" style="height: 30px; background-color:transparent;">
						<tr>
							<td style="background: transparent url('/images/buttons/<?php if ($currentPage == "Home") echo "btn_start.gif"; else echo "btn_active_start.gif"; ?>') repeat-x scroll left bottom; width: 9px"></td>
							<td style="width: 1px"></td>
							<?php
if ($currentPage == "Home")
{
?>
<td class="headerMenuItem2">&nbsp;<a href="/index.php" class="headerMenuItem2">Home</a>&nbsp;</td>
<?php
}
else
{
?>
<td class="headerMenuItem">&nbsp;<a href="/index.php" class="headerMenuItem">Home</a>&nbsp;</td>
<?php
}
?>
								<td style="width: 1px"></td>
							<?php
if ($currentPage == "About")
{
?>
<td class="headerMenuItem2">&nbsp;<a href="/about.php" class="headerMenuItem2">About</a>&nbsp;</td>
<?php
}
else
{
?>
<td class="headerMenuItem">&nbsp;<a href="/about.php" class="headerMenuItem">About</a>&nbsp;</td>
<?php
}
?>
								<td style="width: 1px"></td>
							<?php
if ($currentPage == "Download")
{
?>
<td class="headerMenuItem2">&nbsp;<a href="/downloads.php" class="headerMenuItem2">Download</a>&nbsp;</td>
<?php
}
else
{
?>
<td class="headerMenuItem">&nbsp;<a href="/downloads.php" class="headerMenuItem">Download</a>&nbsp;</td>
<?php
}
?>
								<td style="width: 1px"></td>
							<?php
if ($currentPage == "Documentation")
{
?>
<td class="headerMenuItem2">&nbsp;<a href="/doc/v3/index.php?title=JPPF_3.x_Documentation" class="headerMenuItem2">Documentation</a>&nbsp;</td>
<?php
}
else
{
?>
<td class="headerMenuItem">&nbsp;<a href="/doc/v3/index.php?title=JPPF_3.x_Documentation" class="headerMenuItem">Documentation</a>&nbsp;</td>
<?php
}
?>
								<td style="width: 1px"></td>
							<?php
if ($currentPage == "Forums")
{
?>
<td class="headerMenuItem2">&nbsp;<a href="/forums" class="headerMenuItem2">Forums</a>&nbsp;</td>
<?php
}
else
{
?>
<td class="headerMenuItem">&nbsp;<a href="/forums" class="headerMenuItem">Forums</a>&nbsp;</td>
<?php
}
?>
								<td style="width: 1px"></td>
							<td style="background: transparent url('/images/buttons/<?php if ($currentPage == "Forums") echo "btn_end.gif"; else echo "btn_active_end.gif"; ?>') repeat-x scroll right bottom; width: 9px"></td>
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
				<hr/>
				<?php if ($currentPage == "Documentation") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/doc/v3/index.php?title=JPPF_3.x_Documentation" class="<?php echo $itemClass; ?>">&raquo; Documentation</a><br></div>
				<!--
				<?php if ($currentPage == "v4.x") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/v4" class="<?php echo $itemClass; ?>">v4.x</a><br></div>
				-->
				<?php if ($currentPage == "v3.x") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/v3/index.php?title=JPPF_3.x_Documentation" class="<?php echo $itemClass; ?>">v3.x</a><br></div>
				<?php if ($currentPage == "v2.x") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/v2" class="<?php echo $itemClass; ?>">v2.x</a><br></div>
				<?php if ($currentPage == "Javadoc") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/api-3" class="<?php echo $itemClass; ?>">&raquo; Javadoc</a><br></div>
				<!--
				<?php if ($currentPage == "v4.x") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/api" class="<?php echo $itemClass; ?>">v4.x</a><br></div>
				-->
				<?php if ($currentPage == "v3.x") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/api-3" class="<?php echo $itemClass; ?>">v3.x</a><br></div>
				<?php if ($currentPage == "v2.x") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/api-2.0" class="<?php echo $itemClass; ?>">v2.x</a><br></div>
				<?php if ($currentPage == "License") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/license.php" class="<?php echo $itemClass; ?>">&raquo; License</a><br></div>
				<hr/>
				<?php if ($currentPage == "Issue tracker") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/tracker/tbg" class="<?php echo $itemClass; ?>">&raquo; Issue tracker</a><br></div>
				<?php if ($currentPage == "bugs") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/1/search/1" class="<?php echo $itemClass; ?>">bugs</a><br></div>
				<?php if ($currentPage == "features") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/9/search/1" class="<?php echo $itemClass; ?>">features</a><br></div>
				<?php if ($currentPage == "enhancements") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/2/search/1" class="<?php echo $itemClass; ?>">enhancements</a><br></div>
				<?php if ($currentPage == "current work") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/8/search/1" class="<?php echo $itemClass; ?>">current work</a><br></div>
				<hr/>
				<?php if ($currentPage == "Press") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/press.php" class="<?php echo $itemClass; ?>">&raquo; Press</a><br></div>
				<?php if ($currentPage == "Release notes") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/release_notes.php?version=3.3" class="<?php echo $itemClass; ?>">&raquo; Release notes</a><br></div>
				<?php if ($currentPage == "Quotes") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/quotes.php" class="<?php echo $itemClass; ?>">&raquo; Quotes</a><br></div>
				<?php if ($currentPage == "Screenshots") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/screenshots.php?screenshot=&shotTitle=" class="<?php echo $itemClass; ?>">&raquo; Screenshots</a><br></div>
				<?php if ($currentPage == "News") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/news.php" class="<?php echo $itemClass; ?>">&raquo; News</a><br></div>
				<hr/>
				<?php if ($currentPage == "Contacts") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/contacts.php" class="<?php echo $itemClass; ?>">&raquo; Contacts</a><br></div>
				<?php if ($currentPage == "Services") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/services.php" class="<?php echo $itemClass; ?>">&raquo; Services</a><br></div>
				<br/>
				</div>
				<div class="jppf_content">
<?php
	$screenshot = $_REQUEST["screenshot"];
	if (($screenshot == NULL) || ($screenshot == ""))
	{
		$screenshot = 'AlertThresholds.gif
';
	}
	$shot_title = $_REQUEST["shotTitle"];
	if (($shot_title == NULL) || ($shot_title == ""))
	{
		//$shot_title = "Screenshot";
		$shot_title = 'Alert Thresholds
';
	}
?>
	<div align="center">
		<table border="0" cellspacing="0" cellpadding="0" width="80%">
			<tr>
				<td>
										<table align="center" border="0" cellspacing="0" cellpadding="5">
						<tr>
							<td align="center">
								<a href="screenshots.php?screenshot=AlertThresholds.gif&shotTitle=Alert Thresholds">
									<img src="screenshots/_th_AlertThresholds.jpg" border="0" alt="screenshot"/>
								</a>
							</td>
							<td align="center">
								<a href="screenshots.php?screenshot=Charts-01.gif&shotTitle=Charts 01">
									<img src="screenshots/_th_Charts-01.jpg" border="0" alt="screenshot"/>
								</a>
							</td>
							<td align="center">
								<a href="screenshots.php?screenshot=Charts-02.gif&shotTitle=Charts 02">
									<img src="screenshots/_th_Charts-02.jpg" border="0" alt="screenshot"/>
								</a>
							</td>
							<td align="center">
								<a href="screenshots.php?screenshot=Charts-03.gif&shotTitle=Charts 03">
									<img src="screenshots/_th_Charts-03.jpg" border="0" alt="screenshot"/>
								</a>
							</td>
							<td align="center">
								<a href="screenshots.php?screenshot=ConnectionsStatistics.gif&shotTitle=Connections Statistics">
									<img src="screenshots/_th_ConnectionsStatistics.jpg" border="0" alt="screenshot"/>
								</a>
							</td>
							<td align="center">
								<a href="screenshots.php?screenshot=DeadlockMonitoring.gif&shotTitle=Deadlock Monitoring">
									<img src="screenshots/_th_DeadlockMonitoring.jpg" border="0" alt="screenshot"/>
								</a>
							</td>
							<td align="center">
								<a href="screenshots.php?screenshot=Docking-1.gif&shotTitle=Docking 1">
									<img src="screenshots/_th_Docking-1.jpg" border="0" alt="screenshot"/>
								</a>
							</td>
							<td align="center">
								<a href="screenshots.php?screenshot=Docking-2.gif&shotTitle=Docking 2">
									<img src="screenshots/_th_Docking-2.jpg" border="0" alt="screenshot"/>
								</a>
							</td>
							<td align="center">
								<a href="screenshots.php?screenshot=Docking-3.gif&shotTitle=Docking 3">
									<img src="screenshots/_th_Docking-3.jpg" border="0" alt="screenshot"/>
								</a>
							</td>
						</tr>
						<tr>
							<td align="center">
								<a href="screenshots.php?screenshot=DriverSystemInformation.gif&shotTitle=Driver System Information">
									<img src="screenshots/_th_DriverSystemInformation.jpg" border="0" alt="screenshot"/>
								</a>
							</td>
							<td align="center">
								<a href="screenshots.php?screenshot=JobPriority.gif&shotTitle=Job Priority">
									<img src="screenshots/_th_JobPriority.jpg" border="0" alt="screenshot"/>
								</a>
							</td>
							<td align="center">
								<a href="screenshots.php?screenshot=JobStatistics.gif&shotTitle=Job Statistics">
									<img src="screenshots/_th_JobStatistics.jpg" border="0" alt="screenshot"/>
								</a>
							</td>
							<td align="center">
								<a href="screenshots.php?screenshot=LoadBalancerSettings-01.gif&shotTitle=Load Balancer Settings 01">
									<img src="screenshots/_th_LoadBalancerSettings-01.jpg" border="0" alt="screenshot"/>
								</a>
							</td>
							<td align="center">
								<a href="screenshots.php?screenshot=MandelbrotFractals.jpg&shotTitle=Mandelbrot Fractals">
									<img src="screenshots/_th_MandelbrotFractals.jpg" border="0" alt="screenshot"/>
								</a>
							</td>
							<td align="center">
								<a href="screenshots.php?screenshot=ManyJobs-01.gif&shotTitle=Many Jobs 01">
									<img src="screenshots/_th_ManyJobs-01.jpg" border="0" alt="screenshot"/>
								</a>
							</td>
							<td align="center">
								<a href="screenshots.php?screenshot=ManyJobs-02.gif&shotTitle=Many Jobs 02">
									<img src="screenshots/_th_ManyJobs-02.jpg" border="0" alt="screenshot"/>
								</a>
							</td>
							<td align="center">
								<a href="screenshots.php?screenshot=ManyJobs-03.gif&shotTitle=Many Jobs 03">
									<img src="screenshots/_th_ManyJobs-03.jpg" border="0" alt="screenshot"/>
								</a>
							</td>
							<td align="center">
								<a href="screenshots.php?screenshot=MaxtrixSample-01.gif&shotTitle=Maxtrix Sample 01">
									<img src="screenshots/_th_MaxtrixSample-01.jpg" border="0" alt="screenshot"/>
								</a>
							</td>
						</tr>
						<tr>
							<td align="center">
								<a href="screenshots.php?screenshot=NodeSystemInformation.gif&shotTitle=Node System Information">
									<img src="screenshots/_th_NodeSystemInformation.jpg" border="0" alt="screenshot"/>
								</a>
							</td>
							<td align="center">
								<a href="screenshots.php?screenshot=NodeThreads-01.gif&shotTitle=Node Threads 01">
									<img src="screenshots/_th_NodeThreads-01.jpg" border="0" alt="screenshot"/>
								</a>
							</td>
							<td align="center">
								<a href="screenshots.php?screenshot=NodeTrayAddon-01.gif&shotTitle=Node Tray Addon 01">
									<img src="screenshots/_th_NodeTrayAddon-01.jpg" border="0" alt="screenshot"/>
								</a>
							</td>
							<td align="center">
								<a href="screenshots.php?screenshot=NodeTrayAddon-02.gif&shotTitle=Node Tray Addon 02">
									<img src="screenshots/_th_NodeTrayAddon-02.jpg" border="0" alt="screenshot"/>
								</a>
							</td>
							<td align="center">
								<a href="screenshots.php?screenshot=NodeTrayAddon-03.gif&shotTitle=Node Tray Addon 03">
									<img src="screenshots/_th_NodeTrayAddon-03.jpg" border="0" alt="screenshot"/>
								</a>
							</td>
							<td align="center">
								<a href="screenshots.php?screenshot=ProteinSequenceAlignment.gif&shotTitle=Protein Sequence Alignment">
									<img src="screenshots/_th_ProteinSequenceAlignment.jpg" border="0" alt="screenshot"/>
								</a>
							</td>
							<td align="center">
								<a href="screenshots.php?screenshot=RuntimeMonitoring.gif&shotTitle=Runtime Monitoring">
									<img src="screenshots/_th_RuntimeMonitoring.jpg" border="0" alt="screenshot"/>
								</a>
							</td>
							<td align="center">
								<a href="screenshots.php?screenshot=ServerStats-01.gif&shotTitle=Server Stats 01">
									<img src="screenshots/_th_ServerStats-01.jpg" border="0" alt="screenshot"/>
								</a>
							</td>
							<td align="center">
								<a href="screenshots.php?screenshot=Topology-01.gif&shotTitle=Topology 01">
									<img src="screenshots/_th_Topology-01.jpg" border="0" alt="screenshot"/>
								</a>
							</td>
						</tr>
						<tr>
							<td align="center">
								<a href="screenshots.php?screenshot=Topology-GraphView-02.gif&shotTitle=Topology  Graph View 02">
									<img src="screenshots/_th_Topology-GraphView-02.jpg" border="0" alt="screenshot"/>
								</a>
							</td>
							<td align="center">
								<a href="screenshots.php?screenshot=Topology-GraphView.gif&shotTitle=Topology  Graph View">
									<img src="screenshots/_th_Topology-GraphView.jpg" border="0" alt="screenshot"/>
								</a>
							</td>
							<td align="center">
								<a href="screenshots.php?screenshot=UpdateNodeConfiguration.gif&shotTitle=Update Node Configuration">
									<img src="screenshots/_th_UpdateNodeConfiguration.jpg" border="0" alt="screenshot"/>
								</a>
							</td>
						</tr>
					</table>
				</td>
			</tr>
		</table>
		<br>
				<div>
					<h3 align="center"><?php echo $shot_title; ?></h3>
				<div  style="margin: 1px">
				<br>
				<img src="screenshots/<?php echo $screenshot; ?>" border="0" alt="screenshot"/>
				</div>
				</div>
		<table border="0" cellspacing="0" cellpadding="0">
			<tr><td align="center">
			</td></tr>
		</table>
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
				<td align="center" style="font-size: 9pt; color: #6D78B6">Copyright &copy; 2005-2013 JPPF.org</td>
				<td align="right">
					<a href="http://www.parallel-matters.com"><img src="/images/pm_logo_tiny.jpg" border="0" alt="Powered by Parallel Matters" /></a>&nbsp;
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
