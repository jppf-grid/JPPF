<html>
	$template{name="head-section" title="Links Page"}$

	<body>
		<div align="center">
		$template{name="jppf-header"}$
		<h1 align="center" style="color: blue">Related Links</h1>
		<table cellspacing="0" cellpadding="0" width="80%">

<?php
		$link = mysql_connect('mysql4-j', 'j135654admin', 'Faz600er')
			 or die('Could not connect: ' . mysql_error());
		mysql_select_db('j135654_web') or die('Could not select database');

		$query = 'SELECT * FROM links_groups ORDER BY group_id ASC';
		$result = mysql_query($query) or die('Query failed: ' . mysql_error());
		$groups = array();
		while ($line = mysql_fetch_array($result, MYSQL_ASSOC))
		{
			$groups[$line["group_id"]] = $line["desc"];
		}
		mysql_free_result($result);
?>

<?php
		$count = 0;
		foreach ($groups as $key => $value)
		{
?>
			<tr><td colspan="*">
			<br>$template{name="highlight-header" span="1" title="<?php printf('%s', $value); ?>"}$
			<br>
<?php
			$query = "SELECT * FROM links WHERE group_id = '$key' ORDER BY link_id ASC";
			$result = mysql_query($query) or die('Query failed: ' . mysql_error());
			while ($line = mysql_fetch_array($result, MYSQL_ASSOC))
			{
?>
				<ul>
<?php
				$ref = $key . "." . $line["q_id"];
				printf("<li><span class=\"linksub\"><a href=\"%s\">%s</a>:</span> %s</li>", $line["url"], $line["title"], $line["desc"]);
?>
				</ul>
<?php
			}
?>
			$template{name="highlight-bottom" span="1" color="yellow"}$
			</td></tr>
<?php
		}
		// Closing connection
		mysql_close($link);
?>
		</table>
	</div>
	$template{name="jppf-footer"}$
	</body>
</html>
