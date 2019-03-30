<?php $currentPage="Press" ?>
<?php $jppfVersion="6.1" ?>
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
            <tr class="row_shadow">
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
        <?php if (($currentPage == "v6.2 (alpha)") || ($currentPage == "v6.2-alpha")) $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/6.2" class="<?php echo $itemClass; ?>">v6.2 (alpha)</a><br></div>
        <?php if (($currentPage == "v6.1") || ($currentPage == "v6.1")) $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/6.1" class="<?php echo $itemClass; ?>">v6.1</a><br></div>
        <?php if (($currentPage == "v6.0") || ($currentPage == "v6.0")) $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/6.0" class="<?php echo $itemClass; ?>">v6.0</a><br></div>
        <?php if (($currentPage == "v5.2") || ($currentPage == "v5.2")) $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/5.2" class="<?php echo $itemClass; ?>">v5.2</a><br></div>
        <?php if ($currentPage == "All Javadoc") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/doc/#javadoc" class="<?php echo $itemClass; ?>">&raquo; All Javadoc</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/javadoc/6.2" class="<?php echo $itemClass; ?>">v6.2 (alpha)</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/javadoc/6.1" class="<?php echo $itemClass; ?>">v6.1</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/javadoc/6.0" class="<?php echo $itemClass; ?>">v6.0</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/javadoc/5.2" class="<?php echo $itemClass; ?>">v5.2</a><br></div>
        <?php if ($currentPage == "All .Net APIs") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/doc#csdoc" class="<?php echo $itemClass; ?>">&raquo; All .Net APIs</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/csdoc/6.0" class="<?php echo $itemClass; ?>">v6.0</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/csdoc/5.2" class="<?php echo $itemClass; ?>">v5.2</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/csdoc/5.1" class="<?php echo $itemClass; ?>">v5.1</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/csdoc/6.1" class="<?php echo $itemClass; ?>">v5.0</a><br></div>
        <hr/>
        <?php if ($currentPage == "Github issues") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="https://github.com/jppf-grid/JPPF/issues" class="<?php echo $itemClass; ?>">&raquo; Github issues</a><br></div>
        <?php if ($currentPage == "Issue tracker") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/tracker/tbg" class="<?php echo $itemClass; ?>">&raquo; Issue tracker</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/8/search/1" class="<?php echo $itemClass; ?>">next version</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/22/search/1" class="<?php echo $itemClass; ?>">maintenance</a><br></div>
        <hr/>
        <?php if ($currentPage == "Press") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/press.php" class="<?php echo $itemClass; ?>">&raquo; Press</a><br></div>
        <?php if ($currentPage == "Release notes") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/release_notes.php?version=6.1" class="<?php echo $itemClass; ?>">&raquo; Release notes</a><br></div>
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
  <p><span style="font-size: 12pt; font-weight: bold; color: #6D78B6">Content:</span> <a href="/release_notes.php?version=<?php echo $jppfVersion ?>">Text of the original release</a> -
  <a href="#downloads">Where to download</a> -
  <a href="#license">License</a> -
  <a href="#features">Features</a> -
  <a href="#documentation">Documentation</a> -
  <a href="#contacts">Contacts</a>
  <br>
  </div>
  <br><div class="blockWithHighlightedTitle">
  <a name="original_release"></a>
<!-- ============================== -->
<!-- start version-specific content -->
<!-- ============================== -->
<h2 style="${style}"><img src="images/icons/news.png" class="titleWithIcon"/>Press release: JPPF <?php echo $jppfVersion ?></h2>
<h3>Network communication and workload distribution</h3>
<p class="press"><b>Asynchronous communication between node and server</b>
<ul class="press">
  <li>nodes can now process any number of jobs concurrently</li>
  <li>this addresses the issue where some threads of a node could be idle, even when work was available </li>
  <li>overall performance gain due to I/O being performed in parallel with job processing</li>
  <li><a href="https://www.jppf.org/doc/6.1/index.php?title=Configuring_a_JPPF_server#Maximum_number_of_concurrent_jobs_per_node">configurable limit</a> on the number of jobs a node can process concurrently (unlimited by default)</li>
