<?php $currentPage="Press" ?>
<?php $jppfVersion="6.0" ?>
<html lang="en" xml:lang="en" xmlns="http://www.w3.org/1999/xhtml">
	  <head>
    <title>JPPF Press Kit
</title>
    <meta name="description" content="The open source grid computing solution">
    <meta name="keywords" content="JPPF, java, parallel computing, distributed computing, grid computing, parallel, distributed, cluster, grid, cloud, open source, android, .net">
    <meta HTTP-EQUIV="Content-Type" content="text/html; charset=UTF-8">
    <link rel="shortcut icon" href="/images/jppf-icon.ico" type="image/x-icon">
    <link rel="stylesheet" type="text/css" href="/jppf.css" title="Style">
  </head>
	<body>
		<div align="center">
		<div class="gwrapper" align="center">
			<div style="display: none">JPPF, java, parallel computing, distributed computing, grid computing, parallel, distributed, cluster, grid, cloud, open source, android, .net</div>
    <?php
    if (!isset($currentPage)) {
      $currentPage = $_REQUEST["page"];
      if (($currentPage == NULL) || ($currentPage == "")) {
        $currentPage = "Home";
      }
    }
    if ($currentPage != "Forums") {
    ?>
    <div style="background-color: #E2E4F0">
      <div class="frame_top"/></div>
    </div>
    <?php
    }
    ?>
    <table width="100%" cellspacing="0" cellpadding="0" border="0" class="jppfheader" style="border-left: 1px solid #6D78B6; border-right: 1px solid #6D78B6">
      <tr style="height: 80px">
        <td width="15"></td>
        <td width="191" align="left" valign="center"><a href="/"><img src="/images/logo2.gif" border="0" alt="JPPF" style="box-shadow: 4px 4px 4px #6D78B6;"/></a></td>
        <td width="140" align="center" style="padding-left: 5px; padding-right: 5px"><h3 class="header_slogan">The open source<br>grid computing<br>solution</h3></td>
        <td width="80"></td>
        <td align="right">
          <table border="0" cellspacing="0" cellpadding="0" style="height: 30px; background-color:transparent;">
            <tr>
              <td style="width: 1px"></td>
              <?php $cl = (($currentPage == "Home") ? "headerMenuItem2" : "headerMenuItem") . " " . "header_item_start"; ?>
<td class="<?php echo $cl; ?>">&nbsp;<a href="/index.php" class="<?php echo $cl; ?>">Home</a>&nbsp;</td>
<td style="width: 1px"></td>
              <?php $cl = (($currentPage == "About") ? "headerMenuItem2" : "headerMenuItem") . " " . ""; ?>
<td class="<?php echo $cl; ?>">&nbsp;<a href="/about.php" class="<?php echo $cl; ?>">About</a>&nbsp;</td>
<td style="width: 1px"></td>
              <?php $cl = (($currentPage == "Features") ? "headerMenuItem2" : "headerMenuItem") . " " . ""; ?>
<td class="<?php echo $cl; ?>">&nbsp;<a href="/features.php" class="<?php echo $cl; ?>">Features</a>&nbsp;</td>
<td style="width: 1px"></td>
              <?php $cl = (($currentPage == "Download") ? "headerMenuItem2" : "headerMenuItem") . " " . ""; ?>
<td class="<?php echo $cl; ?>">&nbsp;<a href="/downloads.php" class="<?php echo $cl; ?>">Download</a>&nbsp;</td>
<td style="width: 1px"></td>
              <?php $cl = (($currentPage == "Documentation") ? "headerMenuItem2" : "headerMenuItem") . " " . ""; ?>
<td class="<?php echo $cl; ?>">&nbsp;<a href="/doc" class="<?php echo $cl; ?>">Documentation</a>&nbsp;</td>
<td style="width: 1px"></td>
              <?php $cl = (($currentPage == "Forums") ? "headerMenuItem2" : "headerMenuItem") . " " . "header_item_end"; ?>
