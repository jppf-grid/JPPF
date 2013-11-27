<?php $currentPage="Patches" ?>
$template{name="about-page-header" title="Patches"}$
<?php
  // Connecting, selecting database
  $link = mysql_connect('localhost', 'lolocohe_jppfadm', 'tri75den')
     or die('Could not connect: ' . mysql_error());
  mysql_select_db('lolocohe_jppfweb') or die('Could not select database');

  // Performing SQL query
  $query = 'SELECT DISTINCT jppf_version FROM patch ORDER BY jppf_version DESC';
  $result = mysql_query($query) or die('Query failed: ' . mysql_error());
?>
  <h1 align="center">JPPF Patches</h1>
<?php
  $versions = array();
  while ($line = mysql_fetch_array($result, MYSQL_ASSOC))
  {
    $versions[] = $line["jppf_version"];
  }
  mysql_free_result($result);
  foreach ($versions as $jppf_ver)
  {
?>
    <div style="margin: 10px">
    <h2>JPPF <?php echo $jppf_ver ?></h2>
    <table width="100%" border="1" cellspacing="0" cellpadding="7">

<?php
    $query = "SELECT * FROM patch where jppf_version = '" . $jppf_ver . "' ORDER BY patch_number ASC";
    $result = mysql_query($query) or die('Query failed: ' . mysql_error());
    //if (false)
    while ($line = mysql_fetch_array($result, MYSQL_ASSOC))
    {
?>
    <tr>
      <td valign="top" width="80px"><a href="patch_info.php?patch_id=<?php echo $line['id'] ?>"><b>patch <?php echo $line['patch_number'] ?></b></a></td>
<?php
      $port = ($_SERVER['SERVER_PORT'] == 80) ? '' : ':' . $_SERVER['SERVER_PORT'];
?>
      <td><b>Download:</b> <a href="<?php echo '/private/patch/' . $line['patch_url'] ?>"><?php echo $line['patch_url'] ?></a><br/>
      <b>Bugs:</b>
      <ul class="samplesList" style="margin-top: 0px; margin-bottom: 0px">
<?php
      $patch_number = $line["patch_number"];
      $query2 = "SELECT * FROM patch_bugs where jppf_version = '" . $jppf_ver . "' AND patch_number = '" . $patch_number . "'";
      $result2 = mysql_query($query2) or die('Query failed: ' . mysql_error());
      while ($line2 = mysql_fetch_array($result2, MYSQL_ASSOC))
      {
?>
        <li><a href="<?php echo $line2['bug_url'] ?>"><?php echo $line2['bug_id'] ?>&nbsp;<?php echo $line2['bug_title'] ?></a></li>
<?php
      }
      mysql_free_result($result2);
?>
      </ul>
      </td>
    </tr>
<?php
    }
?>
    </table>
    </div>
<?php
    // Free resultset
    mysql_free_result($result);
  }
?>
  <br/>
<?php
  // Closing connection
  mysql_close($link);
?>
$template{name="about-page-footer"}$
