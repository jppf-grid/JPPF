<html>
	$template{name="head-section" title="Home Page"}$

	<body>
		<div align="center">
		$template{name="jppf-header"}$
		<table border="0" cellspacing="20" cellpadding="0" width="80%">
			<tr>
				<td width="50%" valign="top" rowspan="2">
					<table class="noborder_" cellspacing="0" cellpadding="5" width="100%">
						<tr><td class="noborder_">
							<h3>Project Description</h3>
								JPPF is a grid toolkit for Java that makes it easy to run applications in parallel, and speed up their execution by orders of magnitude.
								Write once, deploy once, execute everywhere!
						</td></tr>
						<tr><td class="noborder_">
							<h4>Features</h4>
							<ul>
								<li>an easy to use <b>API</b> to submit tasks for execution in parallel</li>
								<li>a set of APIs and user interface tools to <b>administrate and monitor</b> the servers</li>
								<li><b>scalability</b> up to an arbitrary number of processing nodes</li>
								<li>the framework is <b>deployment-free</b>: no need to install your application code on a server, just connect to the server and any
								new or updated code is automatically loaded.</li>
								<li>built-in <b>failover and recovery</b> for all components of the framework (clients, servers and nodes)</li>
								<li><b>opportunistic grid</b> capabilities with <b>JPPF@Home</b> (see <a href="http://www.jppf.org/screenshots/shot11.jpg">screenshot</a>)</li>
								<li><b><a href="wiki/index.php/JPPF_And_J2EE">J2EE Integration</a></b>: JPPF grid services are available for market-leading application servers</li>
								<li>fully <b>documented</b> APIs, administration guide and developer guide</li>
								<li>runs on any platform supporting Java 2 Platform Standard Edition 5.0 (J2SE 1.5) or later</li>
							</ul>
						</td></tr>
						<tr><td class="noborder_">
							<h3>Participate, contribute and stay informed:</h3>
							<strong style="color: #8080FF">Browse our <a href="./wiki" target=_top>documentation</a></strong><br>
							<strong style="color: #8080FF">Give your comments and advice, find support, on our <a href="./forums" target=_top>discussion forums</a></strong><br>
							<strong style="color: #8080FF">Browse and update our <a href="http://sourceforge.net/tracker/?atid=733518&group_id=135654&func=browse" target=_top>bugs database</a></strong><br>
							<strong style="color: #8080FF">Browse and contribute to our <a href="http://sourceforge.net/tracker/?atid=733521&group_id=135654&func=browse" target=_top>feature requests database</a></strong>
						</td></tr>

					</table>
				</td>

				<td width="50%" valign="top">
					<table class="noborder_" cellspacing="0" cellpadding="5" width="100%">

						<tr><td class="noborder_">
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

						<tr><td class="noborder_">
							<h4>Current&nbsp;status: <span style="color: black; font-weight: normal; font-size: 10pt">Version 1.0 beta 1</span></h4>
						</td></tr>
						<tr><td class="noborder_">
							<h4>Licensing: <span style="color: black; font-weight: normal; font-size: 10pt">This project is licensed under the Apache License, Version 2.0.<br>
							A copy of the licensing terms can be obtained <a href="http://www.apache.org/licenses/LICENSE-2.0"><b>at this location</b></a>.</span></h4>
						</td></tr>
						<tr><td class="noborder_">
							<h3>JPPF on the web:</h3>
							<strong style="color: #8080FF"><a href="http://www.jroller.com/jppf/" target=_top>JPPF Blog</a></strong><br>
							<strong style="color: #8080FF"><a href="http://www.jroller.com/gkorland/entry/jppf" target=_top>Guy Korland's Blog</a></strong><br>
							<strong style="color: #8080FF"><a href="http://www.infoq.com/news/Grid-Computing-JPPF" target=_top>JPPF on InfoQ</a></strong><br>
							<strong style="color: #8080FF"><a href="http://weblogs.java.net/blog/fabriziogiudici/archive/2006/11/parallel_comput_1.html" target=_top>Fabrizio Giudici's Blog</a></strong><br>
							<strong style="color: #8080FF"><a href="http://wiki.cs.rit.edu/bin/view/Main/KarolPietrzakComputerGraphicsJPPF" target=_top> Karl Pietrzak's Parallelization of Ray Tracing</a></strong><br>
							<strong style="color: #8080FF"><a href="http://www.oitos.it/opencms/opencms/oitos/modules/products/product_0033.html" target=_top>JPPF evaluation on OITOS</a> (in Italian)</strong><br>
							
							
						</td></tr>
					</table>
				</td>
			</tr>
		</table>

		</div>
	</body>
</html>