<td class="<?php echo $cl; ?>">&nbsp;<a href="/forums" class="<?php echo $cl; ?>">Forums</a>&nbsp;</td>
<td style="width: 1px"></td>
            </tr>
          </table>
        </td>
        <td width="15"></td>
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
        <?php if ($currentPage == "Source code") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="https://github.com/jppf-grid/JPPF" class="<?php echo $itemClass; ?>">&raquo; Source code</a><br></div>
        <hr/>
                <?php if ($currentPage == "All docs") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/doc" class="<?php echo $itemClass; ?>">&raquo; All docs</a><br></div>
        <?php if (($currentPage == "v6.1 (alpha)") || ($currentPage == "v6.1")) $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/6.1" class="<?php echo $itemClass; ?>">v6.1 (alpha)</a><br></div>
        <?php if (($currentPage == "v6.0") || ($currentPage == "v6.0")) $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/6.0" class="<?php echo $itemClass; ?>">v6.0</a><br></div>
        <?php if (($currentPage == "v5.2") || ($currentPage == "v5.2")) $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/5.2" class="<?php echo $itemClass; ?>">v5.2</a><br></div>
        <?php if (($currentPage == "v5.1") || ($currentPage == "v5.1")) $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/5.1" class="<?php echo $itemClass; ?>">v5.1</a><br></div>
        <?php if ($currentPage == "All Javadoc") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/doc/#javadoc" class="<?php echo $itemClass; ?>">&raquo; All Javadoc</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/javadoc/6.1" class="<?php echo $itemClass; ?>">v6.1 (alpha)</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/javadoc/6.0" class="<?php echo $itemClass; ?>">v6.0</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/javadoc/5.2" class="<?php echo $itemClass; ?>">v5.2</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/javadoc/5.1" class="<?php echo $itemClass; ?>">v5.1</a><br></div>
        <?php if ($currentPage == "All .Net APIs") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/doc#csdoc" class="<?php echo $itemClass; ?>">&raquo; All .Net APIs</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/csdoc/6.1" class="<?php echo $itemClass; ?>">v6.1 (alpha)</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/csdoc/6.0" class="<?php echo $itemClass; ?>">v6.0</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/csdoc/5.2" class="<?php echo $itemClass; ?>">v5.2</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/csdoc/5.1" class="<?php echo $itemClass; ?>">v5.1</a><br></div>
        <hr/>
        <?php if ($currentPage == "Issue tracker") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/tracker/tbg" class="<?php echo $itemClass; ?>">&raquo; Issue tracker</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/1/search/1" class="<?php echo $itemClass; ?>">bugs</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/9/search/1" class="<?php echo $itemClass; ?>">features</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/2/search/1" class="<?php echo $itemClass; ?>">enhancements</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/8/search/1" class="<?php echo $itemClass; ?>">next version</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/22/search/1" class="<?php echo $itemClass; ?>">maintenance</a><br></div>
        <hr/>
        <?php if ($currentPage == "Press") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/press.php" class="<?php echo $itemClass; ?>">&raquo; Press</a><br></div>
        <?php if ($currentPage == "Release notes") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/release_notes.php?version=6.0" class="<?php echo $itemClass; ?>">&raquo; Release notes</a><br></div>
        <?php if ($currentPage == "Quotes") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/quotes.php" class="<?php echo $itemClass; ?>">&raquo; Quotes</a><br></div>
        <?php if ($currentPage == "Screenshots") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/screenshots" class="<?php echo $itemClass; ?>">&raquo; Screenshots</a><br></div>
        <?php if ($currentPage == "CI") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/ci.php" class="<?php echo $itemClass; ?>">&raquo; CI</a><br></div>
        <?php if ($currentPage == "News") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/news.php" class="<?php echo $itemClass; ?>">&raquo; News</a><br></div>
        <hr/>
        <?php if ($currentPage == "Contacts") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/contacts.php" class="<?php echo $itemClass; ?>">&raquo; Contacts</a><br></div>
        <?php if ($currentPage == "Services") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/services.php" class="<?php echo $itemClass; ?>">&raquo; Services</a><br></div>
        <br/>
				</div>
				<div class="jppf_content">
