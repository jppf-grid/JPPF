<html>
	$template{name="head-section" title="News Page"}$

	<body>
		<div align="center">
		$template{name="jppf-header"}$
		<table cellspacing="0" cellpadding="0" width="80%">
		<?php
		// Connecting, selecting database
		$link = mysql_connect('localhost', 'pervasiv_jppfadm', 'tri75den')
			 or die('Could not connect: ' . mysql_error());
		mysql_select_db('pervasiv_jppfweb') or die('Could not select database');

		// Performing SQL query
		$query = 'SELECT * FROM news ORDER BY date DESC';
		$result = mysql_query($query) or die('Query failed: ' . mysql_error());
		$i = 0;
		while ($line = mysql_fetch_array($result, MYSQL_ASSOC))
		{
		?>
			<tr>
				<td>
					<?php
					$i++;
					$title = date("n/j/Y", strtotime($line["date"])) . " " . $line["title"];
					?>
					<br>
					<?php printf("<a name='news%d'/>", $i); ?>
					$template{name="highlight-header" span="1" color="blue" color2="yellow" title="<?php printf('%s', $title); ?>"}$
					<?php
						printf("<br>%s", $line["desc"]);
					?>
						<p><u style="color: #8080FF"><strong style="color: #8080FF">Summary of changes:</strong></u>
					<?php
						printf("%s", $line["content"]);
					?>
					$template{name="highlight-bottom" span="1" color="yellow"}$
					<br>
				</td>
			</tr>
		<?php
		}
		// Free resultset
		mysql_free_result($result);
		// Closing connection
		mysql_close($link);
		?>
		</table>

		$template{name="jppf-footer"}$
	</body>
</html>
