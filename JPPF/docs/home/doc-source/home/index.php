<?php
  require_once("db_settings.inc.php");
  $currentPage="Home";
?>
$template{name="about-page-header" title="<?php echo $currentPage ?>"}$
  <script src="scripts/jquery.js"></script>
  <script src="scripts/tabs.min.js"></script>
  <script src="scripts/tabs.slideshow.min.js"></script>
  <script src="scripts/jppf.js"></script>
  <br>
  <div class="blockWithHighlightedTitle" style="height: 163px">
  <div style="vertical-align: middle; height: 150px; width: 750px">
    <div align="center" id="images" style="vertical-align: middle; height: 150px; width: 700px">
      <div><img src="/images/anim/Animation_01.gif" border="0" alt="JPPF"/></div>
      <div><img src="/images/anim/Animation_02.gif" border="0" alt="JPPF"/></div>
      <div><img src="/images/anim/Animation_03.gif" border="0" alt="JPPF"/></div>
      <div><img src="/images/anim/Animation_04.gif" border="0" alt="JPPF"/></div>
      <div><img src="/images/anim/Animation_05.gif" border="0" alt="JPPF"/></div>
      <div><img src="/images/anim/Animation_06.gif" border="0" alt="JPPF"/></div>
    </div>
  </div>
  </div>
  <div id="slidetabs" align="center">
    <a href="#"></a>
    <a href="#"></a>
    <a href="#"></a>
    <a href="#"></a>
    <a href="#"></a>
    <a href="#"></a>
  </div>
  <script>anim_main2();</script>
  <div align="center">
    <br/>
    <h2><i>New</i>: JPPF 5.0 is here, <a href='/release_notes.php?version=5.0'>check it out!</a></h2>
    <p style="font-size: 12pt">JPPF makes it easy to parallelize computationally intensive tasks and execute them on a Grid.
  </div>

  <div class="column_left">
    <br><div class="blockWithHighlightedTitle" style="padding-left: 5px">
    $template{name="title-with-icon" img="images/icons/getting-started.png" title="Getting started" heading="h3"}$
    Take an easy start with our <a href="/doc/v5/index.php?title=A_first_taste_of_JPPF"><b>tutorial</b></a><br><br>
    </div>

    <br><div class="blockWithHighlightedTitle" style="padding-left: 5px">
    $template{name="title-with-icon" img="images/icons/easy.png" title="Easy and powerful" heading="h3"}$
    <ul class="samplesList">
      <li>a JPPF grid can be up and running in minutes</li>
      <li>dynamically scalable on-demand</li>
      <li>ready for the Cloud, a natural medium for JPPF</li>
      <li>fully secure SSL / TLS communications</li>
      <li>full volunteer computing support</li>
      <li>integration with leading J2EE application servers</li>
      <li>easy programming model</li>
      <li>fine-grained monitoring and administration</li>
      <li>fault-tolerance and self-repair capabilities</li>
      <li>exceptional level of service and reliability</li>
      <li>full, comprehensive documentation</li>
      <li>broad set of fully documented end-to-end demos</li>
      <li>flexible licensing with the <a href="/license.php"><b>Apache License v2.0</b></a></li>
    </ul>
    </div>

    <br><div class="blockWithHighlightedTitle" style="padding-left: 5px">
    $template{name="title-with-icon" img="images/icons/contribute.png" title="Contribute" heading="h3"}$
    <b>Browse our <a href="/doc" target=_top>documentation</a></b><br>
    <b>Find support, share your ideas, in our <a href="./forums" target=_top>discussion forums</a></b><br>
    <b>Browse and contribute to our <a href="/tracker/tbg/jppf/issues/find/saved_search/1/search/1" target=_top>bugs database</a></b><br>
    <b>Browse and contribute to our <a href="/tracker/tbg/jppf/issues/wishlist" target=_top>feature requests database</a></b><br>
    <b>Gain insight and provide feedback in the <a href="http://www.jroller.com/jppf/" target=_top>JPPF blog</a></b><br><br>
    </div><br>

  </div>

  <div class="column_right">
    <?php
      $link = mysql_connect($jppf_db_server, $jppf_db_user, $jppf_db_pwd) or die('Could not connect: ' . mysql_error());
      mysql_select_db($jppf_db_name) or die('Could not select database');
      $query = 'SELECT * FROM news ORDER BY date DESC';
      $result = mysql_query($query) or die('Query failed: ' . mysql_error());
    ?>
    <br><div class="blockWithHighlightedTitle" style="padding-left: 5px">
    $template{name="title-with-icon" img="images/icons/news.png" title="Latest Releases" heading="h3"}$
    <?php
      for ($i=1; $i<=3; $i++) {
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
    </div>

    <br><div align="justify" class="blockWithHighlightedTitle" style="padding-left: 5px">
    $template{name="title-with-icon" img="images/icons/personal2.png" title="Our users say" heading="h3"}$
    <a href="quotes.php" style="text-decoration: none">... JPPF turned out to be a high-performance framework, which is flexible and nevertheless easy to learn. Even the support by the community is outstanding ...</a>
    <p><a href="quotes.php" style="text-decoration: none">... we have found the framework to be extremely powerful and easy to work with...</a>
    <p><a href="quotes.php" style="text-decoration: none">... The ability to adapt our existing technology without having to redesign or rethink entire processes is fantastic ...</a>
    <br>
    </div>

    <br><div class="blockWithHighlightedTitle" style="padding-left: 5px">
    $template{name="title-with-icon" img="images/icons/help-hint.png" title="Did you know ..." heading="h3"}$
    That you can turn JPPF into a full-fledged P2P Grid?
    Read about it <a href="http://www.jroller.com/jppf/entry/master_worker_or_p2p_grid"><b>here</b></a><br><br>
    </div>

  </div>

$template{name="about-page-footer"}$
