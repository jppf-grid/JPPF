<?php
	$screenshot = $_REQUEST["screenshot"];
	if (($screenshot == NULL) || ($screenshot == "")) {
		$screenshot = '$template{name="first-shot"}$';
	}
  $shot_title = $_REQUEST["shotTitle"];
  if (($shot_title == NULL) || ($shot_title == "")) {
    $shot_title = '$template{name="first-shot-title"}$';
  }
?>
<html>
  $template{name="head-section"}$
  <body style="background: transparent; margin: 0px">
    <div align="center" class="blockWithHighlightedTitle" style="padding-bottom: 10px;">
      $template{name="block-header" title="<?php echo $shot_title; ?>"}$
      <div  style="margin: 1px">
        <img src="/screenshots/<?php echo $screenshot; ?>" border="0" alt="screenshot" style="max-width: 754px"/>
      </div>
      $template{name="block-footer"}$
    </div>
  <body>
</html>