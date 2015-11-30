<?php $currentPage="Screenshots" ?>
$template{name="about-page-header" title="Screenshots"}$
<?php
	$screenshot = $_REQUEST["screenshot"];
	if (($screenshot == NULL) || ($screenshot == "")) {
		$screenshot = '$template{name="first-shot"}$';
	}
	$shot_title = $_REQUEST["shotTitle"];
	if (($shot_title == NULL) || ($shot_title == "")) {
		$shot_title = '$template{name="first-shot-title"}$';
	}
  $pic_height = $_REQUEST["height"];
  if (($pic_height == NULL) || ($pic_height == "")) {
    $pic_height = '800';
  }
  $picnum = $_REQUEST["picnum"];
  if (($picnum == NULL) || ($picnum == "")) {
    $picnum = '0';
  }
?>

<div align="center">
  <br>
  <div align="center" class="blockWithHighlightedTitle" style="padding-top: 5px; padding-bottom: 5px">
    <iframe src="/screenshots_thumbs.php?picnum=<?php echo $picnum; ?>" height="153px" width="100%" frameborder="0" id="thumbs_frame"></iframe><br>
  </div>
  <br>
  <iframe src="/screenshots_picture.php?screenshot=<?php echo $screenshot ?>&shotTitle=<?php echo $shot_title; ?>" width="100%" height="<?php echo $pic_height; ?>" frameborder="0" name="pic" scrolling="no"></iframe><br>
  <br>
</div>
$template{name="about-page-footer"}$
