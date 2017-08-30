<?php $currentPage="Press" ?>
<?php $jppfVersion="5.2" ?>
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
<h3><img src="images/icons/news.png" class="titleWithIcon"/>Press release: JPPF <?php echo $jppfVersion ?></h3>
<p><b>Administration console:</b>
<ul class="samplesList">
  <li><a href="http://www.jppf.org/screenshots/gallery-images/NodeFiltering-Active.gif">node filtering</a> with an execution policy editor with import/export capabilities</li>
  <li>ability to <a href="http://www.jppf.org/screenshots/gallery-images/VisibleServerStatistics.png">select the visible statistics</a> in the server statiscs view</li>
  <li>syntax hihghlighting in all the editors: properties/node filtering</li>
  <li>the admin console splash screen is now <a href="http://www.jppf.org/doc/5.2/index.php?title=Client_and_administration_console_configuration#Customizing_the_administration_console.27s_splash_screen">customizable</a> via the configuration</li>
  <li>The administration console is now fully localized, with full <a href="http://www.jppf.org/screenshots/gallery-images/UpdateNodeConfiguration.gif">English</a> and <a href="http://www.jppf.org/screenshots/gallery-images/UpdateNodeConfigurationFrench.gif">French</a> translations available
</ul>
<p><b>Configuration:</b>
<ul class="samplesList">
  <li>all documented properties are now defined as <a href="http://www.jppf.org/javadoc/5.2/index.html?org/jppf/utils/configuration/JPPFProperties.html">constants</a></li>
  <li>a new and elegant <a href="http://www.jppf.org/doc/5.2/index.php?title=The_JPPF_configuration_API#Predefined_JPPF_properties">API</a> was created to handle them easily</li>
  <li>it is now possible to specify in the configuration which JVM to use for the nodes and servers. This also applies to master and slave nodes when they are (re)started</li>
</ul>
<p><b>Customization/extension:</b>
<ul class="samplesList">
  <li>ability to <a href="http://www.jppf.org/doc/5.2/index.php?title=Composite_serialization">chain serialization schemes</a> to provide compression or encryption over actual serialization</li>
  <li><a href="http://www.jppf.org/doc/5.2/index.php?title=Specifying_alternate_serialization_schemes#Generic_JPPF_serialization">the JPPF serialization</a> scheme was thouroughly optimized and is now faster than the Java serialization</li>
  <li>it is now possible to register for <a href="http://www.jppf.org/doc/5.2/index.php?title=Receiving_server_statistics_events">statistics change events</a> in the server</li>
  <li><a href="http://www.jppf.org/doc/5.2/index.php?title=Network_interceptors">Network communication interceptors</a> enable user-defined code to be executed on both sides of each new connection</li>
  <li>A <a href="http://www.jppf.org/doc/5.2/index.php?title=Pluggable_MBeanServerForwarder">pluggable MBeanServerForwarder</a> can now be associated to the JMX remote connector servers created by JPPF drivers and nodes</li>
  <li><a href="http://www.jppf.org/doc/5.2/index.php?title=Environment_providers_for_JMX_remote_connections">Pluggable environment providers</a> for JMX remote connector clients and servers</li>
</ul>
<p><b>Android node:</b>
<ul class="samplesList">
  <li>It is now possible to configure the node to stop working or terminate <a href="http://www.jppf.org/doc/5.2/index.php?title=Android_Node#Battery_state_monitoring">when the device's battery is low</a></li></li>
  <li>Improved the <a href="http://www.jppf.org/screenshots/gallery-images/AndroidMainScreenBusy.gif">default feedback screen</a></li>
</ul>
<p><b>Job SLA:</b>
<ul class="samplesList">
  <li>The job SLA can now specifiy <a href="http://www.jppf.org/doc/5.2/index.php?title=Job_Service_Level_Agreement#Grid_policy">filtering rules</a> based on the server properties and the number of nodes satisfying one or more conditions<br>
  Example: "execute when the server has at least 2 GB of heap memory and at least 3 nodes with more than 4 cores each"</li>
  <li>The job SLA can <a href="http://www.jppf.org/doc/5.2/index.php?title=Job_Service_Level_Agreement#Specifying_the_desired_node_configuration">specify the desired configuration</a>
  of the nodes on which it will execute and force the nodes to reconfigure themselves accordingly</li>
  <li>execution policies based on server properties now have <a href="http://www.jppf.org/doc/5.2/index.php?title=Execution_policy_properties#Server_statistics">access to the server statistics</a>
