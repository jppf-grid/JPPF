<html>
	$template{name="head-section" title="News Page"}$

	<body>
		<div align="center">
		$template{name="jppf-header"}$
		<table cellspacing="0" cellpadding="0" width="80%">
			<tr>
				<td>
					$template{name="page-title" title="$CONTENT[
						<a style="color: #8080FF" href="http://sourceforge.net/news/?group_id=135654">Latest news on project pages</a>
					]CONTENT$"}$
				</td>
			</tr>
		</table>

		<?php
		// Connecting, selecting database
		$link = mysql_connect('mysql4-j', 'j135654admin', 'Faz600er')
			 or die('Could not connect: ' . mysql_error());
		mysql_select_db('j135654_web') or die('Could not select database');

		// Performing SQL query
		$query = 'SELECT * FROM news ORDER BY date DESC';
		$result = mysql_query($query) or die('Query failed: ' . mysql_error());
		while ($line = mysql_fetch_array($result, MYSQL_ASSOC))
		{
		?>
		<table class="border_" cellspacing="0" cellpadding="5" width="80%">
			<tr>
				<td>
					<?php
						printf("<h3>%s %s</h3>", date("n/j/Y", strtotime($line["date"])), $line["title"]);
						printf("%s", $line["desc"]);
					?>
						<p><u style="color: #8080FF"><strong style="color: #8080FF">Summary of changes:</strong></u>
					<?php
						printf("%s", $line["content"]);
					?>
				</td>
			</tr>
		</table>
		<?php
		}
		// Free resultset
		mysql_free_result($result);
		// Closing connection
		mysql_close($link);
		?>

	</body>
</html>