</ul>
<p class="press"><b>Client connection concurrency</b>
<ul class="press">
  <li>connections from a client to a driver can now handle an unlimited number of concurrent jobs</li>
  <li>in other words, multiple connections are no longer required to achieve job concurrency</li>
  <li>the concurrency level for all connections in a pool can be updated <a href="https://www.jppf.org/doc/6.1/index.php?title=Client_and_administration_console_configuration#Jobs_concurrency_2">statically</a> or <a href="https://www.jppf.org/doc/6.1/index.php?title=Connection_pools#The_JPPFConnectionPool_API">dynamically</a></li>
  <li>the jobs concurrentcy <a href="https://www.jppf.org/doc/6.1/index.php?title=Submitting_multiple_jobs_concurrently">documentation</a> and <a href="https://www.jppf.org/samples-pack/ConcurrentJobs">sample</a> were updated accordingly</li>
</ul>
<p class="press"><b>Pluggable node throttling mechanism</b>
<ul class="press">
  <li><a href="https://www.jppf.org/doc/6.1/index.php?title=Node_throttling">this mechanism</a> prevents resource (e.g. heap, cpu) exhaustion in the node, due to too many concurrent jobs.
  When a condition is reached, the node refuses new jobs. When the condition no longer applies, the node accepts jobs again.</li>
  <li>it can also be used to limit the workload based on any condition, for instance based on time windows or the status of external services</li>
  <li>A <a href="https://www.jppf.org/doc/6.1/index.php?title=Node_throttling#Built-in_implementation:_heap_usage-based_throttling">built-in implementation</a> provides throttling based on heap usage</li>
</ul>
<h3>Management and Monitoring</h3>
<p class="press"><b>JVM health monitoring enhancements</b>
<ul class="press">
  <li>new monitored data elements were added: peak threads count, total started threads count and JVM uptime</li>
  <li>custom <a href="doc/6.1/index.php?title=Monitoring_data_providers#Value_converters">value converters</a> can now be associated with each monitored datum, to enable customized rendering of their value</li>
</ul>
<p class="press"><b>Node provisioning notifications</b>
<ul class="press"><li>
The node provisioning service of each master node now <a href="doc/6.1/index.php?title=Node_management#Provisioning_notifications">emits JMX notifications</a> each time a slave node is started or stopped.
</li></ul>
<h3>Job and Client APIs</h3>
<p class="press"><b>Preference execution policy</b>
<ul class="press"><li>
The new <a href="doc/6.1/index.php?title=Job_Service_Level_Agreement#Preference_policy">preference policy</a> attribute, in the job SLA, defines an ordered set of execution policies, such that eligible channels
or nodes will be chosen from those that satisfy the policy with the foremost position.
</li></ul>
<p class="press"><b>New job SLA attributes</b>
<ul class="press">
  <li><a href="https://www.jppf.org/doc/6.1/index.php?title=Job_Service_Level_Agreement#Maximum_driver_depth">maximum driver depth</a>: in a multi server topology, an upper bound for how many drivers a job can be transfered to before
  being executed on a node (server-side SLA)</li>
  <li><a href="https://www.jppf.org/doc/6.1/index.php?title=Job_Service_Level_Agreement#Maximum_dispatch_size">maximum dispatch size</a>: the maximum number of tasks in a job that can be sent at once to a node (driver-side SLA) or to
  a driver (client-side SLA). This overrides the dipsatch size computed by the load-balancer</li>
  <li><a href="https://www.jppf.org/doc/6.1/index.php?title=Job_Service_Level_Agreement#Allowing_concurrent_dispatches_to_the_same_channel">allow multiple dispatches to the same node (driver-side SLA) or driver (client-side SLA)</a>: a flag to specifiy whether
  a job can be dispatched to the same node or driver multiple times at any given moment</li>
