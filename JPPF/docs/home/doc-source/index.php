<html>
	$template{name="head-section" title="Home Page"}$

	<body>
		<div align="center">
		$template{name="jppf-header"}$
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
