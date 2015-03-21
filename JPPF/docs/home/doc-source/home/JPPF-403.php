<?php $currentPage="Credits" ?>
$template{name="about-page-header" title="Forbidden"}$
<?php
$port = $_SERVER["SERVER_PORT"];
$port = ($port == '80' ? '' : ':' . $port);
$ref_url = 'http://' . $_SERVER["SERVER_NAME"] . $port .$_SERVER["REDIRECT_URL"];
if (!$ref_url)
{
	$ref_url = "unknown";
}
?>
<br><div class="blockWithHighlightedTitle" style="padding-left: 5px; padding-right: 5px; min-height: 500px">
$template{name="title-with-icon" img="/images/403.png" title="Denied !" heading="h1"}$
<br>
<h3>You do not have permission to access:</h3>
<h3 style="color: red"><?php echo $ref_url ?></h3>
</div><br>
$template{name="about-page-footer"}$
