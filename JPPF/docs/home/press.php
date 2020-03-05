<?php $currentPage="Press" ?>
<?php $jppfVersion="6.2" ?>
<html lang="en" xml:lang="en" xmlns="http://www.w3.org/1999/xhtml">
	  <head>
    <title>JPPF Press Kit
</title>
    <meta name="description" content="The open source grid computing solution">
    <meta name="keywords" content="JPPF, java, parallel computing, distributed computing, grid computing, parallel, distributed, cluster, grid, cloud, open source, android, .net, docker, kubernetes, helm">
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
              <?php $cl = (($currentPage == "On Github") ? "headerMenuItem2" : "headerMenuItem") . " " . ""; ?>
<td class="<?php echo $cl; ?>">&nbsp;<a href="https://github.com/jppf-grid/JPPF" class="<?php echo $cl; ?>">On Github</a>&nbsp;</td>
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
        <?php if ($currentPage == "On Github") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="https://github.com/jppf-grid/JPPF" class="<?php echo $itemClass; ?>">&raquo; On Github</a><br></div>
        <hr/>
                <?php if ($currentPage == "All docs") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/doc" class="<?php echo $itemClass; ?>">&raquo; All docs</a><br></div>
        <?php if (($currentPage == "v6.3 (alpha)") || ($currentPage == "v6.3")) $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/6.3" class="<?php echo $itemClass; ?>">v6.3 (alpha)</a><br></div>
        <?php if (($currentPage == "v6.2") || ($currentPage == "v6.2")) $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/6.2" class="<?php echo $itemClass; ?>">v6.2</a><br></div>
        <?php if (($currentPage == "v6.1") || ($currentPage == "v6.1")) $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/6.1" class="<?php echo $itemClass; ?>">v6.1</a><br></div>
        <?php if (($currentPage == "v6.0") || ($currentPage == "v6.0")) $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/6.0" class="<?php echo $itemClass; ?>">v6.0</a><br></div>
        <?php if ($currentPage == "All Javadoc") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/doc/#javadoc" class="<?php echo $itemClass; ?>">&raquo; All Javadoc</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/javadoc/6.3" class="<?php echo $itemClass; ?>">v6.3 (alpha)</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/javadoc/6.2" class="<?php echo $itemClass; ?>">v6.2</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/javadoc/6.1" class="<?php echo $itemClass; ?>">v6.1</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/javadoc/6.0" class="<?php echo $itemClass; ?>">v6.0</a><br></div>
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
<p><b>Tasks dependencies</b>
<ul class="press">
  <li>tasks within the same job can now <a href="https://www.jppf.org/doc/6.2/index.php?title=Task_objects#Dependencies_between_tasks">depend on each other</a></li>
  <li>JPPF guarantees that all dependencies of a task are executed before it can start</li>
  <li><a href="https://www.jppf.org/doc/6.2/index.php?title=Job_Service_Level_Agreement#Dependency_graph_traversal">traversal of the tasks graph</a> can be performed on either server or client side</li>
  <li>built-in detection and handling of dependency cycles</li>
</ul>
<p><b><a href="https://www.jppf.org/doc/6.2/index.php?title=JPPF_in_Docker_containers">JPPF in Docker containers and Kubernetes</a></b>
<ul class="press">
  <li>the JPPF drivers, nodes and web administration console are now available as Docker images, publicly <a href="https://hub.docker.com/u/jppfgrid">available on Docker Hub</a></li>
  <li>JPPF grids can be deployed on Kubernetes clusters using the <a href="https://github.com/jppf-grid/JPPF/tree/master/containers/k8s/jppf">JPPF Helm chart</a></li>
  <li>JPPF grids can also be deployed on Docker swarm clusters as <a href="https://github.com/jppf-grid/JPPF/tree/master/containers#jppf-service-stack">Docker service stacks</a></li>
</ul>
<p><b>Performance improvement in the client</b>
<p class="press">A new client-side cache of class definitions was implemented, to improve the overall dynamic class loading performance.
<p><b>Job dependencies and job graphs</b>
<ul class="press">
  <li>JPPF jobs can now be organized into <a href="https://www.jppf.org/doc/6.2/index.php?title=Job_dependencies_and_job_graphs">dependency graphs</a>, where a job can start only when its dependencies have completed</li>
  <li>new convenient and intuitive <a href="https://www.jppf.org/doc/6.2/index.php?title=Job_dependencies_and_job_graphs#Specifying_job_dependencies">APIs</a> to define the dependency graphs</li>
  <li>automatic detection and handling of <a href="https://www.jppf.org/doc/6.2/index.php?title=Job_dependencies_and_job_graphs#Cycles_in_the_job_dependency_graph">circular dependencies</a> (i.e. cycles)</li>
  <li>by default, cancellation of a job is <a href="https://www.jppf.org/doc/6.2/index.php?title=Job_dependencies_and_job_graphs#Job_cancellation_behavior">automatically cascaded</a> to dependent jobs. This can be overriden for each job</li>
  <li>built-in <a href="https://www.jppf.org/doc/6.2/index.php?title=Job_dependencies_and_job_graphs#Job_graphs_monitoring">management MBean</a> to monitor the state of the job dependency graph</li>
