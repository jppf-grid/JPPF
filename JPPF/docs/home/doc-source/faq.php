<html>
	$template{name="head-section" title="FAQ Page"}$

	<body>
		<div align="center">
		$template{name="jppf-header"}$
		<table cellspacing="0" cellpadding="0" width="70%">
			$template{name="page-title" title="Frequently Asked Questions"}$

<?php
		$link = mysql_connect('mysql4-j', 'j135654admin', 'Faz600er')
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
<?php
		$count = 0;
		foreach ($groups as $key => $value)
		{
?>
			$template{name="row-middle" span="1"}$
			<tr>
				<td width="12" class="bleft"/>
				<td bgcolor="white">
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
				<td width="12" class="bright"/>
			</tr>
<?php
			mysql_free_result($result);
			$count++;
		}
?>
			$template{name="row-bottom" span="1"}$
		</table>
		<br>
<?php
		$count = 0;
		foreach ($groups as $key => $value)
		{
?>
		<table cellspacing="0" cellpadding="0" width="70%">
<?php
?>
			$template{name="row-top" span="1"}$
			<tr>
				<td width="12" class="bleft"/>
				<td bgcolor="white">
<?php
					printf("<h3><b><u><a name=\"%s\">%s %s</a></u></b></h3>", $key, $key, $value);
?>
				</td>
				<td width="12" class="bright"/>
			</tr>
<?php
			$query = "SELECT * FROM faq_questions WHERE group_id = '$key' ORDER BY q_id ASC";
			$result = mysql_query($query) or die('Query failed: ' . mysql_error());
			//echo '<ul>';
			while ($line = mysql_fetch_array($result, MYSQL_ASSOC))
			{
?>
			$template{name="row-blank" span="1"}$
			$template{name="row-middle" span="1"}$
			$template{name="row-blank" span="1"}$
			<tr>
				<td width="12" class="bleft"/>
				<td bgcolor="white">
<?php
				$ref = $key . "." . $line["q_id"];
?>
				<div align="center">
					<table width="100%" cellspacing="0" cellpadding="0" border="0"><tr><td>
<?php
						printf("<a name=\"%s\" style=\"color: #8080FF\"><b>%s %s</b></a>", $ref, $ref, $line["desc"]);
?>
						<div align="center">
						<table width="100%" cellspacing="0" cellpadding="0" border="0"><tr><td>
<?php
							printf("<br>%s", $line["content"]);
?>
						</td></tr></table>
					</td></tr></table>
				</div>
				</td>
				<td width="12" class="bright"/>
			</tr>
<?php
			}
			//echo '</ul>';
?>
			$template{name="row-bottom" span="1"}$
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
