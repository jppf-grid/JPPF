<?php $currentPage="Home" ?>
<html>
		<head>
		<title>JPPF Home
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
				<?php if ($currentPage == "v3.x") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/doc/v3/index.php?title=JPPF_3.x_Documentation" class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;v3.x</a><br></div>
				<?php if ($currentPage == "v2.x") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/doc/v2" class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;v2.x</a><br></div>
				<?php if ($currentPage == "Javadoc") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/api-3" class="<?php echo $itemClass; ?>">&raquo; Javadoc</a><br></div>
				<?php if ($currentPage == "v3.x") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/api-3" class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;v3.x</a><br></div>
				<?php if ($currentPage == "v2.x") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/api-2.0" class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;v2.x</a><br></div>
				<!--
				<?php if ($currentPage == "Faq") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/faq" class="<?php echo $itemClass; ?>">&raquo; Faq</a><br></div>
				-->
				<?php if ($currentPage == "License") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/license.php" class="<?php echo $itemClass; ?>">&raquo; License</a><br></div>
				<hr/>
				<?php if ($currentPage == "Press") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/press.php" class="<?php echo $itemClass; ?>">&raquo; Press</a><br></div>
				<?php if ($currentPage == "Release notes") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/release_notes.php?version=3.1" class="<?php echo $itemClass; ?>">&raquo; Release notes</a><br></div>
				<?php if ($currentPage == "Quotes") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/quotes.php" class="<?php echo $itemClass; ?>">&raquo; Quotes</a><br></div>
				<?php if ($currentPage == "Screenshots") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/screenshots.php" class="<?php echo $itemClass; ?>">&raquo; Screenshots</a><br></div>
				<?php if ($currentPage == "News") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/news.php" class="<?php echo $itemClass; ?>">&raquo; News</a><br></div>
				<hr/>
				<?php if ($currentPage == "Contacts") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/contacts.php" class="<?php echo $itemClass; ?>">&raquo; Contacts</a><br></div>
				<?php if ($currentPage == "Services") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/services.php" class="<?php echo $itemClass; ?>">&raquo; Services</a><br></div>
				<br/>
				</div>
				<div class="jppf_content">
	<script src="scripts/jquery.js"></script>
	<script src="scripts/tabs.min.js"></script>
	<script src="scripts/tabs.slideshow.min.js"></script>
	<script src="scripts/jppf.js"></script>
	<!--<div style="vertical-align: middle; height: 250px; width: 750px; background-image: url('/images/test1.gif'); background-repeat: no-repeat; background-attachment: scroll">-->
	<div style="vertical-align: middle; height: 150px; width: 750px;">
		<div align="center" id="images" style="vertical-align: middle; height: 150px; width: 700px;">
			<div><img src="/images/anim/Animation_01.gif" border="0" alt="JPPF"/></div>
			<div><img src="/images/anim/Animation_02.gif" border="0" alt="JPPF"/></div>
			<div><img src="/images/anim/Animation_03.gif" border="0" alt="JPPF"/></div>
			<div><img src="/images/anim/Animation_04.gif" border="0" alt="JPPF"/></div>
			<div><img src="/images/anim/Animation_05.gif" border="0" alt="JPPF"/></div>
		</div>
	</div>
	<div id="slidetabs" align="center">
		<a href="#"></a>
		<a href="#"></a>
		<a href="#"></a>
		<a href="#"></a>
		<a href="#"></a>
	</div>
	<script>anim_main2();</script>
	<div style="margin: 15px; ">
		<br/><h2 align="center"><i>New</i>: JPPF 3.1 brings security to the Grid and more, check it out!</h2>
		<p style="text-align: center; font-size: 12pt">JPPF makes it easy to parallelize computationally intensive tasks and execute them on a Grid.
	</div>
	<div class="column1">
		<?php
			$link = mysql_connect('localhost', 'pervasiv_jppfadm', 'tri75den')
				 or die('Could not connect: ' . mysql_error());
			mysql_select_db('pervasiv_jppfweb') or die('Could not select database');
			$query = 'SELECT * FROM news ORDER BY date DESC';
			$result = mysql_query($query) or die('Query failed: ' . mysql_error());
		?>
		<h3>Latest Releases</h3>
		<?php
			for ($i=1; $i<=3; $i++)
			{
				$line = mysql_fetch_array($result, MYSQL_ASSOC);
				printf("<a href='news.php#news%d' style='font-size: 10pt'><span style='white-space: nowrap'>%s %s</span></a><br>", $i, date("n/j/Y", strtotime($line["date"])), $line["title"]);
			}
			mysql_free_result($result);
			mysql_close($link);
		?>
		<div align="left">
			<br><b>Feeds: </b>
			<a href="http://sourceforge.net/export/projnews.php?group_id=135654&limit=10&flat=1&show_summaries=1"><img src="images/feed-16x16.gif" border="0"/></a>
			<a href="http://sourceforge.net/export/projnews.php?group_id=135654&limit=10&flat=1&show_summaries=1">News</a>
			&nbsp;<a href="http://sourceforge.net/export/rss2_projnews.php?group_id=135654&rss_fulltext=1"><img src="images/feed-16x16.gif" border="0"/></a>
			<a href="http://sourceforge.net/export/rss2_projnews.php?group_id=135654&rss_fulltext=1">Releases</a>
			&nbsp;&nbsp;<a href="/news.php"><b style="color: #6D78B6">All News</b></a>
		</div>
		<p><b>Follow us on <a href="http://www.twitter.com/jppfgrid"><img src="http://twitter-badges.s3.amazonaws.com/twitter-c.png" alt="Follow JPPF on Twitter" border="0"/></a></b>
		<h3>Our users say</h3>
		<a href="quotes.php" style="text-decoration: none">... we have found the framework to be extremely powerful and easy to work with...</a>
		<p><a href="quotes.php" style="text-decoration: none">... The ability to adapt our existing technology without having to redesign or rethink entire processes is fantastic ...</a>
		<br>
		<h3>Did you know ...</h3>
		That you can turn JPPF into a full-fledged P2P Grid?
		Read about it <a href="http://www.jroller.com/jppf/entry/master_worker_or_p2p_grid"><b>here</b></a>
	</div>
	<div class="column2">
		<h3>Getting started</h3>
		Take an easy start with our <a href="/doc/v3/index.php?title=A_first_taste_of_JPPF"><b>tutorial</b></a>
		<h3>Easy and powerful</h3>
		<ul class="samplesList">
			<li>a JPPF grid can be up and running in minutes</li>
			<li>dynamically scalable on-demand</li>
			<li>ready for the Cloud, a natural medium for JPPF</li>
			<li>fully secure SSL / TLS communications</li>
			<li>connectors with leading J2EE application servers</li>
			<li>easy programming model</li>
			<li>fine-grained monitoring and administration</li>
			<li>fault-tolerance and self-repair capabilities</li>
			<li>exceptional level of service and reliability</li>
			<li>fully documented samples, using JPPF on real-life problems</li>
			<li>flexible open-source licensing with <a href="/license.php"><b>Apache License v2.0</b></a></li>
		</ul>
		<h3>Contribute</h3>
		<b>Browse our <a href="./wiki" target=_top>documentation</a></b><br>
		<b>Find support, share your ideas, in our <a href="./forums" target=_top>discussion forums</a></b><br>
		<b>Browse and contribute to our <a href="http://sourceforge.net/tracker/?atid=733518&group_id=135654&func=browse" target=_top>bugs database</a></b><br>
		<b>Browse and contribute to our <a href="http://sourceforge.net/tracker/?atid=733521&group_id=135654&func=browse" target=_top>feature requests database</a></b><br>
		<b>Gain insight and provide feedback in the <a href="http://www.jroller.com/jppf/" target=_top>JPPF blog</a></b>
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
				<td align="center" style="font-size: 9pt; color: #6D78B6">Copyright &copy; 2005-2011 JPPF.org</td>
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
