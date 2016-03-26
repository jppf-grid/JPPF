<?php
  require_once("db_settings.inc.php");
  $currentPage="News";
?>
$template{name="about-page-header" title="News"}$
	<?php
  $link = mysql_connect($jppf_db_server, $jppf_db_user, $jppf_db_pwd) or die('Could not connect: ' . mysql_error());
  mysql_select_db($jppf_db_name) or die('Could not select database');

	// Performing SQL query
	$query = 'SELECT * FROM news ORDER BY date DESC';
	$result = mysql_query($query) or die('Query failed: ' . mysql_error());
	$i = 0;
	while ($line = mysql_fetch_array($result, MYSQL_ASSOC)) {
	?>
		<?php
		if ($i == 0) {
		?>
		<br>
		<?php
		}
		$i++;
		$title = date("n/j/Y", strtotime($line["date"])) . " " . $line["title"];
		?>
    <div class="blockWithHighlightedTitle">
      <a name="news<?php echo $i ?>"></a>
      <h1><?php echo $title ?></h1>
      <?php echo $line["desc"] ?>
      <?php echo $line["content"]	?>
      <br>
    </div>
		<br>
	<?php
	}
	// Free resultset
	mysql_free_result($result);
	// Closing connection
	mysql_close($link);
	?>
$template{name="about-page-footer"}$
