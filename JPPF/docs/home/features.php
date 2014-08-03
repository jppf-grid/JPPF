<?php $currentPage="Features" ?>
<html>
	  <head>
    <title>JPPF Features
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
<h1>JPPF 4.x features</h1>
<h3>Ease of use</h3>
<ul>
  <li>simple APIs requiring small or no learning curve</li>
  <li>automatic deployment of application code on the grid</li>
  <li>ability to reuse existing or legacy objects without modification</li>
  <li>"happy path" with no additional configuration</li>
  <li>automatic server discovery, combinable with manual connection configuration</li>
  <li>convenient reusable application template to quickly and easily start developing JPPF applications</li>
  <li>straightforward Executor Service interface to the JPPF grid, with high throughput enhancements</li>
</ul>
<h3>Security</h3>
<ul>
  <li>secure network communications via SSL / TLS</li>
  <li>data encryption</li>
  <li>data integrity</li>
  <li>certificate-based authentication, including 2 ways (server and client) authentication</li>
  <li>plain and secure communications can be used separately or together</li>
</ul>
<h3>Cloud ready</h3>
<ul>
  <li>JPPF is ideally suited for cloud deployment</li>
  <li>native dynamic topology makes cloud on-demand scaling transparent</li>
  <li>multiple extensible options for server discovery in the cloud</li>
  <li>documented deployment on Amazon EC2</li>
</ul>
<h3>Self-repair and recovery</h3>
<ul>
  <li>automated node reconnection with failover strategy</li>
  <li>automated client reconnection with failover strategy</li>
  <li>fault tolerance with job requeuing</li>
  <li>detection, recovery from hard-disconnects of remote nodes</li>
</ul>
<h3>Job checkpointing</h3>
<ul>
  <li>automatic latest job state resubmission by the server in case of node failure</li>
  <li>automatic latest job state resubmission by the client in case of server failure</li>
  <li>support for maximum number of resubmissions and job timeout</li>
  <li>job states can be locally persisted for recovery in case of client failure</li>
</ul>
<h3>Job-level SLA</h3>
<ul>
  <li>job execution policies enable rule-based node filtering</li>
  <li>maximum number of nodes a job can run on (grid partitioning)</li>
  <li>dynamic job prioritization</li>
  <li>job scheduled start date</li>
  <li>job scheduled expiration date</li>
  <li>broadcast jobs</li>
</ul>
<h3>Job local monitoring, persistence, recovery, failover</h3>
<ul>
  <li>notification of job start and stop events</li>
  <li>ability to store job snapshots on the fly</li>
  <li>recovery of the latest job state upon application crash</li>
</ul>
<h3>Management and monitoring</h3>
<ul>
  <li>task-level events</li>
  <li>job-level events</li>
  <li>server performance statistics</li>
  <li>server performance charts</li>
  <li>user-defined charts</li>
  <li>remote server control and monitoring</li>
  <li>remote nodes control and monitoring</li>
  <li>cpu utilization monitoring</li>
  <li>management of load-balancing</li>
  <li>management and monitoring available via APIs and graphical user interface (administration console)</li>
  <li>access to remote servers and nodes logs via the JMX-based logger (integrates with Log4j and JDK logging)</li>
  <li>monitoring of remote JVMs health: heap and non-heap memory, cpu load, live threads and deadlock monitoring</li>
</ul>
<h3>Platform extensibility</h3>
<ul>
  <li>All management beans are pluggable, users can add their own management modules at server or node level</li>
  <li>Startup classes: users can add their own initialization modules at server and node startup</li>
  <li>Security: any data transiting over the network can now be encrypted by the way of user-defined transformations</li>
  <li>Pluggable load-balancing modules allow users to write their own load balancing strategies</li>
  <li>Ability to specify alternative serialization schemes</li>
  <li>Subscription to nodes life cycle events</li>
  <li>Node initialization hooks to implement dynamic failover strategies</li>
  <li>Server-side notifications of node connection events</li>
</ul>
<h3>Dynamic class loading extensions</h3>
<ul>
  <li>update the classpath of the nodes at run-time</li>
  <li>download multiple classpath resources in a single network transaction</li>
  <li>remotely manage repositories of application libraries</li>
  <li>new class loader delegation model enables faster class loading</li>
</ul>
<h3>Performance and resources efficiency</h3>
<ul>
  <li>multiple configurable load-balancing algorithms</li>
  <li>adaptive load-balancing adjusts in real-time to workload changes</li>
  <li>memory-aware components with disk overflow</li>
  <li>client-side server connection pools</li>
</ul>
<h3>Dynamic topology scaling and transformation</h3>
<ul>
  <li>nodes can be added and removed dynamically from the grid</li>
  <li>servers can be added and removed dynamically from the grid</li>
  <li>servers can work alone or linked in P2P topology with other servers</li>
  <li>ability to run a node in the same JVM as the server</li>
  <li>from traditional HPC cluster to full-fledged P2P, to volunteer computing topology</li>
