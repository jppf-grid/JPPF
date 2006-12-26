<html>
		<head>
		<title>Java Parallel Processing Framework Home Page</title>
		<meta name="description" content="An open-source, Java-based, framework for parallel computing.">
		<meta name="keywords" content="JPPF, Java, Parallel Computing, Distributed Computing, Grid Computing, Cluster, Grid">
		<meta HTTP-EQUIV="Content-Type" content="text/html; charset=UTF-8">
		<link rel="shortcut icon" href="images/jppf-icon.ico" type="image/x-icon">
		<link rel="stylesheet" type="text/css" href="./jppf.css" title="Style">
	</head>
	<body>
		<div align="center">
				<table align="center" width="80%" cellspacing="0" cellpadding="5"
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
						<img src="http://sourceforge.net/sflogo.php?group_id=135654&amp;type=4"
							width="125" height="37" border="0" alt="SourceForge.net Logo" />
					</a>
				</td>
			</tr>
			<tr><td height="5"></td></tr>
		</table>
		<!--<table border="0" style="background-color: #8080FF" cellspacing="0" cellpadding="0" width="80%">-->
		<table style="background: url('images/bkg-menu.gif'); background-repeat: repeat; background-attachment: fixed"
			cellspacing="0" cellpadding="0" width="80%">
			<tr>
				<td>
					<table border="0" cellspacing="0" cellpadding="5">
						<tr>
							<td class="menu_first"><a href="index.html">Home</a></td>
							<!--<td class="menu"><a href="JPPF-Overview.html">Overview</a></td>-->
							<td class="menu"><a href="presentation.php?current=0">Overview</a></td>
							<td class="menu"><a href="http://sourceforge.net/project/showfiles.php?group_id=135654">Files</a></td>
							<td class="menu"><a href="./wiki">Wiki &amp; Doc</a></td>
							<td class="menu"><a href="./forums">Forums</a></td>
							<td class="menu"><a href="screenshots.html">Screenshots</a></td >
							<td class="menu"><a href="api/index.html">API Doc</a></td >
							<td class="menu"><a href="faq.php">Faqs</a></td>
							<td class="menu"><a href="news.php">News</a></td>
							<td class="menu"><a href="http://sourceforge.net/projects/jppf-project">Project</a></td>
							<td class="menu"><a href="links.php">Links</a></td>
							<td class="menu"></td>
						</tr>
					</table>
				</td>
			</tr>
		</table>
		<table border="0" cellspacing="0" cellpadding="0" width="80%">
			<tr>
				<td width="50%" valign="top" rowspan="2">
					<table class="left_" cellspacing="0" cellpadding="5" width="100%">
						<tr><td class="leftTopBottom_">
							<h3>Project Description</h3>
							Java Parallel Processing Framework is a computational grid framework for Java, focused on performance and ease of use.
							It provides a set of extensible and customizable tools and APIs to facilitate the parallelization of CPU intensive applications, and distribute their execution over a network of heterogenous nodes.
						</td></tr>
						<tr><td class="bottom_">
							<h4>Features</h4>
							<ul>
								<li>an easy to use <b>API</b> to submit tasks for execution in parallel</li>
								<li>a set of APIs and user interface tools to <b>administrate and monitor</b> the servers</li>
								<li><b>scalability</b> up to an arbitrary number of processing nodes</li>
								<li>the framework is <b>deployment-free</b>: no need to install your application code on a server, just connect to the server and any
								new or updated code is automatically loaded.</li>
								<li>built-in <b>failover and recovery</b> for all components of the framework (clients, servers and nodes)</li>
								<li><b>opportunistic grid</b> capabilities with <b>JPPF@Home</b> (see <a href="http://www.jppf.org/screenshots/shot11.jpg">screenshot</a>)</li>
								<li>fully <b>documented</b> APIs, administration guide and developer guide</li>
								<li>runs on any platform supporting Java 2 Platform Standard Edition 5.0 (J2SE 1.5) or later</li>
							</ul>
						</td></tr>
						<tr><td class="bottom_">
							<h4>Current&nbsp;status: <span style="color: black; font-weight: normal; font-size: 10pt">Version 0.24.0 - beta</span></h4>
						</td></tr>
						<tr><td class="bottom_">
							<h3>New: JPPF is looking for developers!</h3>
							If you're interested, please take a look at the <a href="http://sourceforge.net/people/viewjob.php?group_id=135654&job_id=25512">
							announcement</a> posted on <b>SF.net</b><br><br>
						</td></tr>
						<tr><td class="bottom_">
							<h4>Licensing: <span style="color: black; font-weight: normal; font-size: 10pt">This project is licensed under the GNU Lesser General Public License (LGPL).
							A copy of the licensing terms can be obtained <a href="http://www.opensource.org/licenses/lgpl-license.php"><b>here</b></a>.</span></h4>
						</td></tr>
					</table>
				</td>
				<td width="50%" valign="top">
					<table class="leftRight_" cellspacing="0" cellpadding="5" width="100%">
						<tr><td class="bottom_">
						<?php
							$link = mysql_connect('mysql4-j', 'j135654admin', 'Faz600er')
								 or die('Could not connect: ' . mysql_error());
							mysql_select_db('j135654_web') or die('Could not select database');
							$query = 'SELECT * FROM news ORDER BY date DESC';
							$result = mysql_query($query) or die('Query failed: ' . mysql_error());
							$line = mysql_fetch_array($result, MYSQL_ASSOC);
							printf("<h3>Latest news: <span style='color: black'>%s %s</span></h3>", date("n/j/Y", strtotime($line["date"])), $line["title"]);
							printf("%s", $line["desc"]);
						?>
						<p><u style="color: #8080FF"><strong style="color: #8080FF">Summary of changes:</strong></u>
						<?php
							printf("%s", $line["content"]);
							mysql_free_result($result);
							mysql_close($link);
						?>
						</td></tr>
						<tr><td class="bottom_">
							<h3>JPPF articles on the Web</h3>
							<table align="center">
								<tr><td>
									<p><a href="http://www.infoq.com/news/Grid-Computing-JPPF" target=_top>
										<img src="http://www.infoq.com/styles/i/logo.gif" border="0" alternate="InfoQ"/>
									</a>
									</td>
									<td>
									<p><a href="http://lwn.net/Articles/156109" target=_top>
										<img src="http://lwn.net/images/lcorner.png" border="0" alternate="LWN.net"/>
									</a>
								</td></tr>
								<tr><td>
									<p><a href="http://www.artima.com/forums/flat.jsp?forum=276&thread=153331" target=_top>
										<img src="http://www.artima.com/images/ab_dev.gif" border="0" alternate="artima developer"/>
									</a>
								</td></tr>
								<tr><td>
									<p><a href="http://linux.softpedia.com/get/System/Clustering-and-Distributed-Networks/Java-Parallel-Processing-Framework-10529.shtml" target=_top>
										<img src="http://www.softpedia.com/base_img/softpedia_logo.gif" border="0" alternate="Softpedia"/>
									</a>
								</td></tr>
							</table>
						</td></tr>
						<tr><td class="bottom_">
							<h3>Feedback Wanted: <span style="color: #000060">help making JPPF a better open source product</span></h3>
							Suggestions, bug reports, criticism and ideas are most welcome. We will do our best to answer promptly.<br>
							<a href="./forums" target=_top>Discussion forums are available here</a><br>
							<a href="http://sourceforge.net/tracker/?atid=733518&group_id=135654&func=browse" target=_top>The bugs tracking system is here</a><br>
							<a href="http://sourceforge.net/tracker/?atid=733521&group_id=135654&func=browse" target=_top>The feature request tracking system is here</a><br>
						</td></tr>
					</table>
				</td>
			</tr>
		</table>
		</div>
	</body>
</html>