</ul>
<p><b>DSL for job selectors</b>
<p>The <a href="https://www.jppf.org/doc/6.2/index.php?title=Job_selectors">job selector API</a> was extended to include complex boolean expressions and comparison tests, similarly to execution policies
<ul class="press">
  <li><a href="https://www.jppf.org/doc/6.2/index.php?title=Job_selectors#Logical_job_selectors">boolean opearators</a> AND, OR, XOR, Negate</li>
  <li><a href="https://www.jppf.org/doc/6.2/index.php?title=Job_selectors#Binary_comparison_selectors">comparison operators</a> on Comparable values: more than, less than, at least, at most</li>
  <li><a href="https://www.jppf.org/doc/6.2/index.php?title=Job_selectors#Range_comparison_operators">range comparison operators</a> on Comparable values: between with inclusion or exclusion of upper and lower bounds</li>
  <li><a href="https://www.jppf.org/doc/6.2/index.php?title=Job_selectors#Contains.2C_one_of_and_regex_job_selectors">"contains", "one of" and "regex"</a> selectors</li>
  <li>job name selector</li>
  <li>already existing selectors: all jobs, job uuids, scripted and custom</li>
</ul>
<p><b>Use of java.time.* classes for JPPF schedules</b>
<p class="press"<a href="https://docs.oracle.com/javase/8/docs/api/java/time/package-summary.html">java.time.*</a> classes can now be used to build <a href="https://www.jppf.org/javadoc/6.2/index.html?org/jppf/scheduling/JPPFSchedule.html">JPPFSchedule</a> instances to specify jobs <a href="https://www.jppf.org/doc/6.2/index.php?title=Job_Service_Level_Agreement#Job_start_and_expiration_scheduling"/>start or expiration schedules</a>
<p><b>Inspection of the client-side jobs queue</b>
<p class="press"<a href="https://www.jppf.org/doc/6.2/index.php?title=The_JPPFClient_API#Inspecting_the_jobs_queue">New methods</a> were added to the client API to inspect the jobs queue
<p><b>Major refactoring of the management and monitoring MBeans</b>
<ul class="press">
  <li>the <a href="https://www.jppf.org/doc/6.2/index.php?title=Managing_and_monitoring_the_nodes_through_the_driver#The_node_forwarding_MBean">node forwarding MBean</a> now provides compile-time typing of the results</li>
  <li>all built-in MBeans are annotated with metadata available at runtime</li>
  <li>the same metadata is used to generate <a href="https://www.jppf.org/doc/6.2/index.php?title=Managing_and_monitoring_the_nodes_through_the_driver#Node_forwarding_static_proxies">static forwarding proxies</a> (and their unit tests) for the built-in node MBeans</li>
  <li>the MBeans metadata is also used to generate the built-in <a href="https://www.jppf.org/doc/6.2/index.php?title=MBeans_reference">MBeans reference documentation</a></li>
  <li>the <a href="https://www.jppf.org/doc/6.2/index.php?title=Management_and_monitoring">management and monitoring documentation</a> was refactored for greater clarity and readability</li>
</ul>
<p><b>Administration and monitoring console improvements</b>
<ul class="press">
  <li>numerous performance hotspots were identified and fixed, both in the monitoring API and in the UI rendering code</li>
  <li>for license compatibility reasons, the <a href="http://www.jfree.org/jfreechart/">JFreeChart</a> charting library was replaced with <a href="https://github.com/knowm/XChart">XCharts</a>, resulting in further performance improvements</li>
</ul>
<p><b>Performance improvements in the driver and JMX remote connector</b>
<p class="press">Several performance sinks were identified and fixed, in high stress scenarios where a very large number of job lifecyclle notifications were generated, causing high spikes in CPU and memory usage.
<br>
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
        <td align="middle" valign="middle" style="font-size: 9pt; color: #6D78B6">Copyright &copy; 2005-2020 JPPF.org</td>
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
