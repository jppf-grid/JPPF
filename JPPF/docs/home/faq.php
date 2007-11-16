<html>
		<head>
		<title>Java Parallel Processing Framework FAQ Page</title>
		<meta name="description" content="An open-source, Java-based, framework for parallel computing.">
		<meta name="keywords" content="JPPF, Java, Parallel Computing, Distributed Computing, Grid Computing, Cluster, Grid">
		<meta HTTP-EQUIV="Content-Type" content="text/html; charset=UTF-8">
		<link rel="shortcut icon" href="images/jppf-icon.ico" type="image/x-icon">
		<link rel="stylesheet" type="text/css" href="./jppf.css" title="Style">
	</head>
	<body>
		<div align="center">
				<table align="center" width="70%" cellspacing="0" cellpadding="5"
			class="table_" style="background: url('images/grid.gif'); background-repeat: repeat; background-attachment: fixed">
			<tr><td height="5"></td></tr>
			<tr>
				<td width="30%" align="left" valign="center">
					<h3>Java Parallel Processing Framework</h3>
				</td>
				<td width="40%" align="center">
					<img src="images/logo.gif" border="0" alt="Java Parallel Processing Framework"/>
				</td>
				<td width="30%" align="right">
					<a href="http://sourceforge.net" target="_top">
						<img src="http://sflogo.sourceforge.net/sflogo.php?group_id=135654&amp;type=1"
							width="88" height="31" border="0" alt="SourceForge.net Logo" />
					</a>
				</td>
			</tr>
			<tr><td height="5"></td></tr>
		</table>
		<!--<table border="0" style="background-color: #8080FF" cellspacing="0" cellpadding="0" width="80%">-->
		<table style="background: url('images/bkg-menu.gif'); background-repeat: repeat; background-attachment: fixed"
			cellspacing="0" cellpadding="0" width="70%">
			<tr>
				<td>
					<table border="0" cellspacing="0" cellpadding="5">
						<tr>
							<td class="menu_first"><a href="index.php">Home</a></td>
							<td class="menu"><a href="presentation.php?current=0">Overview</a></td>
							<td class="menu"><a href="http://sourceforge.net/project/showfiles.php?group_id=135654">Download</a></td>
							<td class="menu"><a href="./wiki">Documentation</a></td>
							<td class="menu"><a href="./forums">Forums</a></td>
							<td class="menu"><a href="screenshots.html">Screenshots</a></td >
							<!--
							<td class="menu"><a href="api/index.html">API Doc</a></td >
							<td class="menu"><a href="faq.php">Faqs</a></td>
							-->
							<td class="menu"><a href="news.php">News</a></td>
							<td class="menu"><a href="http://sourceforge.net/projects/jppf-project">Project</a></td>
							<td class="menu"><a href="links.php">Links</a></td>
							<td class="menu"></td>
						</tr>
					</table>
				</td>
			</tr>
		</table>
		<table cellspacing="0" cellpadding="0" width="70%">
						<tr>
				<td width="12" class="bleft"/>
				<td colspan="${span}" bgcolor="white">
					<br>
					<h1 align="center" style="color: #8080FF"><b>Frequently Asked Questions</b></h1>
				</td>
				<td width="12" class="bright"/>
			</tr>
<?php
		$link = mysql_connect('mysql4-j', 'j135654admin', 'Faz600er')
			 or die('Could not connect: ' . mysql_error());
		mysql_select_db('j135654_web') or die('Could not select database');
		$query = 'SELECT * FROM faq_groups ORDER BY group_id ASC';
		$result = mysql_query($query) or die('Query failed: ' . mysql_error());
		$groups = array();
		while ($line = mysql_fetch_array($result, MYSQL_ASSOC))
		{
			$groups[$line["group_id"]] = $line["desc"];
		}
		mysql_free_result($result);
