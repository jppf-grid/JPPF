<?php $currentPage="Patches" ?>
$template{name="about-page-header" title="Patches"}$
<?php
  $patch_id = $_REQUEST["patch_id"];
  // Connecting, selecting database
  $link = mysql_connect('127.0.0.1', 'lolocohe_jppfadm', 'tri75den')
     or die('Could not connect: ' . mysql_error());
  mysql_select_db('lolocohe_jppfweb') or die('Could not select database');

  // Performing SQL query
  $query = "SELECT * FROM patch where id = " . $patch_id;
  $result = mysql_query($query) or die('Query failed: ' . mysql_error());
  $line = mysql_fetch_array($result, MYSQL_ASSOC);
  $jppf_ver = $line["jppf_version"];
  $patch_number = $line["patch_number"];
  $patch_url = $line["patch_url"];
  $readme = preg_replace('@\n@', '<br/>', $line['readme']);
  $readme = preg_replace('@(^ )+@', '&nbsp;', $readme);
?>
  <br><div class="blockWithHighlightedTitle">
  <h1>JPPF <?php echo $jppf_ver ?> patch <?php echo $patch_number ?></h1>
  <div style="border-bottom: solid 1px #6D78B6; height: 10px; margin-left: -5px; margin-right: -8px"></div>

<?php
  $downloadLink = "<a href='/private/patch/" . $patch_url . "'>" . $patch_url . "</a>";
?>
  $template{name="title-with-icon" img="images/icons/download.png" title="Download: <?php echo $downloadLink ?>" heading="h3"}$

  <div style="border-bottom: solid 1px #6D78B6; height: 10px; margin-left: -5px; margin-right: -8px"></div>
  $template{name="title-with-icon" img="images/icons/view-list.png" title="Description (included readme.txt):" heading="h3"}$
  <?php echo preg_replace('/\n/', '<br/>', $line['readme']) ?>

  <div style="border-bottom: solid 1px #6D78B6; height: 10px; margin-left: -5px; margin-right: -8px"></div>
  $template{name="title-with-icon" img="images/icons/bug.png" title="Fixed bugs:" heading="h3"}$
  <ul class="samplesList">
<?php
  mysql_free_result($result);
  $query = "SELECT * FROM patch_bugs where jppf_version = '" . $jppf_ver . "' AND patch_number = '" . $patch_number . "'";
  $result = mysql_query($query) or die('Query failed: ' . mysql_error());
  while ($line = mysql_fetch_array($result, MYSQL_ASSOC)) {
?>
    <li><a href="<?php echo $line['bug_url'] ?>"><?php echo $line['bug_id'] ?>&nbsp;<?php echo $line['bug_title'] ?></a></li>
<?php
  }
?>
  </ul>
  </div><br>
<?php
  // Free resultset
  mysql_free_result($result);
  // Closing connection
  mysql_close($link);
?>
$template{name="about-page-footer"}$
