<?php $currentPage="About" ?>
<html>
	  <head>
    <title>JPPF About JPPF
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
        <?php if ($currentPage == "v5.x") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/v5" class="<?php echo $itemClass; ?>">v5.x</a><br></div>
        <?php if ($currentPage == "v4.x") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/v4" class="<?php echo $itemClass; ?>">v4.x</a><br></div>
        <?php if ($currentPage == "v3.x") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/v3" class="<?php echo $itemClass; ?>">v3.x</a><br></div>
        <?php if ($currentPage == "v2.x") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/v2" class="<?php echo $itemClass; ?>">v2.x</a><br></div>
        <?php if ($currentPage == "Javadoc") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/api" class="<?php echo $itemClass; ?>">&raquo; Javadoc</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/api-5" class="<?php echo $itemClass; ?>">v5.x</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/api" class="<?php echo $itemClass; ?>">v4.x</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/api-3" class="<?php echo $itemClass; ?>">v3.x</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/api-2.0" class="<?php echo $itemClass; ?>">v2.x</a><br></div>
        <hr/>
        <?php if ($currentPage == "Issue tracker") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/tracker/tbg" class="<?php echo $itemClass; ?>">&raquo; Issue tracker</a><br></div>
        <?php if ($currentPage == "bugs") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/1/search/1" class="<?php echo $itemClass; ?>">bugs</a><br></div>
        <?php if ($currentPage == "features") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/9/search/1" class="<?php echo $itemClass; ?>">features</a><br></div>
        <?php if ($currentPage == "enhancements") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/2/search/1" class="<?php echo $itemClass; ?>">enhancements</a><br></div>
        <?php if ($currentPage == "current work") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/8/search/1" class="<?php echo $itemClass; ?>">current work</a><br></div>
        <hr/>
        <?php if ($currentPage == "Press") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/press.php" class="<?php echo $itemClass; ?>">&raquo; Press</a><br></div>
        <?php if ($currentPage == "Release notes") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/release_notes.php?version=4.2" class="<?php echo $itemClass; ?>">&raquo; Release notes</a><br></div>
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
		<h3>What it is</h3>
		Simply put, JPPF enables applications with large processing power requirements to be run on any number of computers, in order to dramatically reduce their processing time.
		This is done by splitting an application into smaller parts that can be executed simultaneously on different machines.
		<h3>How it works</h3>
		<p>There are 2 aspects to this:
		<p><i>Dividing an application into smaller parts that can be executed independantly and in parallel.</i>
		<br>JPPF provides facilities that make this effort a lot easier, faster and much less painful than without them.
		The result is a JPPF object called a "job", itself made of smaller independant parts called "tasks".
		<p><i>Executing the application on the JPPF Grid.</i>
		<br>A JPPF Grid is made of a server, to which any number of execution nodes are attached. A node is a JPPF software component that is generally installed and running on a separate machine.
		This is commonly called a master/slave architecture, where the work is distributed by the server (aka "master") to the nodes (aka "slaves").
		In JPPF terms, a unit of work is called a "job", and its constituting "tasks" are distributed by the server among the nodes for parallel execution.
		<h3>Advantages</h3>
		<p>Chief among JPPF benefits is its ease of installation, use and deployment. There is no need to spend days to write a "Hello World" application. A couple of minutes, up to a couple of hours at most, will suffice.
		Deploying JPPF components over a cluster is as simple as copying files over FTP or any network file system.
		JPPF allows developers to focus on their core software development, instead of wasting time on the complexities of parallel and distributed processing.
	`	<p>As a 100% Java framework, JPPF will run on any system that supports Java: MacOS, Windows, Linux, zOS, on any hardware from a simple laptop up to a mainframe computer.
		This does not mean that JPPF is limited to running Java jobs. You can run any application that is available on your platform as a JPPF job.
		For instance, you might want to run your favorite graphics suite in batch mode, to render multiple large, complex images all at once.
		<p>Another benefit of JPPF is a simplified, almost immediate, deployment process of your application on the grid.
		Even though your aplication will be run on many nodes at once, you only need to deploy it in a single location.
		By extending the Java class loading mechanism, JPPF removes most of the deployment burden from the application's life cycle, dramatically shortening the time-to-market and time-to-production.
		<h3>Outstanding features</h3>
		<p>There is a lot more to JPPF than running and deploying your applications on the grid.
		The features are so numerous that we have dedicated a separate page to enumerate them fully.
		For a complete list of what JPPF has to offer, please consult our <a href="features.php">full features list</a>.
		<p><b>Comprehensive, easy to use APIs.</b><br>
		Passing from a single-threaded application model to a grid-based parallel model can be a daunting task.
		JPPF facilitates this work by providing developers with a set of APIs that are simple, can be learned quickly and require a minimal or no modification of the existing code.
		<p><b>No configuration usage.</b><br>
		In most environments, JPPF can be deployed without any additional configuration burden. Nodes and application clients will automatically dicover the servers on the network.
		The server will automatically adapt to workload changes and optimize the throughput. Required code and libraries will be automatically deployed where they are needed.
		<p><b>Dynamic grid scaling and self-repair.</b><br>
		The JPPF grid is fault-tolerant, meaning that the failure of a node, or even a server, does not compromise the jobs currently executing or scheduled.
		In most cases, the performance degradation will be barely noticeable, as JPPF automatically adapts to topology and workload changes.
		Furthermore, nodes and servers can be dynamically started and will be automatically recognized, allowing JPPF to function in "crunch mode".
		In addition to this, lll JPPF components benefit from automatic recovery functionalities.
		<p><b>Job-level SLA.</b><br>
		Each job submitted to the JPPF grid runs within limits defined by its own SLA (service level agreement).
		This allows to specify the characteristics (i.e. available memory, processors, disk space, operating systems, etc.) of the nodes a job can run on, as well as how many nodes it can run on.
		As many functionalities in JPPF, this one can be dynamically adjusted, manually or automatically.
		<p><b>Management and monitoring.</b><br>
		Full-fledged management and monitoring features are provided out of the box: server and nodes status monitoring, detailed statistics and events, remote administration, job-level real-time monitoring and management, charts, cpu utilization (for billing).
		These functionalities are available via a graphical user interface as well as from the JPPF APIs.
		<p><b>Integration with leading application and web servers.</b><br>
		By complying with the Java Connector Architecture 1.5 specification, JPPF integrates seamlessly with and completes the offering of leading J2EE application servers:
		JBoss<sup>&reg;</sup>, Glassfish<sup>&reg;</sup>, IBM Websphere<sup>&reg;</sup>, Oracle Weblogic<sup>&reg;</sup>, Oracle OC4J<sup>&reg;</sup>.
		JPPF also integrates with GigaSpaces eXtreme Application Platform<sup>&reg;</sup> and Apache Tomcat web server<sup>&reg;</sup>
	</div>
	<br>
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
        <td align="center" style="font-size: 9pt; color: #6D78B6">Copyright &copy; 2005-2014 JPPF.org</td>
        <td align="center" valign="center">
          <!-- Google+ button -->
          <div class="g-plusone" data-href="http://www.jppf.org" data-annotation="bubble" data-size="small" data-width="300"></div>
          <script type="text/javascript">
            (function() {
              var po = document.createElement('script'); po.type = 'text/javascript'; po.async = true;
              po.src = 'https://apis.google.com/js/platform.js';
              var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(po, s);
            })();
          </script>
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