?>
<?php
		$count = 0;
		foreach ($groups as $key => $value)
		{
?>
						<tr>
				<td width="12" height="12" class="bleft"/>
				<td width="12" height="12" colspan="1" class="bmiddle"/>
				<td width="12" height="12" class="bright"/>
			</tr>
			<tr>
				<td width="12" class="bleft"/>
				<td bgcolor="white">
<?php
			$query = "SELECT * FROM faq_questions WHERE group_id = '$key' ORDER BY q_id ASC";
			$result = mysql_query($query) or die('Query failed: ' . mysql_error());
			printf("<h4><b><a href=\"#%s\">%s %s</a></b></h4>", $key, $key, $value);
			echo '<ul>';
			while ($line = mysql_fetch_array($result, MYSQL_ASSOC))
			{
				$ref = $key . "." . $line["q_id"];
				printf("<li><a href=\"#%s\">%s %s</a></li>", $ref, $ref, $line["desc"]);
			}
			echo '</ul>';
?>
				</td>
				<td width="12" class="bright"/>
			</tr>
<?php
			mysql_free_result($result);
			$count++;
		}
?>
						<tr>
				<td width="12" height="12" style="background-image: url(images/roundCorner_SW.gif); background-repeat: no-repeat; background-position: 0% 0%"/>
				<td width="12" height="12" colspan="1" class="bbottom"/>
				<td width="12" height="12" style="background-image: url(images/roundCorner_SE.gif); background-repeat: no-repeat; background-position: 100% 0%"/>
			</tr>
		</table>
		<br>
<?php
		$count = 0;
		foreach ($groups as $key => $value)
		{
?>
		<table cellspacing="0" cellpadding="0" width="70%">
<?php
?>
						<tr>
				<td width="12" height="12" style="background-image: url(images/roundCorner_NW.gif); background-repeat: no-repeat; background-position: 1000% 1000%"/>
				<td width="12" height="12" colspan="1" class="btop"/>
				<td width="12" height="12" style="background-image: url(images/roundCorner_NE.gif); background-repeat: no-repeat; background-position: 0% 100%"/>
			</tr>
			<tr>
				<td width="12" class="bleft"/>
				<td bgcolor="white">
<?php
					printf("<h3><b><u><a name=\"%s\">%s %s</a></u></b></h3>", $key, $key, $value);
?>
				</td>
				<td width="12" class="bright"/>
			</tr>
<?php
			$query = "SELECT * FROM faq_questions WHERE group_id = '$key' ORDER BY q_id ASC";
			$result = mysql_query($query) or die('Query failed: ' . mysql_error());
			//echo '<ul>';
			while ($line = mysql_fetch_array($result, MYSQL_ASSOC))
			{
?>
						<tr>
				<td width="12" height="12" class="bleft"/>
				<td colspan="1" bgcolor="white"/>
				<td width="12" height="12" class="bright"/>
			</tr>
						<tr>
				<td width="12" height="12" class="bleft"/>
				<td width="12" height="12" colspan="1" class="bmiddle"/>
				<td width="12" height="12" class="bright"/>
			</tr>
						<tr>
				<td width="12" height="12" class="bleft"/>
				<td colspan="1" bgcolor="white"/>
				<td width="12" height="12" class="bright"/>
			</tr>
			<tr>
				<td width="12" class="bleft"/>
				<td bgcolor="white">
<?php
				$ref = $key . "." . $line["q_id"];
?>
				<div align="center">
					<table width="100%" cellspacing="0" cellpadding="0" border="0"><tr><td>
<?php
						printf("<a name=\"%s\" style=\"color: #8080FF\"><b>%s %s</b></a>", $ref, $ref, $line["desc"]);
?>
						<div align="center">
						<table width="100%" cellspacing="0" cellpadding="0" border="0"><tr><td>
<?php
							printf("<br>%s", $line["content"]);
?>
						</td></tr></table>
					</td></tr></table>
				</div>
				</td>
				<td width="12" class="bright"/>
			</tr>
<?php
			}
			//echo '</ul>';
?>
						<tr>
				<td width="12" height="12" style="background-image: url(images/roundCorner_SW.gif); background-repeat: no-repeat; background-position: 0% 0%"/>
				<td width="12" height="12" colspan="1" class="bbottom"/>
				<td width="12" height="12" style="background-image: url(images/roundCorner_SE.gif); background-repeat: no-repeat; background-position: 100% 0%"/>
			</tr>
		</table>
		<br>
<?php
		}
		// Closing connection
		mysql_close($link);
?>
		</div>
	</body>
</html>