<div align="justify">
  <h1>JPPF Press Kit</h1>
  <div class="blockWithHighlightedTitle">
  <h3>Content</h3>
  <table>
    <tr>
      <td style="padding: 5 10 5 10">
        <a href="/release_notes.php?version=<?php echo $jppfVersion ?>">Text of the original release</a><br>
        <a href="#downloads">Where to download</a><br>
        <a href="#license">License</a><br>
      </td>
      <td style="padding: 5 10 5 10">
        <a href="#features">Features</a><br>
        <a href="#documentation">Documentation</a><br>
        <a href="#contacts">Contacts</a><br>
      </td>
    </tr>
  </table>
  <!--
  <div class="u_link" style="margin-left: 10px">
    <a href="/release_notes.php?version=<?php echo $jppfVersion ?>">Text of the original release</a><br>
    <a href="#features">Features</a><br>
    <a href="#downloads">Where to download</a><br>
    <a href="#documentation">Documentation</a><br>
    <a href="#license">License</a><br>
    <a href="#contacts">Contacts</a><br>
  </div>
  -->
  <br>
  </div>
  <br><div class="blockWithHighlightedTitle">
  <a name="original_release"></a>
<!-- ============================== -->
<!-- start version-specific content -->
<!-- ============================== -->
<h3 style="${style}"><img src="images/icons/news.png" class="titleWithIcon"/>Press release: JPPF <?php echo $jppfVersion ?></h3>
<p><a href="https://www.jppf.org/doc/6.0/index.php?title=Web_administration_console"><b>Web administration console</b></a>
<ul class="samplesList">
  <li>same functionality as the desktop console, except for the topology graph and the charts</li>
  <li>role-based authentication and authorization</li>
  <li>tested on major web servers: Tomcat, Jetty, JBoss, Open Liberty, Weblogic, Glassfish</li>
</ul>
<p><a href="https://www.jppf.org/doc/6.0/index.php?title=Database_services"><b>Database Services</b></a>
<ul class="samplesList">
  <li>easily create JDBC datasource definitions and propagate them throughout the grid</li>
  <li>reuse datasource definitions accross job submissions</li>
</ul>
<p><a href="https://www.jppf.org/doc/6.0/index.php?title=Jobs_persistence_in_the_driver"><b>Jobs Persistence</b></a>
<ul class="samplesList">
  <li>automatic job recovery in case of grid crashes</li>
  <li>"submit and forget" jobs, then check their completion state and retrieve their results from a separate client later on</li>
  <li>retrieve jobs on demand from a persistent store and resubmit them to completion if needed</li>
  <li>pluggable persistence mechanism</li>
  <li>built-in implementations: database persistence, file-based persistence, asynchronous (write-behind) wrapper and caching wrapper</li>
</ul>
<p><a href="https://github.com/jppf-grid/JPPF/tree/master/jmxremote-nio"><b>NIO-based JMX remote connector</b></a>
<ul class="samplesList">
  <li>a fast, scalable, NIO-based JMX remote connector that uses less system resources</li>
  <li>can be used as a standalone connector</li>
  <li>perfectly integrated with JPPF (no additional port required in the JPPF server)</li>
</ul>
<p><a href="https://www.jppf.org/doc/6.0/index.php?title=Monitoring_data_providers"><b>Pluggable monitoring data providers</b></a>
<ul class="samplesList">
  <li>plugins for any source of data for JVM/Process/SYstem health monitoring</li>
  <li>provided data is automatically and immediately available in the desktop and web administration consoles</li>
  <li>out-of-the-box health monitoring data is implemnted as a built-in monitoring data provider</li>
</ul>
<p><a href="https://www.jppf.org/doc/6.0/index.php?title=Load-balancer_state_persistence"><b>Persistence and reuse of load-balancers states</b></a>
<ul class="samplesList">
  <li>the state of the adaptive load-balancers can now be persisted, allowing the reuse of optimal parameters without the long convergence phase</li>
  <li>state persistence is available to client-side and server-side load-balancing</li>
  <li>pluggable persistence mechanism</li>
  <li>built-in implementations: database persistence, file-based persistence and asynchronous (write-behind) wrapper</li>
