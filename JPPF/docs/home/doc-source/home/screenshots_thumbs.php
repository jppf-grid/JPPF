<?php
  $picnum = $_REQUEST["picnum"];
  if (($picnum == NULL) || ($picnum == "")) {
    $picnum = '0';
  }
?>
<html>
  $template{name="head-section"}$
  <script>
    function jumpTo(id){
      window.location.hash = '#'+id;
    }
  </script>
  <body style="background-color: #E8EAFD; padding: transparent; margin: 0px" onLoad="jumpTo('pic_' + <?php echo $picnum; ?>)">
    $template{name="shots"}$
  <body>
</html>