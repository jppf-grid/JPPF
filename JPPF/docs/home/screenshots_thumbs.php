<?php
  $picnum = $_REQUEST["picnum"];
  if (($picnum == NULL) || ($picnum == "")) {
    $picnum = '0';
  }
?>
<html>
    <head>
    <title>JPPF ${title}</title>
    <meta name="description" content="The open source grid computing solution">
    <meta name="keywords" content="JPPF, java, parallel computing, distributed computing, grid computing, parallel, distributed, cluster, grid, cloud, open source, android, .net">
    <meta HTTP-EQUIV="Content-Type" content="text/html; charset=UTF-8">
    <link rel="shortcut icon" href="images/jppf-icon.ico" type="image/x-icon">
    <link rel="stylesheet" type="text/css" href="/jppf.css" title="Style">
  </head>
  <script>
    function jumpTo(id){
      window.location.hash = '#'+id;
    }
  </script>
  <body style="background-color: #E8EAFD; padding: transparent; margin: 0px" onLoad="jumpTo('pic_' + <?php echo $picnum; ?>)">
    					<table align="center" border="0" cellspacing="0" cellpadding="4">
						</tr>
					</table>
  <body>
</html>
