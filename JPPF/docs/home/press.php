<?php $currentPage="Press" ?>
<?php $jppfVersion="5.0" ?>
<html>
	  <head>
    <title>JPPF Press Kit
</title>
    <meta name="description" content="The open source grid computing solution">
    <meta name="keywords" content="JPPF, java, parallel computing, distributed computing, grid computing, parallel, distributed, cluster, grid, cloud, open source">
    <meta HTTP-EQUIV="Content-Type" content="text/html; charset=UTF-8">
    <link rel="shortcut icon" href="images/jppf-icon.ico" type="image/x-icon">
    <link rel="stylesheet" type="text/css" href="/jppf.css" title="Style">
  </head>
	<body>
		<div align="center">
		<div class="gwrapper" align="center">
			<?php
    if (!isset($currentPage))
    {
      $currentPage = $_REQUEST["page"];
      if (($currentPage == NULL) || ($currentPage == ""))
      {
        $currentPage = "Home";
      }
    }
    if ($currentPage != "Forums")
    {
    ?>
    <div style="background-color: #E2E4F0; margin: 0px;height: 10px"><img src="/images/frame_top.gif"/></div>
    <?php
    }
    ?>
    <table width="100%" cellspacing="0" cellpadding="0" border="0" class="jppfheader" style="border-left: 1px solid #6D78B6; border-right: 1px solid #6D78B6">
      <tr style="height: 80px">
        <td width="20"></td>
        <td width="400" align="left" valign="center"><a href="/"><img src="/images/logo2.gif" border="0" alt="JPPF"/></a></td>
        <td align="right">
          <table border="0" cellspacing="0" cellpadding="0" style="height: 30px; background-color:transparent;">
            <tr>
              <td style="background: transparent url('/images/buttons/<?php if ($currentPage == "Home") echo "btn_start.gif"; else echo "btn_active_start.gif"; ?>') repeat-x scroll left bottom; width: 9px"></td>
              <td style="width: 1px"></td>
              <?php
if ($currentPage == "Home")
{
?>
<td class="headerMenuItem2">&nbsp;<a href="/index.php" class="headerMenuItem2">Home</a>&nbsp;</td>
<?php
}
else
{
?>
<td class="headerMenuItem">&nbsp;<a href="/index.php" class="headerMenuItem">Home</a>&nbsp;</td>
<?php
}
?>
								<td style="width: 1px"></td>
              <?php
if ($currentPage == "About")
{
?>
<td class="headerMenuItem2">&nbsp;<a href="/about.php" class="headerMenuItem2">About</a>&nbsp;</td>
<?php
}
else
{
?>
<td class="headerMenuItem">&nbsp;<a href="/about.php" class="headerMenuItem">About</a>&nbsp;</td>
<?php
}
?>
								<td style="width: 1px"></td>
              <?php
if ($currentPage == "Features")
{
?>
<td class="headerMenuItem2">&nbsp;<a href="/features.php" class="headerMenuItem2">Features</a>&nbsp;</td>
<?php
}
else
{
?>
<td class="headerMenuItem">&nbsp;<a href="/features.php" class="headerMenuItem">Features</a>&nbsp;</td>
<?php
}
?>
								<td style="width: 1px"></td>
              <?php
if ($currentPage == "Download")
{
?>
<td class="headerMenuItem2">&nbsp;<a href="/downloads.php" class="headerMenuItem2">Download</a>&nbsp;</td>
<?php
}
else
{
?>
<td class="headerMenuItem">&nbsp;<a href="/downloads.php" class="headerMenuItem">Download</a>&nbsp;</td>
<?php
}
?>
								<td style="width: 1px"></td>
              <?php
