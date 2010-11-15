<?php $currentPage="Patches" ?>
$template{name="about-page-header" title="Patches"}$
<?php
	// Connecting, selecting database
	$link = mysql_connect('localhost', 'pervasiv_jppfadm', 'tri75den')
		 or die('Could not connect: ' . mysql_error());
	mysql_select_db('pervasiv_jppfweb') or die('Could not select database');

	// Performing SQL query
	$query = 'SELECT DISTINCT jppf_version FROM patch ORDER BY jppf_version DESC';
	$result = mysql_query($query) or die('Query failed: ' . mysql_error());
?>
	<div align="center">
	<h1>JPPF Patches</h1>
	<table border="1" cellspacing="0" cellpadding="5">
		<!--
		<tr>
			<td><b>JPPF version</b></td>
			<td><b>Patches</b></td>
			<td><b>Details</b></td>
		</tr>
		-->
<?php
	while ($line = mysql_fetch_array($result, MYSQL_ASSOC))
	{
		$jppf_ver = $line["jppf_version"];
?>
		<tr><td colspan="3"><h3 style="margin: 0px">JPPF <?php echo $jppf_ver ?></h3></td></tr>

<?php
		$query2 = "SELECT * FROM patch where jppf_version = '" . $jppf_ver . "' ORDER BY patch_number ASC";
		$result2 = mysql_query($query2) or die('Query failed: ' . mysql_error());
		while ($line2 = mysql_fetch_array($result2, MYSQL_ASSOC))
		{
?>
		<tr>
			<td>patch <?php echo $line2['patch_number'] ?></td>
			<td><a href="patch_info.php?patch_id=<?php echo $line2['id'] ?>">full details</a></td>
			<td><a href="<?php echo $line2['patch_url'] ?>">patch <?php echo $line2['patch_url'] ?></a></td>
		</tr>
<?php
		}
		// Free resultset
		mysql_free_result($result2);
	}
?>
	</table>
	<br/>
	</div>
<?php
	// Free resultset
	mysql_free_result($result);
	// Closing connection
	mysql_close($link);
?>
$template{name="about-page-footer"}$
