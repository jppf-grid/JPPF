<?php $currentPage="Home" ?>
$template{name="about-page-header" title="Home"}$
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
		<br/><h2 align="center"><i>New</i>: JPPF 3.2 is here, <a href="/release_notes.php?version=3.2">check it out!</a></h2>
		<p style="text-align: center; font-size: 12pt">JPPF makes it easy to parallelize computationally intensive tasks and execute them on a Grid.
	</div>

	<div class="column1">
		<?php
			$link = mysql_connect('localhost', 'lolocohe_jppfadm', 'tri75den')
				 or die('Could not connect: ' . mysql_error());
			mysql_select_db('lolocohe_jppfweb') or die('Could not select database');
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
			<li>full, comprehensive documentation</li>
			<li>fully documented samples, using JPPF on real-life problems</li>
			<li>flexible open-source licensing with <a href="/license.php"><b>Apache License v2.0</b></a></li>
		</ul>

		<h3>Contribute</h3>
		<b>Browse our <a href="./wiki" target=_top>documentation</a></b><br>
		<b>Find support, share your ideas, in our <a href="./forums" target=_top>discussion forums</a></b><br>
		<b>Browse and contribute to our <a href="/tracker/tbg/jppf/issues/find/saved_search/1/search/1" target=_top>bugs database</a></b><br>
		<b>Browse and contribute to our <a href="/tracker/tbg/jppf/issues/wishlist" target=_top>feature requests database</a></b><br>
		<b>Gain insight and provide feedback in the <a href="http://www.jroller.com/jppf/" target=_top>JPPF blog</a></b>

	</div>

$template{name="about-page-footer"}$
