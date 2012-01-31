<?php $currentPage="Press" ?>
<?php $jppfVersion="3.0" ?>
$template{name="about-page-header" title="Press Kit"}$
$template{name="press-header"}$

<h3>Latest press release: JPPF <?php echo $jppfVersion ?></h3>

<p><b>Simplified network configuration</b>: only <a href="/doc/v3/index.php?title=Configuring_a_JPPF_server#Basic_network_configuration">one TCP port</a> for grid operations, only one port for management, that's only 2 ports needed instead of 5 in previous versions.
Server manual and auto discovery can now be <a href="/doc/v3/index.php?title=Client_and_administration_console_configuration#Using_manual_configuration_and_server_discovery_together">mixed</a>.

<p><b>Job recovery</b>: grid jobs will survive an application crash and will be restarted from their latest persisited state. Documentation is <a href="/doc/v3/index.php?title=Jobs_runtime_behavior%2C_recovery_and_failover#Job_persistence_and_recovery">here</a>
and an example can be found <a href="/samples-pack/JobRecovery/Readme.php">here</a>. 

<p><b>Class loading</b>: full <a href="/doc/v3/index.php?title=Class_loading_in_JPPF">documentation</a>, new <a href="/doc/v3/index.php?title=Class_loading_in_JPPF#JPPF_class_loading_extensions">extensions</a>,
multiple <a href="/doc/v3/index.php?title=Class_loading_in_JPPF#Class_loader_delegation_models">delegation models</a> enable faster startup of grid jobs and nodes, and open up new possibilities for efficient and flexible grid usage.
A new <a href="/samples-pack/ExtendedClassLoading/Readme.php">sample</a> illustrates how these can be used to automate the management of remote grid nodes classpath.

<p><b>New extension points</b> allow powerful capabilities to be easily added to an existing grid infrastructure.

<p><b>The administration console</b> now enables management of <a href="/screenshots.php?screenshot=JobPriority.gif">jobs priority</a>, displays <a href="/screenshots.php?screenshot=JobStatistics.gif">jobs statistics</a> and
<a href="/screenshots.php?screenshot=ConnectionsStatistics.gif">idle nodes</a>, retrieves full <a href="/screenshots.php?screenshot=DriverSystemInformation.gif">system information</a> from servers.

<p style="margin-bottom: 0px"><b>Four new samples</b>:
<ul class="samplesList" style="margin-top: 0px">
	<li><a href="/samples-pack/NodeConnectionEvents/Readme.php">How to receive notifications of nodes connecting and disconnecting on the server</a></li>
	<li><a href="/samples-pack/JobRecovery/Readme.php">Job recovery after an application crash</a></li>
	<li><a href="/samples-pack/InitializationHook/Readme.php">Using a node initialization hook to implement a sophisticated failover mechanism</a></li>
	<li><a href="/samples-pack/ExtendedClassLoading/Readme.php">Automating the deployment and management of application libraries in the nodes at runtime</a></li>
</ul>

$template{name="press-footer"}$
$template{name="about-page-footer"}$
