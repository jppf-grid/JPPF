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
<table align="left" border="0" cellpadding="5">
	<tr>
		<td valign="middle">
			<img src="/images/404.png"/>
		</td>
		<td valign="middle">
			<h1>Apologies ... </h1>
		</td>
	</tr>
	<tr>
		<td colspan="2">
			<h3>The requested page:</h3>
			<h3 style="color: red"><?php echo $ref_url ?></h3>
			<h3>could not be found</h3>
		</td>
	</tr>
</table>
$template{name="about-page-footer"}$
