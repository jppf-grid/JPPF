<?php $currentPage="Press" ?>
<?php $jppfVersion="4.0" ?>
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
<td class="headerMenuItem2">&nbsp;<a href="/doc" class="headerMenuItem2">Documentation</a>&nbsp;</td>
<?php
}
else
{
?>
<td class="headerMenuItem">&nbsp;<a href="/doc" class="headerMenuItem">Documentation</a>&nbsp;</td>
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
        <?php if ($currentPage == "Documentation") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/doc/v4" class="<?php echo $itemClass; ?>">&raquo; Documentation</a><br></div>
        <?php if ($currentPage == "v4.x") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/v4" class="<?php echo $itemClass; ?>">v4.x</a><br></div>
        <?php if ($currentPage == "v3.x") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/v3" class="<?php echo $itemClass; ?>">v3.x</a><br></div>
        <?php if ($currentPage == "v2.x") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/v2" class="<?php echo $itemClass; ?>">v2.x</a><br></div>
        <?php if ($currentPage == "Javadoc") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/api" class="<?php echo $itemClass; ?>">&raquo; Javadoc</a><br></div>
        <?php if ($currentPage == "v4.x") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/api" class="<?php echo $itemClass; ?>">v4.x</a><br></div>
        <?php if ($currentPage == "v3.x") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/api-3" class="<?php echo $itemClass; ?>">v3.x</a><br></div>
        <?php if ($currentPage == "v2.x") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/api-2.0" class="<?php echo $itemClass; ?>">v2.x</a><br></div>
        <hr/>
        <?php if ($currentPage == "Issue tracker") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/tracker/tbg" class="<?php echo $itemClass; ?>">&raquo; Issue tracker</a><br></div>
        <?php if ($currentPage == "bugs") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/1/search/1" class="<?php echo $itemClass; ?>">bugs</a><br></div>
        <?php if ($currentPage == "features") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/9/search/1" class="<?php echo $itemClass; ?>">features</a><br></div>
        <?php if ($currentPage == "enhancements") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/2/search/1" class="<?php echo $itemClass; ?>">enhancements</a><br></div>
        <?php if ($currentPage == "current work") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/8/search/1" class="<?php echo $itemClass; ?>">current work</a><br></div>
        <hr/>
        <?php if ($currentPage == "Press") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/press.php" class="<?php echo $itemClass; ?>">&raquo; Press</a><br></div>
        <?php if ($currentPage == "Release notes") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/release_notes.php?version=4.0" class="<?php echo $itemClass; ?>">&raquo; Release notes</a><br></div>
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
<h3>Press release: JPPF <?php echo $jppfVersion ?></h3>
<p><b>Support for Java 7 and later</b>: As of this release, JPPF will stop supporting Java 6 and will only run on Java 7 and later.<br/>
<span style="font-style: italic">Application code written and compiled with Java 6 will still run as is</span>.
<p><b>Full support for volunteer computing</b>: "JPPF@home" is now a reality. Build your volunteer computing project with all the benefits of a JPPF Grid and the underlying Java technologies.
This is made possible with the addition of new capabilities which can be combined or used individually and enhance the scalability and reliability of JPPF grids:
<ul class="samplesList">
  <li><a href="/doc/v4/index.php?title=Deployment_and_run_modes#Offline_nodes">Offline nodes</a> work disconnected from the grid and only connect to get more work</li>
  <li>New <a href="/doc/v4/index.php?title=Deployment_and_run_modes#Avoiding_stuck_jobs">fault-tolerance capabilities</a> handle cases when a node fails to return results</li>
  <li><a href="/doc/v4/index.php?title=JPPF_node_screensaver">Customizable screen saver</a> associated with each node, with entry points for receiving feedback from the tasks and jobs at any point of their life cycle.
  JPPF also includes a full-fledged, highly personalizable, <a href="/doc/v4/index.php?title=JPPF_node_screensaver#JPPF_built-in_screensaver">default animated screen saver</a></li>
  <li>The ability to <a href="/doc/v4/index.php?title=Job_Service_Level_Agreement#Setting_a_class_path_onto_the_job">transport Java libraries along with the jobs</a> enables the use of JPPF nodes as universal volunteer computing clients, enabling them to run multiple projects with a single installation</li>
</ul>
<p><b>Support for dynamic scripting</b>: JPPF 4.0 leverages the <a href="https://www.jcp.org/en/jsr/detail?id=223">JSR 223 specification</a> and corresponding <a href="http://docs.oracle.com/javase/7/docs/api/javax/script/package-summary.html">javax.script</a> APIs to enable dynamic scripting and raise its capabilities to a new level:
<ul class="samplesList">
  <li><a href="/doc/v4/index.php?title=Task_objects#Executing_dynamic_scripts:_ScriptedTask">scripted tasks</a> allow you to execute tasks entirely written in any JSR 223-compliant dynamic script language</a></li>
  <li><a href="/doc/v4/index.php?title=Execution_Policies#Scripted_policies">scripted execution policies</a> make node and server channel filtering easier and more powerful than ever</li>
</ul>
<p><b>Management console enhancements</b>:
<ul class="samplesList">
  <li>Every tab in the administration console can be displayed <a href="/screenshots.php?screenshot=Docking-3.gif&shotTitle=Docking%203">in a separate view</a></li>
  <li>New statistics were added to the <a href="/screenshots.php?screenshot=ServerStats-01.gif&shotTitle=Server%20Stats%2001">server statistics view</a>: class loading requests from the node and to the client, inbound and outbound network traffic to and from the nodes and clients</li>
  <li>Statistics can now be exported to the clipboard from the <a href="/screenshots.php?screenshot=ServerStats-01.gif&shotTitle=Server%20Stats%2001">server statistics view</a>, as either CSV or formatted plain text</li>
  <li>The statistics view now uses a flow layout for a better usability and user experience</li>
</ul>
<p><b>Revamped task API</b>:
<ul class="samplesList">
  <li>The base class for JPPF tasks was genericized and expanded into an interface/abstract class pattern. This results in <a href="/doc/v4/index.php?title=API_changes_in_JPPF_4.0">documented API changes</a>.
  A best effort was made to keep backward compatibility with JPPF 3.3, with a single and rare exception. The vast majority of existing applications will still run with the old deprecated APIs</li>
  <li>Exception handling: the <code>getException()</code> / <code>setException()</code> methods have been deprecated in favor of the more generic <code>getThrowable()</code> / <code>setThrowable()</code></li>
  <li>Tasks now have the native <a href="/doc/v4/index.php?title=Task_objects#Sending_notifications_from_a_task">ability to send notifications</a> to locally registered listeners, remote JMX listeners, or both</li>
</ul>
<p><b>Configuration</b>:
<ul class="samplesList">
  <li>The <a href="/doc/v4/index.php?title=Configuration_file_specification_and_lookup">configuration plugin API</a> was extended to enable reading the configuration from character streams (Java readers) in addition to byte streams</li>
  <li>Configuration sources can now <a href="/doc/v4/index.php?title=Includes_in_the_configuration">include other sources</a> at any level of nesting, to enhance the readability, modularity and maintenance of JPPF configuration files</li>
</ul>
<p><b>J2EE connector enhancements</b>:
<ul class="samplesList">
  <li>The J2EE connector client API was refactored to use the same code base as the standalone client</li>
  <li>The JPPF configuration can now be <a href="/doc/v4/index.php?title=How_to_use_the_connector_API#Reset_of_the_JPPF_client">updated dynamically without restarting the application server</a></li>
</ul>
<p><b>New and enhanced extension points</b>:
<ul class="samplesList">
  <li>A new <a href="/doc/v4/index.php?title=Specifying_alternate_serialization_schemes">serialization scheme API</a> was implemented, to enable integration with a broader range of serialization frameworks, while keeping backward compatibility with older serialization schemes.
  A <a href="/samples-pack/KryoSerializer/Readme.php">new sample</a> illustrates how the integration with the <a href="https://github.com/EsotericSoftware/kryo">Kryo</a> framework can result in a major performance improvement.</li>
  <li><a href="/doc/v4/index.php?title=Receiving_notifications_from_the_tasks">Listeners to task notifications</a> can now be registered with the nodes via the service provider interface</li>
  <li>A <a href="/doc/v4/index.php?title=JPPF_node_screensaver">screen saver</a> can now be associated with a node, for use in a volunteer computing model, to add meaningful animated graphics, or even just for fun</li>
</ul>
<p><b>New and enhanced samples</b>:
<ul class="samplesList">
  <li>The <a href="/samples-pack/Fractals/Readme.php">fractals generation sample</a> was enhanced to enable recording, replaying, saving and loading sets of points in the Mandelbrot space. This enables creating slide-shows of Mandlebrot images with just a few clicks.</li>
  <li>The new <a href="/samples-pack/FractalMovieGenerator/Readme.php">Mandelbrot.Movie@home</a> sample produces a full-fledged node distribution that is ready to install in a volunteer computing grid.
  The sample generates movies based on record sets produced by the <a href="/samples-pack/Fractals/Readme.php">fractals generation sample</a></li>
  <li>The <a href="/samples-pack/WordCount/Readme.php">Wikipedia word count sample</a> illustrates how JPPF can tackle big data and job streaming</li>
  <li>The new <a href="/samples-pack/KryoSerializer/Readme.php">Kryo serialization sample</a> demonstrates how to replace the default Java serialization with <a href="https://github.com/EsotericSoftware/kryo">Kryo</a></li>
</ul>
<p><b>Automated testing coverage</b>: Automated testing is a vital part of the JPPF development life cycle. Our automated testing framework creates small, but real, JPPF grids on the spot and uses the JPPF documented APIs to execute test cases based on JUnit.
<ul class="samplesList">
  <li>The range of automated test cases was broadened to include all major features, and most minor ones</li>
  <li>Various grid topologies are now included on demand in the tests, incuding single servers or multiple servers in P2P, offline nodes, etc., with and without SSL/TLS communication</li>
  <li>The J2EE connector is now automatically tested using scripts which download and install the application server, deploy the connector and test application, execute the JUnit-based tests and report the results</li>
</ul>
<a name="features"></a>
  <h3>Features</h3>
  <div class="u_link" style="margin-left: 10px">
    <a href="release_notes.php?version=<?php echo $jppfVersion ?>">Release notes</a>: see everything that's new in JPPF <?php echo $jppfVersion ?><br>
    <a href="features.php">Full features list</a><br>
  </div>
  <a name="downloads"></a>
  <h3>Downloads</h3>
  All files can be found from our <a href="/downloads.php">downloads page</a>.<br>
  <!--
  A <a href="/download/jppf_ws.jnlp">web installer</a> allows you to select and download only the specific modules you want to install (requires Java Web Start 1.5 or later).
  -->
  <a name="documentation"></a>
  <h3>Documentation</h3>
  The JPPF documentation can be found <a href="/doc/v4">online</a>. You may also read it offline as <a href="/documents/JPPF-User-Guide.pdf">a PDF document</a>.
  <a name="license"></a>
  <h3>License</h3>
  JPPF is released under the terms of the <a href="/license.php">Apachache v2.0</a> license.
  This <a href="http://www.opensource.org">OSI-approved</a> open source license is friendly to individuals, businesses, governments and academia, for commercial and non-commercial purposes.
  It does not restrict the use of JPPF with commercial and proprietary applications.
  <a name="contacts"></a>
  <h3>Contacts</h3>
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
				<td align="center" style="font-size: 9pt; color: #6D78B6">Copyright &copy; 2005-2013 JPPF.org</td>
				<td align="right">
					<a href="http://www.parallel-matters.com"><img src="/images/pm_logo_tiny.jpg" border="0" alt="Powered by Parallel Matters" /></a>&nbsp;
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
