<?php
$currentPage = $_REQUEST["page"];
if (($currentPage == NULL) || ($currentPage == ""))
{
	$currentPage = "Home";
}
require($currentPage."php");
?>
