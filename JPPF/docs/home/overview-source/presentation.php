<?php
	session_start();
?>
<html>
	$template{name="head-section" title="Links Page"}$

	<body>
		<div align="center">
		$template{name="jppf-header"}$

<?php
			$defined = $_SESSION["defined"];
			if (!$defined)
			{
				$_SESSION["defined"] = "true";
				$_SESSION["first"] = 0;
				$_SESSION["last"] = 18;
			}
			$current = $_REQUEST["current"];
			if (!$current)
			{
				$currrent = 0;
			}
?>

			<table align="center" cellspacing="0" cellpadding="0" width="70%">
				$template{name="row-blank" span="2"}$
				<tr>
					<td width="12" class="bleft"/>
					<td align="center" bgcolor="white">
<?php
					// Link to first page
					if ($current > $_SESSION["first"])
					{
						echo '<a href="presentation.php?current=', $_SESSION["first"], '">';
						echo '<img src="overview/first1.gif" border=0 alt="Last page"></a>';
					}
					else
					{
						echo '<img src="overview/first0.gif" border=0 alt="Last page">';
					}
					echo '&nbsp;';

					// Link to previous page
					if ($current > $_SESSION["first"])
					{
						echo '<a href="presentation.php?current=', ($current-1), '">';
						echo '<img src="overview/prev1.gif" border=0 alt="Back"></a>';
					}
					else
					{
						echo '<img src="overview/prev0.gif" border=0 alt="Back">';
					}
					echo '&nbsp;';

					// Link to next page
					if ($current < $_SESSION["last"])
					{
						echo '<a href="presentation.php?current=', ($current+1), '">';
						echo '<img src="overview/next1.gif" border=0 alt="Continue"></a>';
					}
					else
					{
						echo '<img src="overview/next0.gif" border=0 alt="Continue">';
					}
					echo '&nbsp;';

					// Link to last page
					if ($current < $_SESSION["last"])
					{
						echo '<a href="presentation.php?current=', $_SESSION["last"], '">';
						echo '<img src="overview/last1.gif" border=0 alt="Last page"></a>';
					}
					else
					{
						echo '<img src="overview/last0.gif" border=0 alt="Last page"></a>';
					}
?>
					</td>
					<td align="left" bgcolor="white">This presentation is available in <a href="documents/JPPF-Presentation.pdf">PDF Format</a></td>
					<td width="12" class="bright"/>
				</tr>
				<tr>
					<td width="12" class="bleft"/>
					<td align="center" colspan="2" bgcolor="white">
<?php
					echo '<img src="overview/img', $current, '.gif">';
?>
					</td>
					<td width="12" class="bright"/>
				</tr>
				$template{name="row-bottom" span="2"}$
			</table>
		</div>
	</body>
</html>
