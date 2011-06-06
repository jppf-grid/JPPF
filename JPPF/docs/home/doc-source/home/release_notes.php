<?php $currentPage="Release notes" ?>
<?php
// Connecting, selecting database
$link = mysql_connect('localhost', 'pervasiv_jppfadm', 'tri75den')
	 or die('Could not connect: ' . mysql_error());
mysql_select_db('pervasiv_jppfweb') or die('Could not select database');

$version = $_REQUEST["version"];
if (!$version)
{
	$version = "1.9.1";
}
// Performing SQL query
$query = "SELECT * FROM news WHERE version = '" . $version . "'";
$result = mysql_query($query) or die('Query failed: ' . mysql_error());
$line = mysql_fetch_array($result, MYSQL_ASSOC);
$title = "JPPF " . $version . " Release Notes";
?>
$template{name="about-page-header" title="<?php printf('%s', $title); ?>"}$
	<h1>
	<?php	printf('%s JPPF %s release notes', date("n/j/Y", strtotime($line["date"])), $version); ?>
	</h1>
	<p>
	<?php printf("%s", $line["content"]);?>
	<br>
<?php
// Free resultset
mysql_free_result($result);
// Closing connection
mysql_close($link);
?>
$template{name="about-page-footer"}$