</ul>
<p><b>Server configuration now requires a single port for everything, including management and heartbeating</b></li>
<p><a href="https://www.jppf.org/doc/6.0/index.php?title=Execution_Policies#Execution_policy_arguments_as_expressions"><b>Complex expressions as arguments of execution policies</b></a>
<ul class="samplesList">
  <li>all arguments of execution policies can include scipts and properties substitutions</li>
  <li>comparisons are no longer limited to available properties and their value, complex expressions can be used instead</li>
</ul>
<p><a href="https://www.jppf.org/doc/6.0/index.php?title=Configuring_a_JPPF_server#Heartbeat-based_connection_failure_detection"><b>Revamping of the heartbeat mechanism for connection failure detection</b></a>
<ul class="samplesList">
  <li>heartbeat mechanism is now available for client-to-server and server-to-server connections</li>
  <li>configuration simplification: heartbeat only needs to be enabled on the remote peers</li>
</ul>
<p><b>Desktop administration console enhancements</b>
<ul class="samplesList">
  <li>pick lists are now used in the desktop console to select the visible columns of all tree views</li>
  <li>added a "select all nodes" button in the topology views and a "select all jobs" buttons in the jobs view</li>
  <li>load-balancing settings tab was moved to a popup dialog in the topology views</li>
  <li><a href="https://www.jppf.org/screenshots/gallery-images/Admin%20Console%20-%20Desktop/MasterSlave.png">master/slave nodes relationships</a> are now visible in the graph view of the topology</li>
</ul>
<p><b>Multi-server topologies improvements</b>
<ul class="samplesList">
  <li>Full-fledged NIO implementation</li>
  <li><a href="https://www.jppf.org/doc/6.0/index.php?title=Configuring_a_JPPF_server#Configuring_peer_connections_manually">Server to server connection pooling</a></li>
  <li><a href="https://www.jppf.org/doc/6.0/index.php?title=Configuring_a_JPPF_server#Peer_drivers_load-balancing_threshold">Load-balancing vs. failover threshold</a></li>
</ul>
<p><b>Configuration API improvements</b>
<ul class="samplesList">
  <li>localization support for the documentation of properties</li>
  <li><a href="https://www.jppf.org/doc/6.0/index.php?title=The_JPPF_configuration_API#Parametrized_properties">parametrized configuration properties</a></li>
  <li>deprecation support</li>
  <li>new sample "<a href="https://www.jppf.org/samples-pack/ConfigurationProperties">JPPF properties documentation generator</a>"</li>
</ul>
<p><b>Various enhancements</b>
<ul class="samplesList">
  <li>server statistics snapshots now have a last updated timestamp</li>
  <li>the new <a href="https://www.jppf.org/doc/6.0/index.php?title=The_Location_API#MavenCentralLocation">MavenCentralLocation</a> class allows downloading artifacts from Maven Central</li>
  <li><a href="https://www.jppf.org/doc/6.0/index.php?title=Defining_the_node_connection_strategy#Configuration-based_CSV_server_definitions">a new built-in node connection strategy</a> allows configuration with a single configuration property</li>
  <li><a href="https://www.jppf.org/doc/6.0/index.php?title=Deployment_on_Open_Liberty">port of the J2EE connector to Open Liberty</a></li>
  <li>the JPPF client code handling the connections state was simplified, resulting in improved robustness and performance</li>
  <li><a href="https://www.jppf.org/doc/6.0/index.php?title=Task_objects#Getting_information_on_the_node_executing_the_task">JPPF tasks can now access the node they execute on</a></li>
  <li>significant performance improvements, based on numerous profiling sessions and stress tests</li>
  <li>all APIs deprecated in 5.x were removed</li>
  <li>wherever applicable, setter and modifier methods were refactored to enable method call chaining, allowing a more fluent code style</li>
  <li>use ScriptDefinition objects wherever JSR 223 compliant scripts are used</li>
  <li>improvements to the handling of <a href="https://www.jppf.org/doc/6.0/index.php?title=Job_Service_Level_Agreement#Setting_a_class_path_onto_the_job">jobs classpath</a></li>