</ul>
<p class="press"><b>New convenience execution policies</b>
<ul class="press"><li>
New execution policies were added, to simplify policies with a cumbersome syntax.
As an example, <i>new IsMasterNode()</i> nicely replaces <i>new Equals("jppf.node.provisioning.master", true)</i>. See <a href="doc/6.1/index.php?title=Execution_Policy_Elements#IsMasterNode">IsMasterNode</a>,
<a href="doc/6.1/index.php?title=Execution_Policy_Elements#IsSlaveNode">IsSlaveNode</a>,
<a href="doc/6.1/index.php?title=Execution_Policy_Elements#IsLocalChannel">IsLocalChannel</a>,
<a href="doc/6.1/index.php?title=Execution_Policy_Elements#IsPeerDriver">IsPeerDriver</a>
</li></ul>
<p class="press"><b>New MavenLocation class</b>
<ul class="press"><li>
<a href="javadoc/6.1/index.html?org/jppf/location/MavenLocation.html">MavenLocation</a> is a <a href="doc/6.1/index.php?title=The_Location_API">Location</a> implementation which allows downloading artifacts from any maven repository.
Consequently, <a href="javadoc/6.1/index.html?org/jppf/location/MavenCentralLocation.html">MavenCentralLocation</a> is now a specialized subclass pointing to the Maven Central repository.
</li></ul>
<p class="press"><b>Deprecation of the "blocking" job attribute</b>
<ul class="press"><li>
A job should be <a href="doc/6.1/index.php?title=The_JPPFClient_API#Submitting_a_job">submittable</a> either synchronously or asynchronously, regardless of its state and at the user's choice at the time of submission.
To this effect, the job's "blocking" attribute was deprecated. So was the "<i>JPPFClient.submitJob()</i>" method, now replaced with "<i>JPPFClient.submit()</i>" and "<i>JPPFClient.submitAsync()</i>".
</li></ul>
<h3>Miscellaneous features and enhancements</h3>
<p class="press"><b>Embedded drivers and nodes</b>
<ul class="press">
  <li>it is now possible to start a driver and or a node <a href="https://www.jppf.org/doc/6.1/index.php?title=Embedded_driver_and_node">programmatically</a></li>
  <li>a driver and node embedded in the same JVM share common resources such as thread pools, JMX remote connector server, NIO acceptor (connections) server, etc.</li>
  <li>a new <a href="https://www.jppf.org/samples-pack/EmbeddedGrid">dedicated sample</a> was implemented</li>
</ul>
<p class="press"><b>Offline documentation (PDF)</b>
<ul class="press">
  <li>the source documents (.odt) were regrouped into a single document, in order to fix the broken cross-document links in the <a href="https://www.jppf.org/download/JPPF-User-Guide.pdf">produced PDF</a></li>
  <li>the font and background for code snippets were changed to improve readability</li>
</ul>
<h3>Feature removals</h3>
<p>Following the move of JPPF to Java 8, we deeply regret to announce the removal of two integration features:
<ul class="press">
  <li><b>.Net bridge</b>: the jni4net project, on which the .Net bridge is based, is no longer actively maintained and does not support some of the Java 8 language constructs</li>
  <li><b>Android port</b>: we no longer have the bandwidth to maintain the Android node integration. In particular: porting to a version of Android which supports Java 8 language features.</li>
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
    </div><br>
    </div>
    <br><div class="blockWithHighlightedTitle">
    <a name="downloads"></a>
    <h3 style="${style}"><img src="images/icons/download.png" class="titleWithIcon"/>Downloads</h3>
    All files can be found from our <a href="/downloads.php">downloads page</a>.<br>
    <br>
    </div>
    <br><div class="blockWithHighlightedTitle">
    <a name="contacts"></a>
    <h3 style="${style}"><img src="images/icons/contact.png" class="titleWithIcon"/>Contacts</h3>
    <p>For any press inquiry, please refer to our <a href="/contacts.php">contacts</a> page.
    <br>
    </div>
  </div>
  <div class="column_right" style="text-align: justify">
    <br><div class="blockWithHighlightedTitle">
    <a name="documentation"></a>
    <h3 style="${style}"><img src="images/icons/documentation.png" class="titleWithIcon"/>Documentation</h3>
    <p>The JPPF documentation can be found <a href="/doc/<?php echo $jppfVersion ?>">online</a>. You may also read it offline as <a href="/download/JPPF-User-Guide.pdf">a PDF document</a>.
    <br>
    </div><br>
    <br><div class="blockWithHighlightedTitle">
    <a name="license"></a>
    <h3 style="${style}"><img src="images/icons/document-sign.png" class="titleWithIcon"/>License</h3>
    <p>JPPF is released under the terms of the <a href="/license.php">Apachache v2.0</a> license.
    This <a href="http://www.opensource.org">OSI-approved</a> open source license is friendly to individuals, businesses, governments and academia, for commercial and non-commercial purposes.
    It does not restrict the use of JPPF with commercial and proprietary applications.
    <br>
    </div>
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
        <td align="middle" valign="middle" style="font-size: 9pt; color: #6D78B6">Copyright &copy; 2005-2019 JPPF.org</td>
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
