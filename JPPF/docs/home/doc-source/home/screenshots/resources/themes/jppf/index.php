<?php $currentPage="Screenshots" ?>
$template{name="screenshots-page-header" title="JPPF Screenshots"}$
<h1 align="center">Screenshots</h1>
<?php
  $galleryArray['relText'] = 'colorbox';
  echo $gallery->readTemplate('templates/defaultGallery.php', $galleryArray);
?>
$template{name="about-page-footer"}$
