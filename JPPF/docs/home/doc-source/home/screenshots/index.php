<?php $currentPage="Screenshots" ?>
<?php include_once('resources/UberGallery.php'); ?>
$template{name="screenshots-page-header" title="JPPF Screenshots"}$
<h1 align="center">Screenshots</h1>
<?php
      $files = scandir('gallery-images');
      foreach ($files as $file):
        $dir = 'gallery-images/' . $file;
        if (is_dir($dir) && $file != '.' && $file != '..'):
?>
          <div class="blockWithHighlightedTitle" style="padding: 0px">
          <h2 style="margin-left: 10px"><?php echo ucwords($file); ?></h2>

<?php
          $gallery = UberGallery::init()->createGallery($dir, $file);
?>
          <br></div><br/>
<?php
        endif;
      endforeach;
?>
<div id="credit">Powered by, <a href="http://www.ubergallery.net">UberGallery</a></div>

$template{name="about-page-footer"}$
