<html>
	$template{name="head-section" title="Links Page"}$

	<body>
		<div align="center">
		$template{name="jppf-header"}$
		<table cellspacing="0" cellpadding="0" width="70%">
			$template{name="page-title" title="Related Links" span="1"}$

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
			$template{name="row-bottom" span="1"}$
			$template{name="row-top" span="1"}$
			<tr>
				<td width="12" class="bleft"/>
				<td bgcolor="white">
					$template{name="highlight-top" span="1" color="pblue"}$
<?php
					printf("<span class='newsTitle'>%s</span>", $value);
?>
					$template{name="highlight-bottom" span="1" color="pblue"}$
				</td>
				<td width="12" class="bright"/>
			</tr>
			$template{name="row-blank" span="1"}$
<?php
			$query = "SELECT * FROM links WHERE group_id = '$key' ORDER BY link_id ASC";
			$result = mysql_query($query) or die('Query failed: ' . mysql_error());
			while ($line = mysql_fetch_array($result, MYSQL_ASSOC))
			{
?>
			<tr>
				<td width="12" class="bleft"/>
				<td bgcolor="white">
				<ul>
<?php
				$ref = $key . "." . $line["q_id"];
				printf("<li><span class=\"linksub\"><a href=\"%s\">%s</a>:</span> %s</li>", $line["url"], $line["title"], $line["desc"]);
?>
				</ul>
				</td>
				<td width="12" class="bright"/>
			</tr>
<?php
			}
		}
		// Closing connection
		mysql_close($link);
?>
			$template{name="row-bottom" span="1"}$
		</table>
	</div>
	</body>
</html>
