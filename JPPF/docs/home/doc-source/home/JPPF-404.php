<?php $currentPage="Credits" ?>
$template{name="about-page-header" title="Page not found"}$
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
$template{name="title-with-icon" img="/images/404.png" title="Apologies ... " heading="h1"}$
<br><h3>The requested page:</h3>
<h3 style="color: red"><?php echo $ref_url ?></h3>
<h3>could not be found</h3>
</div><br>
$template{name="about-page-footer"}$
