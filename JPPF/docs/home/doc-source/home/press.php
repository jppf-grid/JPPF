<?php $currentPage="Press" ?>
<?php $jppfVersion="5.0" ?>
$template{name="about-page-header" title="Press Kit"}$
$template{name="press-header"}$

<h3>Press release: JPPF <?php echo $jppfVersion ?></h3>

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

$template{name="press-footer"}$
$template{name="about-page-footer"}$
