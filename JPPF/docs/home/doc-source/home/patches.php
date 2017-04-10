<?php
  require_once("db_settings.inc.php");
  $currentPage="Patches";
?>
$template{name="about-page-header" title="Patches"}$
<?php
  $link = mysql_connect($jppf_db_server, $jppf_db_user, $jppf_db_pwd) or die('Could not connect: ' . mysql_error());
  mysql_select_db($jppf_db_name) or die('Could not select database');
  $query = 'SELECT DISTINCT jppf_version FROM patch ORDER BY jppf_version DESC';
  $result = mysql_query($query) or die('Query failed: ' . mysql_error());
?>
  <table width="100%" border="0"><tr>
  <td align="left"><h1>JPPF Patches</h1></td>
<?php
  $versions = array();
  while ($line = mysql_fetch_array($result, MYSQL_ASSOC)) {
    $versions[] = $line["jppf_version"];
  }
  mysql_free_result($result);
?>
  <script>
    function jumpToVersion(id) {
      window.location.hash = '#' + id;
    }
  </script>
  <td align="right" class="version_select"><i>Jump to version:</i>
  <select name="version_select" class="version_select">
<?php
  foreach ($versions as $ver) {
?>
  <option value="<?php echo $ver ?>" onclick="jumpToVersion('<?php echo $ver ?>')"> <?php echo $ver ?> </option>
<?php
  }
?>
  </select></td>
  </tr></table>
<?php
  foreach ($versions as $jppf_ver) {
?>
    <div class="blockWithHighlightedTitle" style="padding: 0px">
    <a name="<?php echo $jppf_ver ?>"></a>
    <h2>&nbsp;JPPF <?php echo $jppf_ver ?></h2>

<?php
    $query = "SELECT * FROM patch where jppf_version = '" . $jppf_ver . "' ORDER BY patch_number ASC";
    $result = mysql_query($query) or die('Query failed: ' . mysql_error());
    //if (false)
    while ($line = mysql_fetch_array($result, MYSQL_ASSOC)) {
?>
    <!--<div style="background-color: white; border-radius: 10px">-->
    <table width="100%" border="0" cellspacing="0" cellpadding="0" style="border-top: solid 1px #B5C0E0; margin_left: -5px; margin-right: -8px">
    <tr>
      <td valign="top" width="80px" class="patch_cell" style="border-right: solid 1px #B5C0E0"><a href="patch_info.php?patch_id=<?php echo $line['id'] ?>"><b>patch <?php echo $line['patch_number'] ?></b></a></td>
<?php
      $port = ($_SERVER['SERVER_PORT'] == 80) ? '' : ':' . $_SERVER['SERVER_PORT'];
?>
      <td class="patch_cell"><b>Download:</b> <a href="<?php echo '/private/patch/' . $line['patch_url'] ?>"><?php echo $line['patch_url'] ?></a><br/>
      <b>Bugs:</b>
      <ul class="samplesList" style="margin-top: 0px; margin-bottom: 0px">
<?php
      $patch_number = $line["patch_number"];
      $query2 = "SELECT * FROM patch_bugs where jppf_version = '" . $jppf_ver . "' AND patch_number = '" . $patch_number . "'";
      $result2 = mysql_query($query2) or die('Query failed: ' . mysql_error());
      while ($line2 = mysql_fetch_array($result2, MYSQL_ASSOC)) {
?>
        <li><a href="<?php echo $line2['bug_url'] ?>"><?php echo $line2['bug_id'] ?>&nbsp;<?php echo $line2['bug_title'] ?></a></li>
<?php
      }
      mysql_free_result($result2);
?>
      </ul>
      </td>
    </tr>
    </table>
    <!--</div>-->
<?php
    }
?>
    </div>
    <br>
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
