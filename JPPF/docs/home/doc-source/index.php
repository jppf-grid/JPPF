<html>
	$template{name="head-section" title="Home Page"}$

	<body>
		<div align="center">
		$template{name="jppf-header"}$
		<br>
		<table border="0" cellspacing="0" cellpadding="0" width="80%" style="min-width: 1024px; max-width: 1536px;
			width:expression(document.body.clientWidth < 1024 ? '1024px': (document.body.clientWidth > 1536 ? 1536 : '100%'))">
   		<tr>
				<td valign="top" width="50%" style="max-width: 50%">
					<table class="noborder_" cellspacing="0" cellpadding="5" width="100%">
						<tr><td class="noborder_">
							$template{name="highlight-header" span="1" title="Grid Computing has never been easier"}$
							<br><div style="text-align: justify">
								JPPF is an open source Grid Computing platform written in Java that makes it easy to run applications in parallel, and speed up their execution by orders of magnitude. Write once, deploy once, execute everywhere!
							</div>
							<p>Start here with the <a href="JPPFQuickStart/docs/toc.html">JPPF Quick Start Guide</a>
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
							<br>Latest stable version: Version 1.7
							$template{name="highlight-bottom" span="1"}$
						</td></tr>

					</table>
				</td>

				<td valign="top" style="width: 10px; min-width: 10px; max-width: 10px; width:expression('10px')">

				<td valign="top" width="50%">
					<table class="noborder_" cellspacing="0" cellpadding="5" width="100%">

						<tr><td class="noborder_">
						<?php
							$link = mysql_connect('localhost', 'pervasiv_jppfadm', 'tri75den')
								 or die('Could not connect: ' . mysql_error());
							mysql_select_db('pervasiv_jppfweb') or die('Could not select database');
							$query = 'SELECT * FROM news ORDER BY date DESC';
							$result = mysql_query($query) or die('Query failed: ' . mysql_error());
						?>
						$template{name="highlight-header" span="1" title="Latest news"}$
						<table border="0" cellspacing="0" cellpadding="5" align="center" width="100%">
							<tr><td align="center" colspan="2">
								<a href="http://www.jppf.org/forums/viewtopic.php?t=653"><span style="font-size: 14pt; font-weight: bold; color: blue">JPPF at SCALE 7X!</span></a><br>
								<a href="http://www.socallinuxexpo.org/"><img src="http://scale7x.socallinuxexpo.org/sites/scale7x.socallinuxexpo.org/files/scale7x-banner-468x60_3.gif.thumb.jpg" border="0"/></a>
							</td></tr>
							<tr>
								<td valign="top" width="50%">
									<h2>Latest Releases</h2>
									<?php
										for ($i=1; $i<=3; $i++)
										{
											$line = mysql_fetch_array($result, MYSQL_ASSOC);
											//printf("<br><span class='newsTitle' style='color: black'>%s %s</span><br>", date("n/j/Y", strtotime($line["date"])), $line["title"]);
											printf("<p><a href='news.php#news%d' style='font-size: 12pt'>%s %s</a>", $i, date("n/j/Y", strtotime($line["date"])), $line["title"]);
										}
									?>
									<!--
										printf("<br>%s", $line["desc"]);
										printf("%s", $line["content"]);
									-->
									<?php
										mysql_free_result($result);
										mysql_close($link);
									?>
								</td>
								<td width="50%" style="text-align: justify">
									<h2>Release highlight</h2>
									The coolest feature in JPPF 1.7 is the ability to automatically discover JPPF servers on the network, without the need to know where they run.
									This makes the deployment of JPPF components even easier, bypassing much of the configuration overhead that existed before.
									Read more on how it is done in the <a href="wiki/index.php?title=Configuring#Automatic_driver_discovery">JPPF documentation</a>.
								</td>
							</tr>
						<table>
						<table border="0" cellspacing="0" cellpadding="5" align="center">
							<tr><td colspan="*" height="10"/></tr>
							<tr>
								<td align="center" valign="center">
									<strong style="color: #8080FF">Feeds:</strong>
								</td>
								<td align="center" valign="center">
									<a href="http://sourceforge.net/export/projnews.php?group_id=135654&limit=10&flat=1&show_summaries=1">
										<img src="images/feed-16x16.gif" border="0"/></a>&nbsp;
									<a href="http://sourceforge.net/export/projnews.php?group_id=135654&limit=10&flat=1&show_summaries=1">News</a>
								</td>
								<td width="5"/>
								<td align="center" valign="center">
									<a href="http://sourceforge.net/export/rss2_projnews.php?group_id=135654&rss_fulltext=1">
										<img src="images/feed-16x16.gif" border="0"/></a>&nbsp;
									<a href="http://sourceforge.net/export/rss2_projnews.php?group_id=135654&rss_fulltext=1">Releases</a>
								</td>
								<td width="10"/>
								<td align="center" valign="center">
									<a href="/news.php"><strong style="color: #8080FF">All News</strong></a>
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
							$template{name="highlight-header" span="1" title="Our users say"}$
							<br><a href="testimonials.php" style="text-decoration: none">... we have found the framework to be extremely powerful and easy to work with...
							<p>... The ability to adapt our existing technology without having to redesign or rethink entire processes is fantastic ...</a>
							$template{name="highlight-bottom" span="1"}$
						</td></tr>

						<tr><td class="noborder_" height="10"/></tr>
						<tr><td class="noborder_" align = "center">
							$template{name="highlight-header" span="1"
								title="<table border='0' cellspacing='0' cellpadding='0'>
									<tr>
										<td valign='center'><img src='images/jppf_group_large.gif' border='1'/></td>
										<td valign='center'><span class='blockHeader'>&nbsp;on the web</span></td>
									</tr>
								</table>"
							}$
							<br><strong style="color: #8080FF"><a href="http://www.jroller.com/jppf/" target=_top>JPPF Blog</a></strong><br>
							<strong style="color: #8080FF"><a href="http://www.linkedin.com/groups?gid=757317" target=_top>JPPF Users Group on LinkedIn</a></strong><br>
							<strong style="color: #8080FF"><a href="http://www.theserverside.com/news/thread.tss?thread_id=47941" target=_top>JPPF 1.0 on TheServerSide.com</a></strong><br>
							<strong style="color: #8080FF"><a href="http://www.infoq.com/news/Grid-Computing-JPPF" target=_top>JPPF on InfoQ</a></strong><br>
							<strong style="color: #8080FF"><a href="http://developers.slashdot.org/article.pl?sid=08/06/23/2036222" target=_top>JPPF on Slashdot</a></strong><br>
							<strong style="color: #8080FF"><a href="http://www.jroller.com/gkorland/entry/jppf" target=_top>Guy Korland's Blog</a></strong><br>
							<strong style="color: #8080FF"><a href="http://jppfpov.sourceforge.net" target=_top>JppfPov: POV-Ray grid integration</a></strong><br>
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