if ($currentPage == "Documentation")
{
?>
<td class="headerMenuItem2">&nbsp;<a href="/doc/v5" class="headerMenuItem2">Documentation</a>&nbsp;</td>
<?php
}
else
{
?>
<td class="headerMenuItem">&nbsp;<a href="/doc/v5" class="headerMenuItem">Documentation</a>&nbsp;</td>
<?php
}
?>
								<td style="width: 1px"></td>
              <?php
if ($currentPage == "Forums")
{
?>
<td class="headerMenuItem2">&nbsp;<a href="/forums" class="headerMenuItem2">Forums</a>&nbsp;</td>
<?php
}
else
{
?>
<td class="headerMenuItem">&nbsp;<a href="/forums" class="headerMenuItem">Forums</a>&nbsp;</td>
<?php
}
?>
								<td style="width: 1px"></td>
              <td style="background: transparent url('/images/buttons/<?php if ($currentPage == "Forums") echo "btn_end.gif"; else echo "btn_active_end.gif"; ?>') repeat-x scroll right bottom; width: 9px"></td>
            </tr>
          </table>
        </td>
        <td width="20"></td>
      </tr>
    </table>
			<table border="0" cellspacing="0" cellpadding="5" width="100%px" style="border: 1px solid #6D78B6; border-top: 8px solid #6D78B6;">
			<tr>
				<td style="background-color: #FFFFFF">
				<div class="sidebar">
					        <?php if ($currentPage == "Home") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/" class="<?php echo $itemClass; ?>">&raquo; Home</a><br></div>
        <?php if ($currentPage == "About") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/about.php" class="<?php echo $itemClass; ?>">&raquo; About</a><br></div>
        <?php if ($currentPage == "Download") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/downloads.php" class="<?php echo $itemClass; ?>">&raquo; Download</a><br></div>
        <?php if ($currentPage == "Features") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/features.php" class="<?php echo $itemClass; ?>">&raquo; Features</a><br></div>
        <?php if ($currentPage == "Patches") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/patches.php" class="<?php echo $itemClass; ?>">&raquo; Patches</a><br></div>
        <?php if ($currentPage == "Samples") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/samples-pack/index.php" class="<?php echo $itemClass; ?>">&raquo; Samples</a><br></div>
        <?php if ($currentPage == "License") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/license.php" class="<?php echo $itemClass; ?>">&raquo; License</a><br></div>
        <hr/>
                <?php if ($currentPage == "Documentation") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/doc/v5" class="<?php echo $itemClass; ?>">&raquo; Documentation</a><br></div>
        <?php if ($currentPage == "v5.x") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/v5" class="<?php echo $itemClass; ?>">v5.x</a><br></div>
        <?php if ($currentPage == "v4.x") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/v4" class="<?php echo $itemClass; ?>">v4.x</a><br></div>
        <?php if ($currentPage == "v3.x") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/v3" class="<?php echo $itemClass; ?>">v3.x</a><br></div>
        <?php if ($currentPage == "Javadoc") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/api-5" class="<?php echo $itemClass; ?>">&raquo; Javadoc</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/api-5" class="<?php echo $itemClass; ?>">v5.x</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/api" class="<?php echo $itemClass; ?>">v4.x</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/api-3" class="<?php echo $itemClass; ?>">v3.x</a><br></div>
        <?php if ($currentPage == ".Net API") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/csdoc-5" class="<?php echo $itemClass; ?>">&raquo; .Net API</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/csdoc-5" class="<?php echo $itemClass; ?>">v5.x</a><br></div>
        <hr/>
        <?php if ($currentPage == "Issue tracker") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/tracker/tbg" class="<?php echo $itemClass; ?>">&raquo; Issue tracker</a><br></div>
        <?php if ($currentPage == "bugs") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/1/search/1" class="<?php echo $itemClass; ?>">bugs</a><br></div>
        <?php if ($currentPage == "features") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/9/search/1" class="<?php echo $itemClass; ?>">features</a><br></div>
        <?php if ($currentPage == "enhancements") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/2/search/1" class="<?php echo $itemClass; ?>">enhancements</a><br></div>
        <?php if ($currentPage == "next version") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/8/search/1" class="<?php echo $itemClass; ?>">next version</a><br></div>
        <?php if ($currentPage == "maintenance") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/22/search/1" class="<?php echo $itemClass; ?>">maintenance</a><br></div>
        <hr/>
        <?php if ($currentPage == "Press") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/press.php" class="<?php echo $itemClass; ?>">&raquo; Press</a><br></div>
        <?php if ($currentPage == "Release notes") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/release_notes.php?version=5.0" class="<?php echo $itemClass; ?>">&raquo; Release notes</a><br></div>
        <?php if ($currentPage == "Quotes") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/quotes.php" class="<?php echo $itemClass; ?>">&raquo; Quotes</a><br></div>
        <?php if ($currentPage == "Screenshots") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/screenshots.php?screenshot=&shotTitle=" class="<?php echo $itemClass; ?>">&raquo; Screenshots</a><br></div>
        <?php if ($currentPage == "News") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/news.php" class="<?php echo $itemClass; ?>">&raquo; News</a><br></div>
        <hr/>
        <?php if ($currentPage == "Contacts") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/contacts.php" class="<?php echo $itemClass; ?>">&raquo; Contacts</a><br></div>
        <?php if ($currentPage == "Services") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/services.php" class="<?php echo $itemClass; ?>">&raquo; Services</a><br></div>
        <br/>
				</div>
				<div class="jppf_content">
