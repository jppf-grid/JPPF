<html>
	$template{name="head-section" title="News Page"}$

	<body>
		<div align="center">
		$template{name="jppf-header"}$
		<table cellspacing="0" cellpadding="0" width="70%">
		<?php
		// Connecting, selecting database
		$link = mysql_connect('mysql4-j', 'j135654admin', 'Faz600er')
			 or die('Could not connect: ' . mysql_error());
		mysql_select_db('j135654_web') or die('Could not select database');

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
				$template{name="row-bottom" span="1"}$
				$template{name="row-top" span="1"}$
			<?php
			}
			$i = $i + 1;
			?>
			$template{name="row-blank" span="1"}$
			<tr>
				<td width="12" class="bleft"/>
				<td bgcolor="white">
					$template{name="highlight-top" span="1" color="pblue"}$
					<?php
						printf("<span class='newsTitle'>%s %s</span>", date("n/j/Y", strtotime($line["date"])), $line["title"]);
					?>
					$template{name="highlight-bottom" span="1" color="pblue"}$
					<?php
						printf("<br>%s", $line["desc"]);
					?>
						<p><u style="color: #8080FF"><strong style="color: #8080FF">Summary of changes:</strong></u>
					<?php
						printf("%s", $line["content"]);
					?>
				</td>
				<td width="12" class="bright"/>
			</tr>
		<?php
		}
		// Free resultset
		mysql_free_result($result);
		// Closing connection
		mysql_close($link);
		?>
			$template{name="row-blank" span="1"}$
			$template{name="row-bottom" span="1"}$
		</table>

	</body>
</html>
