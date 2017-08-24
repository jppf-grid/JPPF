<?php $currentPage="Features" ?>
<html lang="en" xml:lang="en" xmlns="http://www.w3.org/1999/xhtml">
	  <head>
    <title>JPPF Features
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
        <td width="130" align="center"><h3 class="header_slogan">The open source<br>grid computing<br>solution</h3></td>
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
        <hr/>
                <?php if ($currentPage == "All docs") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/doc" class="<?php echo $itemClass; ?>">&raquo; All docs</a><br></div>
        <?php if ($currentPage == "v5.2") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/5.2" class="<?php echo $itemClass; ?>">v5.2</a><br></div>
        <?php if ($currentPage == "v5.1") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/5.1" class="<?php echo $itemClass; ?>">v5.1</a><br></div>
        <?php if ($currentPage == "v4.2") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/4.2" class="<?php echo $itemClass; ?>">v4.2</a><br></div>
        <?php if ($currentPage == "All Javadoc") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/doc/#javadoc" class="<?php echo $itemClass; ?>">&raquo; All Javadoc</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/javadoc/5.2" class="<?php echo $itemClass; ?>">v5.2</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/javadoc/5.1" class="<?php echo $itemClass; ?>">v5.1</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/javadoc/5.0" class="<?php echo $itemClass; ?>">v5.0</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/javadoc/4.2" class="<?php echo $itemClass; ?>">v4.2</a><br></div>
        <?php if ($currentPage == "All .Net APIs") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/doc#csdoc" class="<?php echo $itemClass; ?>">&raquo; All .Net APIs</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/csdoc/5.2" class="<?php echo $itemClass; ?>">v5.2</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/csdoc/5.1" class="<?php echo $itemClass; ?>">v5.1</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/csdoc/5.0" class="<?php echo $itemClass; ?>">v5.0</a><br></div>
        <hr/>
        <?php if ($currentPage == "Issue tracker") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/tracker/tbg" class="<?php echo $itemClass; ?>">&raquo; Issue tracker</a><br></div>
        <?php if ($currentPage == "bugs") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/1/search/1" class="<?php echo $itemClass; ?>">bugs</a><br></div>
        <?php if ($currentPage == "features") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/9/search/1" class="<?php echo $itemClass; ?>">features</a><br></div>
        <?php if ($currentPage == "enhancements") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/2/search/1" class="<?php echo $itemClass; ?>">enhancements</a><br></div>
        <?php if ($currentPage == "next version") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/8/search/1" class="<?php echo $itemClass; ?>">next version</a><br></div>
        <?php if ($currentPage == "maintenance") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/22/search/1" class="<?php echo $itemClass; ?>">maintenance</a><br></div>
        <hr/>
        <?php if ($currentPage == "Press") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/press.php" class="<?php echo $itemClass; ?>">&raquo; Press</a><br></div>
        <?php if ($currentPage == "Release notes") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/release_notes.php?version=5.2" class="<?php echo $itemClass; ?>">&raquo; Release notes</a><br></div>
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
  <h1 align="center">JPPF Features</h1>
  <div class="column_left" style="text-align: justify">
    <div class="blockWithHighlightedTitle">
    <a name="feat01"></a>
    <h4><img src="images/icons/easy.png" class="titleWithIcon"/>Ease of use</h4>
    <p><a href="/doc/5.1/index.php?title=Introduction">Installing</a> a JPPF grid is as easy as running the web installer or un-zipping a few files.
    Launch as many nodes and servers as needed and get immediately ready to write your first JPPF application.
    The <a href="/doc/5.1/index.php?title=Development_guide">APIs</a> are easy to learn, yet very powerful, flexible and semantically consistent, and will allow you to get started in no time.
    <br></div>
    <br><div class="blockWithHighlightedTitle">
    <a name="feat02"></a>
    <h4><img src="images/icons/topology.png" class="titleWithIcon"/>Dynamic flexible topology</h4>
    <p>From master/worker to P2P, with anything in between, JPPF allows <a href="/doc/5.1/index.php?title=JPPF_Overview#Architecture_and_topology">any topology</a> that will suit your requirements.
    Furthermore, the topology is not static and can grow or shrink dynamically and on-demand, with a unique ability to adapt to any workload.
    Easily build any level of redundancy, avoid single points of failure and ensure the best performance and throughput for your needs.
    <br></div>
    <br><div class="blockWithHighlightedTitle">
    <a name="feat03"></a>
    <h4><img src="images/icons/preferences-desktop-4.png" class="titleWithIcon"/>Fault tolerance, self-repair and recovery</h4>
    <p>With built-in failure detection and fault tolerance mechanisms at all levels, a JPPF grid can survive massive failures in the topology, whatever the cause.
    From job requeuing to nodes rerouting, down to the ultimate failover to local execution - and even that has its own crash recovery capability -, JPPF ensures that the job is done even in extreme degradation conditions.
    <br></div>
    <br><div class="blockWithHighlightedTitle">
    <a name="feat04"></a>
    <h4><img src="images/icons/job-node2.png" class="titleWithIcon"/>Matching the Workload with the Grid</h4>
    <p>The <a href="/doc/5.1/index.php?title=Job_Service_Level_Agreement">right tools</a> at the right time for the job. Ensure that jobs are executed where they are supposed to, without interfering with each other and in the best conditions.
    Fine-grained node filtering, job prioritization and scheduling, grid partitioning and many other features provide a dynamic way of matching heterogenous workloads to the grid's capacity.
    <br></div>
    <br><div class="blockWithHighlightedTitle">
    <a name="feat05"></a>
    <h4><img src="images/icons/no-deployment.png" class="titleWithIcon"/>No deployment</h4>
    <p>The built-in networked and <a href="/doc/5.1/index.php?title=Class_loading_in_JPPF">distributed class loader</a> transparently ensures that the nodes can download the Java code for your application from where it is running.
    New or changed code is automatically reloaded into the nodes without any deployment hassle.
    Not only is tweaking and tinkering with the code no longer a source of time-consuming problems, it is actively facilitated and encouraged.
    <br></div>
  </div>
  <div class="column_right" style="text-align: justify">
    <div class="blockWithHighlightedTitle">
    <a name="feat06"></a>
    <h4><img src="images/icons/security.png" class="titleWithIcon"/>Security</h4>
    <p>Communications between components of a JPPF grid support <a href="/doc/5.1/index.php?title=Configuring_SSL/TLS_communications">SSL/TLS</a> encrytpion and authentication all the way. Certificate-based authentication, 1 way or mutual, is fully supported.
    Additional extension points allow you to further <a href="/doc/5.1/index.php?title=Transforming_and_encrypting_networked_data">transform any data</a> that is transported over network connections: tunnel grid data within your own protocol, use an additional encryption layer, or any data transformation which can be of use.
    <br></div>
    <br><div class="blockWithHighlightedTitle">
    <a name="feat07"></a>
    <h4><img src="images/icons/monitoring.png" class="titleWithIcon"/>Administration and monitoring</h4>
    <p>The JPPF graphical <a href="/screenshots/gallery-images/Topology-TreeView.gif">administration console</a>, along with the public <a href="http://www.jppf.org/doc/5.1/index.php?title=Management_and_monitoring">API</a> it is based on,
    enable remote monitoring and management of the grid <a href="/screenshots/gallery-images/GraphView.gif">topology</a>, <a href="/screenshots/gallery-images/JobPriority.gif">jobs</a> life cycle,
    servers and nodes <a href="/screenshots/gallery-images/RuntimeMonitoring.gif">health</a>, <a href="/screenshots/gallery-images/LoadBalancerSettings.gif">load-balancing</a> configuration,
    server <a href="/screenshots/gallery-images/ServerStats-01.gif">statistics</a>, and a lot more.
    The console also provides the ability to <a href="/screenshots/gallery-images/ChartsConfiguration-01.gif">define</a> your own dynamic <a href="/screenshots/gallery-images/Charts-01.gif">charts</a> based on dozens of dynamically updated fields you can chose from.
    <br></div>
    <br><div class="blockWithHighlightedTitle">
    <a name="feat08"></a>
    <h4><img src="images/icons/load-balancing.png" class="titleWithIcon"/>Load balancing</h4>
    <p>Multiple built-in <a href="/doc/5.2/index.php?title=Load_Balancing">load-balancing</a> algorithms are available at client and server levels, to enable an optimal distribution of the workload over the entire grid topology.
    Load balancing can be statically defined, adaptive based on the the topology and jobs requirements or even user-defined thanks to the <a href="/doc/5.1/index.php?title=Creating_a_custom_load-balancer">dedicated extension point</a>.
    <br></div>
    <br><div class="blockWithHighlightedTitle">
    <a name="feat09"></a>
    <h4><img src="images/icons/integration.png" class="titleWithIcon"/>Android, .Net and J2EE integration</h4>
    <p>
    Specialized client and node implementations bring JPPF grids to the <a href="/doc/5.1/index.php?title=Android_Node">Android</a>, <a href="/doc/5.1/index.php?title=.Net_Bridge">.Net</a> and <a href="/doc/5.1/index.php?title=J2EE_Connector">J2EE</a> worlds.
    Open up your grid implementation to the world of Android mobile devices.
    Write your jobs in any .Net language and execute them on .Net-enabled JPPF nodes.
    Use JPPF services from JEE enterprise applications or wrap them as Web or REST services. Make interoperability an operational reality.
    <br></div>
    <br><div class="blockWithHighlightedTitle">
    <a name="feat10"></a>
    <h4><img src="images/icons/personal.png" class="titleWithIcon"/>Customization</h4>
    <p>Whenever your specialized needs go beyond what is available out-of-the-box, JPPF provides many <a href="/doc/5.1/index.php?title=Extending_and_Customizing_JPPF">extension points, addons and plugins</a> which allow you to tailor, extend and customize the behavior of any of its components.
    Make JPPF your grid computing solution, without ever being stuck because of missing features.
    <br></div><br>
  </div>
</div>
				</td>
				</tr>
			</table>
			<table border="0" cellspacing="0" cellpadding="0" width="100%" class="jppffooter">
      <tr><td colspan="*" style="height: 10px"></td></tr>
      <tr>
        <td align="center" style="font-size: 9pt; color: #6D78B6">
          <a href="http://www.jppf.org"><img src="/images/jppf_group_large.gif" border="0" alt="JPPF"/></a>
        </td>
        <td align="middle" valign="middle" style="font-size: 9pt; color: #6D78B6">Copyright &copy; 2005-2017 JPPF.org</td>
        <td align="middle" valign="center">
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
  <div style="background-color: #E2E4F0">
  <div class="frame_bottom"/></div>
  </div>
		</div>
		</div>
	</body>
</html>
