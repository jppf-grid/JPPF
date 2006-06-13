<html>
	$template{name="head-section" title="Links Page"}$

	<body>
		<div align="center">
		$template{name="jppf-header"}$
		<table cellspacing="0" cellpadding="5" width="80%">
			$template{name="page-title" title="Related Links"}$
		</table>

<?php
		$link = mysql_connect('mysql4-j', 'j135654admin', 'tri75den')
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

		<table class="leftRightBottom_" cellspacing="0" cellpadding="5" width="80%">
<?php
		$count = 0;
		foreach ($groups as $key => $value)
		{
?>
			<tr>
				<td class="top_">
<?php
					printf("<h3><b><u>%s</u></b></h3>", $value);
?>
				</td>
			</tr>
<?php
			$query = "SELECT * FROM links WHERE group_id = '$key' ORDER BY link_id ASC";
			$result = mysql_query($query) or die('Query failed: ' . mysql_error());
			while ($line = mysql_fetch_array($result, MYSQL_ASSOC))
			{
?>
			<tr>
				<td>
				<ul>
<?php
				$ref = $key . "." . $line["q_id"];
				printf("<li><span class=\"linksub\"><a href=\"%s\">%s</a>:</span> %s</li>", $line["url"], $line["title"], $line["desc"]);
?>
				</ul>
				</td>
			</tr>
<?php
			}
		}
		// Closing connection
		mysql_close($link);
?>
		</table>
	</div>
	</body>
</html>
