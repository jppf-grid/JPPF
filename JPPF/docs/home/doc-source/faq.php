<html>
	$template{name="head-section" title="FAQ Page"}$

	<body>
		<div align="center">
		$template{name="jppf-header"}$
		<table cellspacing="0" cellpadding="5" width="80%">
			$template{name="page-title" title="Frequently Asked Questions"}$
		</table>

<?php
		$link = mysql_connect('mysql4-j', 'j135654admin', 'tri75den')
			 or die('Could not connect: ' . mysql_error());
		mysql_select_db('j135654_web') or die('Could not select database');

		$query = 'SELECT * FROM faq_groups ORDER BY group_id ASC';
		$result = mysql_query($query) or die('Query failed: ' . mysql_error());
		$groups = array();
		while ($line = mysql_fetch_array($result, MYSQL_ASSOC))
		{
			$groups[$line["group_id"]] = $line["desc"];
		}
		mysql_free_result($result);
?>
		<table class="border_" cellspacing="0" cellpadding="5" width="80%">
<?php
		$count = 0;
		foreach ($groups as $key => $value)
		{
?>
			<tr><td height="2px" colspan="0" style="background-color: #8080FF"/></tr>
			<tr>
				<td>
<?php
			$query = "SELECT * FROM faq_questions WHERE group_id = '$key' ORDER BY q_id ASC";
			$result = mysql_query($query) or die('Query failed: ' . mysql_error());
			printf("<h4><b><a href=\"#%s\">%s %s</a></b></h4>", $key, $key, $value);
			echo '<ul>';
			while ($line = mysql_fetch_array($result, MYSQL_ASSOC))
			{
				$ref = $key . "." . $line["q_id"];
				printf("<li><a href=\"#%s\">%s %s</a></li>", $ref, $ref, $line["desc"]);
			}
			echo '</ul>';
?>
				</td>
			</tr>
<?php
			mysql_free_result($result);
			$count++;
		}
?>
			<tr><td height="2px" colspan="0" style="background-color: #8080FF"/></tr>
		</table>
		<br>
<?php
		$count = 0;
		foreach ($groups as $key => $value)
		{
?>
		<table class="border_" cellspacing="0" cellpadding="5" width="80%">
<?php
?>
			<tr><td height="10" style="background-color: #8080FF"/></tr>
			<tr>
				<td>
<?php
					printf("<h3><b><u><a name=\"%s\">%s %s</a></u></b></h3>", $key, $key, $value);
?>
				</td>
			</tr>
<?php
			$query = "SELECT * FROM faq_questions WHERE group_id = '$key' ORDER BY q_id ASC";
			$result = mysql_query($query) or die('Query failed: ' . mysql_error());
			//echo '<ul>';
			while ($line = mysql_fetch_array($result, MYSQL_ASSOC))
			{
?>
			<tr>
				<td style="border-top: 1px solid #8080FF">
<?php
				$ref = $key . "." . $line["q_id"];
?>
				<div align="center">
				<table width="97%" cellspacing="0" cellpadding="0" border="0"><tr><td>
<?php
				printf("<a name=\"%s\" style=\"color: #8080FF\"><b>%s %s</b></a>", $ref, $ref, $line["desc"]);
?>
				<div align="center">
				<table width="95%" cellspacing="0" cellpadding="0" border="0"><tr><td>
<?php
				printf("<br>%s", $line["content"]);
?>
				</td></tr></table>
				</td></tr></table>
				</div>
				</div>
				</td>
			</tr>
<?php
			}
			//echo '</ul>';
?>
			<tr><td height="10" style="background-color: #8080FF"/></tr>
		</table>
		<br>
<?php
		}
		// Closing connection
		mysql_close($link);
?>
		</div>

	</body>
</html>