</ul>
<h3>Third-party connectors</h3>
<ul>
  <li>J2EE connector, JCA 1.5 compliant, deployed as a standard resource adapter</li>
  <li>Apache Tomcat connector</li>
</ul>
<h3>Ease of integration</h3>
<ul>
  <li><a href="samples-pack/FTPServer/Readme.php">Apache FTP server</a></li>
  <li><a href="samples-pack/DataDependency/Readme.php">Hazelcast data grid</a></li>
  <li><a href="samples-pack/NodeLifeCycle/Readme.php">Atomikos transaction manager and JDBC database</a></li>
  <li><a href="samples-pack/GPU/Readme.php">Harnessing the power of the GPU with APARAPI</a></li>
</ul>
<h3>Deployment modes</h3>
<ul>
  <li>all components deployable as standalone Java applications</li>
  <li>servers and nodes deployable as Linux/Unix daemons</li>
  <li>servers and nodes deployable as Windows services</li>
  <li>application client deployment as a Web, J2EE or GigaSpaces XAP application
  <li>nodes can run in idle system mode (CPU scavenging)</li>
  <li>deployment on Amazon EC2</li>
</ul>
<h3>Execution modes</h3>
<ul>
  <li>synchronous and asynchronous job submissions</li>
  <li>client can execute in local mode (benefits to systems with many CPUs)</li>
  <li>client can execute in distributed mode (execution delegated to remote nodes)</li>
  <li>client can execute in mixed local/distributed mode with adaptive load-balancing</li>
</ul>
<h3>Full fledged samples</h3>
<ul>
  <li><a href="samples-pack/Fractals/Readme.php">Generation of fractal images (Mandelbrot Set)</a></li>
  <li><a href="samples-pack/SequenceAlignment/Readme.php">Protein and DNA sequence alignment</a></li>
  <li><a href="samples-pack/WebSearchEngine/Readme.php">Distributed web crawler and search engine</a></li>
  <li><a href="samples-pack/TomcatPort/Readme.php">Tomcat 5.5/6.0 port</a></li>
  <li><a href="samples-pack/CustomMBeans/Readme.php">Pluggable management beans</a></li>
  <li><a href="samples-pack/DataEncryption/Readme.php">Network data encryption</a></li>
  <li><a href="samples-pack/StartupClasses/Readme.php">Customized server and node initialization</a></li>
  <li><a href="samples-pack/MatrixMultiplication/Readme.php">Basic dense matrix multiplication parallelization</a></li>
  <li><a href="samples-pack/DataDependency/Readme.php">Simulation of large portfolio updates</a></li>
  <li><a href="samples-pack/NodeTray/Readme.php">JPPF node health monitor in the system tray</a></li>
  <li><a href="samples-pack/CustomLoadBalancer/Readme.php">An example of a sophisticated load-balancer implementation</a></li>
  <li><a href="samples-pack/IdleSystem/Readme.php">An extension that enables nodes to run only when the machine is idle</a></li>
  <li><a href="samples-pack/NodeLifeCycle/Readme.php">Control of database transactions via node life cycle events</a></li>
  <li><a href="samples-pack/Nbody/Readme.php">Parallel N-body problem applied to anti-protons trapped in a  magnetic field</a></li>
  <li><a href="samples-pack/FTPServer/Readme.php">How to embed and use an FTP server in JPPF</a></li>
  <li><a href="samples-pack/NodeConnectionEvents/Readme.php">How to receive notifications of nodes connecting and disconnecting on the server</a></li>
  <li><a href="samples-pack/JobRecovery/Readme.php">Job recovery after an application crash</a></li>
  <li><a href="samples-pack/InitializationHook/Readme.php">Using a node initialization hook to implement a sophisticated failover mechanism</a></li>
  <li><a href="samples-pack/ExtendedClassLoading/Readme.php">On-demand management and deployment of appplication libraries in the Grid</a></li>
  <li><a href="samples-pack/GPU/Readme.php">Harnessing the power of the GPU with APARAPI</a></li>
  <li><a href="samples-pack/KryoSerializer/Readme.php">A JPPF serialization scheme using Kryo</a></li>
  <li><a href="samples-pack/ForkJoinNodeExecutor/Readme.php">A Fibonacci computation demo using fork/join thread pools in the nodes</a></li>
  <li><a href="samples-pack/WordCount/Readme.php">A word count demo on the Wikipedia database</a></li>
  <li><a href="samples-pack/FractalMovieGenerator/Readme.php">Generating AVI movies</a> from images produced by the <a href="Fractals/Readme.php">Mandelbrot fractals sample</a></li>
  <li><a href="samples-pack/NodeProvisioning/Readme.php">A cloud-like JPPF node provisioning facility</a></li>
</ul>
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
