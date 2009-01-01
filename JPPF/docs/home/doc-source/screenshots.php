<html>
	$template{name="head-section" title="Screenshots Page"}$

	<body>
		<div align="center">
		$template{name="jppf-header"}$
<?php
			$screenshot = $_REQUEST["screenshot"];
			if (($screenshot == NULL) || ($screenshot == ""))
			{
				$screenshot = "$template{name="first-shot"}$";
			}
?>

		<!--<h1 align="center" style="color: blue">Screenshots</h1>-->

		<br>
		<table border="0" cellspacing="0" cellpadding="0" width="80%">
			<tr>
				<td>
					$template{name="shots"}$
				</td>
			</tr>
		</table>
		<br>
		<table border="0" cellspacing="0" cellpadding="0">
			<tr><td align="center">
				$template{name="highlight-header" span="1" title="<?php echo $screenshot; ?>"}$
					<div  style="margin: 10px">
					<br>
					<img src="screenshots/<?php echo $screenshot; ?>" border="0" alt="screenshot"/>
					</div>
				$template{name="highlight-bottom" span="1"}$
			</td></tr>
		</table>
		</div>
		$template{name="jppf-footer"}$
	</body>
</html>
