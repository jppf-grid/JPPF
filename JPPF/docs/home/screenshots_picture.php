<?php
	$screenshot = $_REQUEST["screenshot"];
	if (($screenshot == NULL) || ($screenshot == "")) {
		$screenshot = 'AlertThresholds.gif
';
	}
  $shot_title = $_REQUEST["shotTitle"];
  if (($shot_title == NULL) || ($shot_title == "")) {
    $shot_title = 'Alert Thresholds
';
  }
?>
<html>
    <head>
    <title>JPPF ${title}</title>
    <meta name="description" content="The open source grid computing solution">
    <meta name="keywords" content="JPPF, java, parallel computing, distributed computing, grid computing, parallel, distributed, cluster, grid, cloud, open source">
    <meta HTTP-EQUIV="Content-Type" content="text/html; charset=UTF-8">
    <link rel="shortcut icon" href="images/jppf-icon.ico" type="image/x-icon">
    <link rel="stylesheet" type="text/css" href="/jppf.css" title="Style">
  </head>
  <body style="background: transparent; margin: 0px">
    <div align="center" class="blockWithHighlightedTitle" style="padding-bottom: 10px;">
      <div>
					<h3 align="center"><?php echo $shot_title; ?></h3>
      <div  style="margin: 1px">
        <img src="/screenshots/<?php echo $screenshot; ?>" border="0" alt="screenshot" style="max-width: 754px"/>
      </div>
      </div>
    </div>
  <body>
</html>
