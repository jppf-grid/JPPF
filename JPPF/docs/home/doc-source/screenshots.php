<?php $currentPage="Screenshots" ?>
$template{name="about-page-header" title="Screenshots"}$
<?php
	$screenshot = $_REQUEST["screenshot"];
	if (($screenshot == NULL) || ($screenshot == ""))
	{
		$screenshot = "$template{name="first-shot"}$";
	}
?>
	<div align="center">
		<table border="0" cellspacing="0" cellpadding="0" width="80%">
			<tr>
				<td>
					$template{name="shots"}$
				</td>
			</tr>
		</table>
		<br>
				$template{name="block-header" title="<?php echo $screenshot; ?>"}$
				<div  style="margin: 1px">
				<br>
				<img src="screenshots/<?php echo $screenshot; ?>" border="0" alt="screenshot"/>
				</div>
				$template{name="block-footer"}$
		<table border="0" cellspacing="0" cellpadding="0">
			<tr><td align="center">
			</td></tr>
		</table>
	</div>
$template{name="about-page-footer"}$
