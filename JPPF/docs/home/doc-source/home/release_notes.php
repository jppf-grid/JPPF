<?php
  require_once("db_settings.inc.php");
  $currentPage="Release notes";
?>
<?php
$link = mysql_connect($jppf_db_server, $jppf_db_user, $jppf_db_pwd) or die('Could not connect: ' . mysql_error());
mysql_select_db($jppf_db_name) or die('Could not select database');

$version = $_REQUEST["version"];
if (!$version) {
	$version = "5.1";
}
// Performing SQL query
$query = "SELECT * FROM news WHERE version = '" . $version . "'";
$result = mysql_query($query) or die('Query failed: ' . mysql_error());
$line = mysql_fetch_array($result, MYSQL_ASSOC);
$title = "JPPF " . $version . " Release Notes";
?>
$template{name="about-page-header" title="<?php printf('%s', $title); ?>"}$
  <h1><?php printf('%s JPPF %s release notes', date("n/j/Y", strtotime($line["date"])), $version); ?></h1>
  <div class="blockWithHighlightedTitle">
	<p><?php printf("%s", $line["content"]);?>
  </div>
	<br>
<?php
// Free resultset
mysql_free_result($result);
// Closing connection
mysql_close($link);
?>
$template{name="about-page-footer"}$