</ul>
<p><b>Management and Monitoring</b>
<p>Two new types of node selectors are now available: <a href="http://www.jppf.org/doc/5.2/index.php?title=Nodes_management_and_monitoring_via_the_driver#Scripted_node_selector">scripted node selector</a> and <a href="http://www.jppf.org/doc/5.2/index.php?title=Nodes_management_and_monitoring_via_the_driver#Custom_node_selector">custom node selector</a>
<p><b>Load-balancing</b>
<p>A <a href="http://www.jppf.org/doc/5.2/index.php?title=Built-in_algorithms#.22rl2.22">new load-balancing algorithm</a>, named "rl2", was implemented
<p><b>Documentation</b>
<p>Added a complete section on <a href="http://www.jppf.org/doc/5.2/index.php?title=Load_Balancing">load balancing</a>
<p><b>Samples</b>
<ul class="samplesList">
  <li>A new sample was added, illustrating a full-fledged management of <a href="http://www.jppf.org/samples-pack/JobDependencies">dependencies between jobs</a></li>
  <li>The <a href="http://www.jppf.org/samples-pack/NetworkInterceptor">Network Interceptor sample</a> shows how a network connection interceptor can be used to implement a simple authentication mechanism with symetric encryption</li>
</ul>
<p><b>Packaging</b>
<p>The JPPF jar files now include the version number in their name, e.g. jppf-common-5.2.jar
<p><b>Continuous Integration</b>
<ul class="samplesList">
  <li>A large amount of time and effort was invested in setting up a continuous integration environment based on Jenkins</li>
  <li>Automated builds are now in place with <a href="http://www.jppf.org/ci.php">results</a> automatically published to the JPPF web site</li>
  <li>Automated tests coverage was largely improved</li>
</ul>
<!-- ============================== -->
<!-- end version-specific content   -->
<!-- ============================== -->
  <br>
  </div>
  <div class="column_left" style="text-align: justify">
    <br><div class="blockWithHighlightedTitle">
    <a name="features"></a>
    <h3><img src="images/icons/view-list.png" class="titleWithIcon"/>Features</h3>
    <div class="u_link" style="margin-left: 10px">
      <a href="release_notes.php?version=<?php echo $jppfVersion ?>">Release notes</a>: see everything that's new in JPPF <?php echo $jppfVersion ?><br>
      Our <a href="features.php">features page</a> provides a comprenhensive overview of what JPPF has to offer.<br>
    </div>
    <br>
    </div>
    <br>
    <br><div class="blockWithHighlightedTitle">
    <a name="license"></a>
    <h3><img src="images/icons/document-sign.png" class="titleWithIcon"/>License</h3>
    <p>JPPF is released under the terms of the <a href="/license.php">Apachache v2.0</a> license.
    This <a href="http://www.opensource.org">OSI-approved</a> open source license is friendly to individuals, businesses, governments and academia, for commercial and non-commercial purposes.
    It does not restrict the use of JPPF with commercial and proprietary applications.
    <br>
    </div>
  </div>
  <div class="column_right" style="text-align: justify">
    <br><div class="blockWithHighlightedTitle">
    <a name="downloads"></a>
    <h3><img src="images/icons/download.png" class="titleWithIcon"/>Downloads</h3>
    All files can be found from our <a href="/downloads.php">downloads page</a>.<br>
    <br>
    </div>
    <br><div class="blockWithHighlightedTitle">
    <a name="documentation"></a>
    <h3><img src="images/icons/documentation.png" class="titleWithIcon"/>Documentation</h3>
    <p>The JPPF documentation can be found <a href="/doc/v5">online</a>. You may also read it offline as <a href="/documents/JPPF-User-Guide.pdf">a PDF document</a>.
    <br>
    </div>
    <br><div class="blockWithHighlightedTitle">
    <a name="contacts"></a>
    <h3><img src="images/icons/contact.png" class="titleWithIcon"/>Contacts</h3>
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