<div align="justify">
  <h1>JPPF Press Kit</h1>
  <h3>Content</h3>
  <div class="u_link" style="margin-left: 10px">
    <a href="/release_notes.php?version=<?php echo $jppfVersion ?>">Text of the original release</a><br>
    <a href="#features">Features</a><br>
    <a href="#downloads">Where to download</a><br>
    <a href="#documentation">Documentation</a><br>
    <a href="#license">License</a><br>
    <a href="#contacts">Contacts</a><br>
  </div>
  <br>
  <a name="original_release"></a>
<!-- ============================== -->
<!-- start version-specific content -->
<!-- ============================== -->
<h3><img src="images/icons/news.png" class="titleWithIcon"/>Press release: JPPF <?php echo $jppfVersion ?></h3>
<p><b>.Net integration</b>: the main focus of this release, the <a href="/doc/v5/index.php?title=.Net_Bridge">.Net bridge</a> for JPPF brings JPPF grids to the .Net world.
<ul class="samplesList">
  <li>Submit pure .Net workloads and execute them on .Net-capable JPPF nodes</li>
  <li>The .Net <a href="/doc/v5/index.php?title=Using_the_JPPF_.Net_API">client API</a> is almost identical to the Java API</li>
  <li>Includes full <a href="/doc/v5/index.php?title=Management_and_monitoring_from_.Net">grid management and monitoring features</a></li>
  <li>.Net and Java clients can mix freely in the same grid</li>
</ul>
<p><b>Administration console extensions</b>:
<ul class="samplesList">
  <li>new extension point: add your own <a href="/doc/v5/index.php?title=Pluggable_views">pluggable view</a> to the administration tool.
  A new sample <a href="/samples-pack/PluggableView/Readme.php">"topology event log"</a> is provided to showcase this feature.</li>
  <li>any built-in view <a href="/doc/v5/index.php?title=Hiding_built-in_views">can be hidden</a> at will</li>
  <li>the administration console can be <a href="/doc/v5/index.php?title=Embedding_the_administration_console">embedded</a> in any other Swing application</li>
  <li>the columns in all tree views can now be <a href="/screenshots.php?screenshot=VisibleColumns.gif&shotTitle=Visible Columns">switched from visible to hidden</a> on demand</li>
  <li>ability to <a href="/screenshots.php?screenshot=ExportConsoleSettings.gif&shotTitle=Export Console Settings">import/export the console settings</a>, including window size and location, value of persistent fields, tree columns' hidden state and width, charts definitions</li>
  <li>new <a href="/screenshots.php?screenshot=Charts-02.gif&shotTitle=Charts 02">charts types and fields</a> are now available for built-in and user-defined charts</li>
  <li>the console was refactored to use more consistent code and APIs. In particular, it is now based on the new <a href="/doc/v5/index.php?title=Grid_topology_monitoring">grid topology monitoring</a> API</li>
