<?php $currentPage="News" ?>
$template{name="about-page-header" title="News"}$
	<?php
	// Connecting, selecting database
	$link = mysql_connect('localhost', 'lolocohe_jppfadm', 'tri75den')
		 or die('Could not connect: ' . mysql_error());
	mysql_select_db('lolocohe_jppfweb') or die('Could not select database');

	// Performing SQL query
	$query = 'SELECT * FROM news ORDER BY date DESC';
	$result = mysql_query($query) or die('Query failed: ' . mysql_error());
	$i = 0;
	while ($line = mysql_fetch_array($result, MYSQL_ASSOC))
	{
	?>
		<?php
		if ($i > 0)
		{
		?>
		<br>
		<hr width="100%" align="center" style="color: #6D78B6">
		<?php
		}
		$i++;
		$title = date("n/j/Y", strtotime($line["date"])) . " " . $line["title"];
		?>
		<br><a name="news<?php echo $i ?>"></a>
		<h1><?php echo $title ?></h1>
		<br><?php echo $line["desc"] ?>
		<?php echo $line["content"]	?>
		<br>
	<?php
	}
	// Free resultset
	mysql_free_result($result);
	// Closing connection
	mysql_close($link);
	?>
$template{name="about-page-footer"}$
