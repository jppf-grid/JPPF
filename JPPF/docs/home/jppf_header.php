<?php
		if (!isset($currentPage))
		{
			$currentPage = $_REQUEST["page"];
			if (($currentPage == NULL) || ($currentPage == ""))
			{
				$currentPage = "Home";
			}
		}
		if ($currentPage != "Forums")
		{
		?>
		<div style="background-color: #E2E4F0; margin: 0px;height: 10px"><img src="/images/frame_top.gif"/></div>
		<?php
		}
		?>
		<table width="100%" cellspacing="0" cellpadding="0" border="0" class="jppfheader" style="border-left: 1px solid #6D78B6; border-right: 1px solid #6D78B6">
			<tr style="height: 80px">
				<td width="20"></td>
				<td width="400" align="left" valign="center"><a href="/"><img src="/images/logo2.gif" border="0" alt="JPPF"/></a></td>
				<td align="right">
					<table border="0" cellspacing="0" cellpadding="0" style="height: 30px; background-color:transparent;">
						<tr>
							<td style="background: transparent url('/images/buttons/<?php if ($currentPage == "Home") echo "btn_start.gif"; else echo "btn_active_start.gif"; ?>') repeat-x scroll left bottom; width: 9px"></td>
							<td style="width: 1px"></td>
							<?php
if ($currentPage == "Home")
{
?>
<td class="headerMenuItem2">&nbsp;<a href="/index.php" class="headerMenuItem2">Home</a>&nbsp;</td>
<?php
}
else
{
?>
<td class="headerMenuItem">&nbsp;<a href="/index.php" class="headerMenuItem">Home</a>&nbsp;</td>
<?php
}
?>
								<td style="width: 1px"></td>
							<?php
if ($currentPage == "About")
{
?>
<td class="headerMenuItem2">&nbsp;<a href="/about.php" class="headerMenuItem2">About</a>&nbsp;</td>
<?php
}
else
{
?>
<td class="headerMenuItem">&nbsp;<a href="/about.php" class="headerMenuItem">About</a>&nbsp;</td>
<?php
}
?>
								<td style="width: 1px"></td>
							<?php
if ($currentPage == "Download")
{
?>
<td class="headerMenuItem2">&nbsp;<a href="/downloads.php" class="headerMenuItem2">Download</a>&nbsp;</td>
<?php
}
else
{
?>
<td class="headerMenuItem">&nbsp;<a href="/downloads.php" class="headerMenuItem">Download</a>&nbsp;</td>
<?php
}
?>
								<td style="width: 1px"></td>
							<?php
if ($currentPage == "Documentation")
{
?>
<td class="headerMenuItem2">&nbsp;<a href="/doc/v3/index.php?title=JPPF_3.x_Documentation" class="headerMenuItem2">Documentation</a>&nbsp;</td>
<?php
}
else
{
?>
<td class="headerMenuItem">&nbsp;<a href="/doc/v3/index.php?title=JPPF_3.x_Documentation" class="headerMenuItem">Documentation</a>&nbsp;</td>
<?php
}
?>
								<td style="width: 1px"></td>
							<?php
if ($currentPage == "Forums")
{
?>
<td class="headerMenuItem2">&nbsp;<a href="/forums" class="headerMenuItem2">Forums</a>&nbsp;</td>
<?php
}
else
{
?>
<td class="headerMenuItem">&nbsp;<a href="/forums" class="headerMenuItem">Forums</a>&nbsp;</td>
<?php
}
?>
								<td style="width: 1px"></td>
							<td style="background: transparent url('/images/buttons/<?php if ($currentPage == "Forums") echo "btn_end.gif"; else echo "btn_active_end.gif"; ?>') repeat-x scroll right bottom; width: 9px"></td>
						</tr>
					</table>
				</td>
				<td width="20"></td>
			</tr>
		</table>