</ul>
<p><b>New APIs</b>:
<ul class="samplesList">
  <li>A new <a href="/doc/v5/index.php?title=Grid_topology_monitoring">grid topology monitoring</a> API was added, enabling developers to programmatically browse the JPPF topology and receive notifications of any change. This is also the API the administration console is based on</li>
  <li>New and convenient methods were added to easily explore the client <a href="/doc/v5/index.php?title=Connection_pools#Exploring_the_connections_in_a_pool">connections pools</a> and obtain connection objects</li>
  <li>Execution policies now have access to <a href="/doc/v5/index.php?title=Execution_Policies#Execution_policy_context">contextual information</a> during their evaluation</li>
  <li>Connection pools defined in the configuration can now <a href="/doc/v5/index.php?title=Configuring_SSL/TLS_communications#In_the_clients">indivdually specify</a> whether secure connections should be used</li>
  <li>A <a href="/doc/v5/index.php?title=Submitting_multiple_jobs_concurrently#The_AbstractJPPFJobStream_helper_class">new helper class</a> is provided to facilitate the implemetntation of job streaming patterns</li>
</ul>
<p><b>Server extensions and improvements</b>:
<ul class="samplesList">
  <li>It is now possible to <a href="/doc/v5/index.php?title=Receiving_the_status_of_tasks_returning_from_the_nodes">receive the status of tasks</a> returning from the nodes with fine details</li>
  <li>The thread pool management was refactored, resulting in many less threads created and increased scalability</li>
</ul>
<p><b>Management and monitoring</b>:
<ul class="samplesList">
  <li>server monitoring: all MBean methods getting information on the nodes now accept a NodeSelector parameter to provide fine-grained filtering</li>
  <li>server management: <a href="/doc/v5/index.php?title=Server_management#Driver_UDP_broadcasting_state">server broadcasting</a> can now be remotely enabled or disabled on-demand</li>
  <li>nodes reprovisioning requests, as well as shutdown and restart requests, can now be deferred until the nodes are idle</li>
</ul>
<p><b>Deployment</b>:
<ul class="samplesList">
  <li>Servers and nodes can now be <a href="/doc/v5/index.php?title=Drivers_and_nodes_as_services#Windows_services_with_Apache.27s_commons-daemon">installed as Windows services</a> without having to download a third-party library</li>
  <li>Nodes in "idle host" mode (aka CPU scavenging) can now be configured to stop <a href="/doc/v5/index.php?title=Nodes_in_%22Idle_Host%22_mode">only when the current tasks are complete</a></li>
