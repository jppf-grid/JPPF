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
<table align="left" border="0" cellpadding="5">
	<tr>
		<td valign="middle" style="width: 140px">
			<img src="/images/403.png"/>
		</td>
		<td valign="middle">
			<h1>Denied !</h1>
		</td>
	</tr>
	<tr>
		<td colspan="2">
			<h3>You do not have permission to access:</h3>
			<h3 style="color: red"><?php echo $ref_url ?></h3>
		</td>
	</tr>
</table>
$template{name="about-page-footer"}$
