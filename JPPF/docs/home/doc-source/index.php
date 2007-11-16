<html>
	$template{name="head-section" title="Home Page"}$

	<body>
		<script type="text/javascript">
			function setImage(id, image)
			{
				document.getElementById(id).src=image;
			}
		</script>
		<div align="center">
		$template{name="jppf-header"}$
		<table border="0" cellspacing="0" cellpadding="0" width="70%">
			$template{name="row-blank" span="3"}$
   		<tr>
				<td width="12" class="bleft"/>
				<td valign="top" bgcolor="white" width="50%" style="max-width: 50%">
					<table class="noborder_" cellspacing="0" cellpadding="5" width="100%">
						<tr><td class="noborder_">
							$template{name="highlight-top" span="1" color="pblue"}$
							<h3>About JPPF</h3>
							<div style="text-align: justify">JPPF is a grid toolkit for Java that makes it easy to run your applications in parallel, and speed up their execution by orders of magnitude.
							Write once, deploy once, execute everywhere!</div>
							$template{name="highlight-bottom" span="1" color="pblue"}$
						</td></tr>
						<tr><td class="noborder_">
							<h4>Features</h4>
							<ul type="square">
								<li>a JPPF grid can be up and running in minutes</li>
								<li>full platform independance
								<li>highly scalable, distributed framework for the execution of Java tasks</li>
								<li>leverages JCA 1.5 to integrate with leading J2EE application servers</li>
								<li>easy programming model that abstracts the complexity of distributed and parallel processing</li>
								<li>graphical and programmatic tools for fine-grained monitoring and administration</li>
								<li>reliability through redundancy, recovery and failover capabilities</li>
								<li>a set of fully documented sample applications, applying JPPF to real-life problems</li>
								<li>a JPPF screensaver node enables the automatic use of idle computers</li>
								<li>very flexible and non-constraining open-source licensing</li>
							</ul>
						</td></tr>
						<tr><td align="center" class="noborder_">
							<a href="http://sourceforge.net/project/showfiles.php?group_id=135654"
								onmouseover="setImage('dl_jppf', 'images/downloadJPPF2.jpg')"
								onmouseout="setImage('dl_jppf', 'images/downloadJPPF.jpg')">
								<img id="dl_jppf" src="images/downloadJPPF.jpg" border="0" alt="Download JPPF Now"/></a><br>&nbsp;
						</td></tr>
						<tr><td class="noborder_">
							<h4>Current&nbsp;status: <span style="color: black; font-weight: normal; font-size: 10pt">Version 1.0 release candidate 1</span></h4>
						</td></tr>
						<tr><td class="noborder_">
							$template{name="highlight-top" span="1" color="pblue"}$
								<h3>Participate and stay informed:</h3>
								<strong style="color: #8080FF">Browse our <a href="./wiki" target=_top>documentation</a></strong><br>
								<strong style="color: #8080FF">Give your comments and advice, find support, on our <a href="./forums" target=_top>discussion forums</a></strong><br>
								<strong style="color: #8080FF">Browse and update our <a href="http://sourceforge.net/tracker/?atid=733518&group_id=135654&func=browse" target=_top>bugs database</a></strong><br>
								<strong style="color: #8080FF">Browse and contribute to our <a href="http://sourceforge.net/tracker/?atid=733521&group_id=135654&func=browse" target=_top>feature requests database</a></strong>
							$template{name="highlight-bottom" span="1" color="pblue"}$

						</td></tr>
					</table>
				</td>
				<td width="10" bgcolor="white">
				<td valign="top" bgcolor="white">
					<table class="noborder_" cellspacing="0" cellpadding="5" width="100%">

						<tr><td class="noborder_">
						<?php
							$link = mysql_connect('mysql4-j', 'j135654admin', 'Faz600er')
								 or die('Could not connect: ' . mysql_error());
							mysql_select_db('j135654_web') or die('Could not select database');
							$query = 'SELECT * FROM news ORDER BY date DESC';
							$result = mysql_query($query) or die('Query failed: ' . mysql_error());
							$line = mysql_fetch_array($result, MYSQL_ASSOC);
						?>
						$template{name="highlight-top" span="1" color="pblue"}$
						<?php
							printf("<span class='newsTitle'>Latest news: <span style='color: black'>%s %s</span></span><br>", date("n/j/Y", strtotime($line["date"])), $line["title"]);
							//printf("<h3>Latest news: <span style='color: black'>%s %s</span></h3>", date("n/j/Y", strtotime($line["date"])), $line["title"]);
						?>
						$template{name="highlight-bottom" span="1" color="pblue"}$
						<?php
							printf("<br>%s", $line["desc"]);
						?>
						<p><u style="color: #8080FF"><strong style="color: #8080FF">Summary of changes:</strong></u>
						<?php
							printf("%s", $line["content"]);
							mysql_free_result($result);
							mysql_close($link);
						?>
						</td></tr>
						<tr><td class="noborder_">
							$template{name="highlight-top" span="1" color="pblue"}$
							<table border="0" cellspacing="0" cellpadding="5">
								<tr>
									<td align="center" valign="center">
										<strong style="color: #8080FF">JPPF feeds:</strong>
									</td>
									<td align="center" valign="center">
										<a href="http://sourceforge.net/export/projnews.php?group_id=135654&limit=10&flat=1&show_summaries=1">
											<img src="images/feed-16x16.gif" border="0"/>News</a>
									</td>
									<td align="center" valign="center">
										<a href="http://sourceforge.net/export/rss2_projnews.php?group_id=135654&rss_fulltext=1">
											<img src="images/feed-16x16.gif" border="0"/>Releases</a>
									</td>
								</tr>
							</table>
							$template{name="highlight-bottom" span="1" color="pblue"}$
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
				<td width="12" class="bright"/>
			</tr>
			$template{name="row-bottom" span="3"}$
		</table>

		</div>
	</body>
</html>