</ul>
<p><b>Refactoring of distribution packaging</b>: the JPPF jar files were <a href="/doc/v5/index.php?title=Changes_in_JPPF_5.0#New_packaging">refactored</a> to adopt a more meaningful naming and a consistent distribution of the code.
<!-- ============================== -->
<!-- end version-specific content   -->
<!-- ============================== -->
<a name="features"></a>
  <h3><img src="images/icons/view-list.png" class="titleWithIcon"/>Features</h3>
  <div class="u_link" style="margin-left: 10px">
    <a href="release_notes.php?version=<?php echo $jppfVersion ?>">Release notes</a>: see everything that's new in JPPF <?php echo $jppfVersion ?><br>
    Our <a href="features.php">features page</a> provides a comprenhensive overview of what JPPF has to offer.<br>
  </div>
  <a name="downloads"></a>
  <h3><img src="images/icons/download.png" class="titleWithIcon"/>Downloads</h3>
  All files can be found from our <a href="/downloads.php">downloads page</a>.<br>
  <a name="documentation"></a>
  <h3><img src="images/icons/documentation.png" class="titleWithIcon"/>Documentation</h3>
  The JPPF documentation can be found <a href="/doc/v5">online</a>. You may also read it offline as <a href="/documents/JPPF-User-Guide.pdf">a PDF document</a>.
  <a name="license"></a>
  <h3><img src="images/icons/document-sign.png" class="titleWithIcon"/>License</h3>
  JPPF is released under the terms of the <a href="/license.php">Apachache v2.0</a> license.
  This <a href="http://www.opensource.org">OSI-approved</a> open source license is friendly to individuals, businesses, governments and academia, for commercial and non-commercial purposes.
  It does not restrict the use of JPPF with commercial and proprietary applications.
  <a name="contacts"></a>
  <h3><img src="images/icons/contact.png" class="titleWithIcon"/>Contacts</h3>
  For any press inquiry, please refer to our <a href="/contacts.php">contacts</a> page.
</div>
</div>
				</td>
				</tr>
			</table>
			<table border="0" cellspacing="0" cellpadding="0" width="100%" class="jppffooter">
      <tr><td colspan="*" style="height: 10px"></td></tr>
      <tr>
        <td align="center" style="font-size: 9pt; color: #6D78B6">
          <a href="http://sourceforge.net/donate/index.php?group_id=135654"><img src="http://images.sourceforge.net/images/project-support.jpg" width="88" height="32" border="0" alt="Support This Project" /></a>
        </td>
        <td align="center" style="font-size: 9pt; color: #6D78B6">Copyright &copy; 2005-2015 JPPF.org</td>
        <td align="center" valign="center">
          <!-- Google+ button -->
          <!--
          <div class="g-plusone" data-href="http://www.jppf.org" data-annotation="bubble" data-size="small" data-width="300"></div>
          <script type="text/javascript">
            (function() {
              var po = document.createElement('script'); po.type = 'text/javascript'; po.async = true;
              po.src = 'https://apis.google.com/js/platform.js';
              var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(po, s);
            })();
          </script>
          -->
          <!-- Twitter share button -->
          <a href="https://twitter.com/share" class="twitter-share-button" data-url="http://www.jppf.org" data-via="jppfgrid" data-count="horizontal" data-dnt="true">Tweet</a>
          <script>!function(d,s,id){var js,fjs=d.getElementsByTagName(s)[0],p=/^http:/.test(d.location)?'http':'https';if(!d.getElementById(id)){js=d.createElement(s);js.id=id;js.src=p+'://platform.twitter.com/widgets.js';fjs.parentNode.insertBefore(js,fjs);}}(document, 'script', 'twitter-wjs');</script>
          <!-- Facebook Like button -->
          <iframe src="http://www.facebook.com/plugins/like.php?href=http%3A%2F%2Fwww.jppf.org&amp;layout=button_count&amp;show_faces=true&amp;width=40&amp;action=like&amp;colorscheme=light&amp;height=20" scrolling="no" frameborder="0"
            class="like" allowTransparency="true"></iframe>
        </td>
        <td align="right">
          <a href="http://sourceforge.net/projects/jppf-project">
            <img src="http://sflogo.sourceforge.net/sflogo.php?group_id=135654&type=10" width="80" height="15" border="0"
              alt="Get JPPF at SourceForge.net. Fast, secure and Free Open Source software downloads"/>
          </a>
        </td>
        <td style="width: 10px"></td>
      </tr>
      <tr><td colspan="*" style="height: 10px"></td></tr>
    </table>
  <!--</div>-->
  <div style="background-color: #E2E4F0; width: 100%;"><img src="/images/frame_bottom.gif" border="0"/></div>
		</div>
		</div>
	</body>
</html>