</ul>
<!-- ============================== -->
<!-- end version-specific content   -->
<!-- ============================== -->
  <br>
  </div>
  <div class="column_left" style="text-align: justify">
    <br><div class="blockWithHighlightedTitle">
    <a name="features"></a>
    <h3 style="${style}"><img src="images/icons/view-list.png" class="titleWithIcon"/>Features</h3>
    <div class="u_link" style="margin-left: 10px">
      <a href="release_notes.php?version=<?php echo $jppfVersion ?>">Release notes</a>: see everything that's new in JPPF <?php echo $jppfVersion ?><br>
      Our <a href="features.php">features page</a> provides a comprenhensive overview of what JPPF has to offer.<br>
    </div>
    <br>
    </div>
    <br>
    <br><div class="blockWithHighlightedTitle">
    <a name="license"></a>
    <h3 style="${style}"><img src="images/icons/document-sign.png" class="titleWithIcon"/>License</h3>
    <p>JPPF is released under the terms of the <a href="/license.php">Apachache v2.0</a> license.
    This <a href="http://www.opensource.org">OSI-approved</a> open source license is friendly to individuals, businesses, governments and academia, for commercial and non-commercial purposes.
    It does not restrict the use of JPPF with commercial and proprietary applications.
    <br>
    </div>
  </div>
  <div class="column_right" style="text-align: justify">
    <br><div class="blockWithHighlightedTitle">
    <a name="downloads"></a>
    <h3 style="${style}"><img src="images/icons/download.png" class="titleWithIcon"/>Downloads</h3>
    All files can be found from our <a href="/downloads.php">downloads page</a>.<br>
    <br>
    </div>
    <br><div class="blockWithHighlightedTitle">
    <a name="documentation"></a>
    <h3 style="${style}"><img src="images/icons/documentation.png" class="titleWithIcon"/>Documentation</h3>
    <p>The JPPF documentation can be found <a href="/doc/6.0">online</a>. You may also read it offline as <a href="/documents/JPPF-User-Guide.pdf">a PDF document</a>.
    <br>
    </div>
    <br><div class="blockWithHighlightedTitle">
    <a name="contacts"></a>
    <h3 style="${style}"><img src="images/icons/contact.png" class="titleWithIcon"/>Contacts</h3>
    <p>For any press inquiry, please refer to our <a href="/contacts.php">contacts</a> page.
    <br>
    </div>
    <br>
  </div>
</div>
</div>
				</td>
				</tr>
			</table>
			<table border="0" cellspacing="0" cellpadding="0" width="100%" class="jppffooter">
      <tr><td colspan="*" style="height: 10px"></td></tr>
      <tr>
        <td style="width: 10px"></td>
        <td align="left" style="font-size: 9pt; color: #6D78B6">
          <a href="/"><img src="/images/jppf_group_large.gif" border="0" alt="JPPF"/></a>
        </td>
        <td align="middle" valign="middle" style="font-size: 9pt; color: #6D78B6">Copyright &copy; 2005-2018 JPPF.org</td>
        <td align="right">
          <a href="https://sourceforge.net/projects/jppf-project">
            <img src="https://sflogo.sourceforge.net/sflogo.php?group_id=135654&type=10" width="80" height="15" border="0"
              alt="Get JPPF at SourceForge.net. Fast, secure and Free Open Source software downloads"/>
          </a>
        </td>
        <td style="width: 10px"></td>
      </tr>
      <tr><td colspan="*" style="height: 10px"></td></tr>
    </table>
  <!--</div>-->
  <div style="background-color: #E2E4F0">
    <div class="frame_bottom"/></div>
  </div>
		</div>
		</div>
	</body>
</html>
