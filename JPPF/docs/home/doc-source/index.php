<html>
	$template{name="head-section" title="Home Page"}$

	<body>
		<div align="center">
		$template{name="jppf-header"}$
		<table border="0" cellspacing="10" cellpadding="0" width="80%">
   		<tr>
				<td valign="top" width="50%" style="max-width: 50%">
					<table class="noborder_" cellspacing="0" cellpadding="5" width="100%">
						<tr><td class="noborder_">
							$template{name="highlight-header" span="1" title="Grid Computing has never been easier"}$
							<br><div style="text-align: justify">
								JPPF is a grid computing framework for Java that makes it easy to run your applications in parallel, and speed up their execution by orders of magnitude.
								Write once, deploy once, execute everywhere!
							</div>
							<br>
							$template{name="highlight-bottom" span="1"}$
						</td></tr>

						<tr><td class="noborder_" height="10"/></tr>
						<tr><td class="noborder_">
							$template{name="highlight-header" span="1" title="Easy and powerful"}$
							<br><ul type="square">
								<li>A JPPF grid can be up and running in minutes</li>
								<li>Simple programming model that abstracts the complexity of distributed and parallel processing</li>
								<li>Highly scalable, distributed framework for the parallel execution of cpu-intensive tasks</li>
								<li>Seamless integration with leading J2EE application servers</li>
								<li>Graphical and programmatic tools for fine-grained monitoring and administration</li>
								<li>Fault-tolerance and self-repair capabilities ensure the highest level of service and reliability</li>
								<li>A set of fully documented sample applications of JPPF to real-life problems</li>
								<li>JPPF@Home screensaver leverages idle enterprise resources</li>
								<li>Runs on any platform that supports Java &reg;
								<li>Very flexible and business-friendly open source licensing</li>
							</ul>
							$template{name="highlight-bottom" span="1"}$
						</td></tr>

						<tr><td class="noborder_" height="10"/></tr>
						<tr><td class="noborder_">
							$template{name="highlight-header" span="1" title="Contribute and keep up to date"}$
								<br>
								<strong>Browse our <a href="./wiki" target=_top>documentation</a></strong><br>
								<strong>Find support, share your ideas, comments and advice, in our <a href="./forums" target=_top>discussion forums</a></strong><br>
								<strong>Browse and contribute to our <a href="http://sourceforge.net/tracker/?atid=733518&group_id=135654&func=browse" target=_top>bugs database</a></strong><br>
								<strong>Browse and contribute to our <a href="http://sourceforge.net/tracker/?atid=733521&group_id=135654&func=browse" target=_top>feature requests database</a></strong>
							$template{name="highlight-bottom" span="1"}$
						</td></tr>

						<tr><td class="noborder_" height="10"/></tr>
						<tr><td class="noborder_">
							$template{name="highlight-header" span="1" title="Licensing & Status"}$
							<br>JPPF is licensed under the <a href="http://www.apache.org/licenses/LICENSE-2.0"><b>Apache License, Version 2.0</b></a>
							<br>Latest stable version: Version 1.3
							$template{name="highlight-bottom" span="1"}$
						</td></tr>

					</table>
				</td>

				<td valign="top" width="50%">
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
						$template{name="highlight-header" span="1" title="Latest news"}$
						<?php
							printf("<br><span class='newsTitle' style='color: black'>%s %s</span><br>", date("n/j/Y", strtotime($line["date"])), $line["title"]);
						?>
						<?php
							printf("<br>%s", $line["desc"]);
						?>
						<p><u style="color: #8080FF"><strong style="color: #8080FF">Summary of changes:</strong></u>
						<?php
							printf("%s", $line["content"]);
							mysql_free_result($result);
							mysql_close($link);
						?>
							<table border="0" cellspacing="0" cellpadding="5">
								<tr><td colspan="*" height="10"/></tr>
								<tr>
									<td align="center" valign="center">
										<strong style="color: #8080FF">JPPF feeds:</strong>
									</td>
									<td width="10"/>
									<td align="center" valign="center">
										<a href="http://sourceforge.net/export/projnews.php?group_id=135654&limit=10&flat=1&show_summaries=1">
											<img src="images/feed-16x16.gif" border="0"/></a>&nbsp;
										<a href="http://sourceforge.net/export/projnews.php?group_id=135654&limit=10&flat=1&show_summaries=1">News</a>
									</td>
									<td width="10"/>
									<td align="center" valign="center">
										<a href="http://sourceforge.net/export/rss2_projnews.php?group_id=135654&rss_fulltext=1">
											<img src="images/feed-16x16.gif" border="0"/></a>&nbsp;
										<a href="http://sourceforge.net/export/rss2_projnews.php?group_id=135654&rss_fulltext=1">Releases</a>
									</td>
								</tr>
							</table>

							<div align="center">
								<a class="download" href="http://sourceforge.net/project/showfiles.php?group_id=135654">
								<span class="download">Download JPPF Now</span>
								</a>
							</div>
						$template{name="highlight-bottom" span="1"}$
						</td></tr>

						<tr><td class="noborder_" height="10"/></tr>
						<tr><td class="noborder_" align = "center">
							$template{name="highlight-header" span="1" title="JPPF on the web"}$
							<br><strong style="color: #8080FF"><a href="http://www.jroller.com/jppf/" target=_top>JPPF Blog</a></strong><br>
							<strong style="color: #8080FF"><a href="http://www.theserverside.com/news/thread.tss?thread_id=47941" target=_top>JPPF 1.0 on TheServerSide.com</a></strong><br>
							<strong style="color: #8080FF"><a href="http://www.infoq.com/news/Grid-Computing-JPPF" target=_top>JPPF on InfoQ</a></strong><br>
							<strong style="color: #8080FF"><a href="http://developers.slashdot.org/article.pl?sid=08/06/23/2036222" target=_top>JPPF on Slashdot</a></strong><br>
							<strong style="color: #8080FF"><a href="http://www.jroller.com/gkorland/entry/jppf" target=_top>Guy Korland's Blog</a></strong><br>
							<strong style="color: #8080FF"><a href="http://artemis.ms.mff.cuni.cz/pogamut/tiki-index.php?page=Pogamut+GRID" target=_top>Pogamut GRID</a></strong><br>
							<strong style="color: #8080FF"><a href="http://weblogs.java.net/blog/fabriziogiudici/archive/2006/11/parallel_comput_1.html" target=_top>Fabrizio Giudici's Blog</a></strong><br>
							<strong style="color: #8080FF"><a href="http://wiki.cs.rit.edu/bin/view/Main/KarolPietrzakComputerGraphicsJPPF" target=_top> Karl Pietrzak's Parallelization of Ray Tracing</a></strong><br>
							<strong style="color: #8080FF"><a href="http://www.oitos.it/opencms/opencms/oitos/modules/products/product_0033.html" target=_top>JPPF evaluation on OITOS</a> (in Italian)</strong><br>
							$template{name="highlight-bottom" span="1"}$
						</td></tr>
					</table>
				</td>
			</tr>
		</table>
		</div>
		$template{name="jppf-footer"}$
	</body>
</html>
