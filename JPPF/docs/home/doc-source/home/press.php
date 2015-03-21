<?php $currentPage="Press" ?>
<?php $jppfVersion="5.0" ?>
$template{name="about-page-header" title="Press Kit"}$

<div align="justify">
  <h1>JPPF Press Kit</h1>
  <div class="blockWithHighlightedTitle">
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
  </div>

  <br><div class="blockWithHighlightedTitle">
  <a name="original_release"></a>

<!-- ============================== -->
<!-- start version-specific content -->
<!-- ============================== -->
$template{name="title-with-icon" img="images/icons/news.png" title="Press release: JPPF <?php echo $jppfVersion ?>" heading="h3"}$

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
<!-- ============================== -->
<!-- end version-specific content   -->
<!-- ============================== -->
  <br>
  </div>

  <div class="column_left" style="text-align: justify">

    <br><div class="blockWithHighlightedTitle">
    <a name="features"></a>
    $template{name="title-with-icon" img="images/icons/view-list.png" title="Features" heading="h3"}$
    <div class="u_link" style="margin-left: 10px">
      <a href="release_notes.php?version=<?php echo $jppfVersion ?>">Release notes</a>: see everything that's new in JPPF <?php echo $jppfVersion ?><br>
      Our <a href="features.php">features page</a> provides a comprenhensive overview of what JPPF has to offer.<br>
    </div>
    <br>
    </div>
    <br>

    <br><div class="blockWithHighlightedTitle">
    <a name="license"></a>
    $template{name="title-with-icon" img="images/icons/document-sign.png" title="License" heading="h3"}$
    <p>JPPF is released under the terms of the <a href="/license.php">Apachache v2.0</a> license.
    This <a href="http://www.opensource.org">OSI-approved</a> open source license is friendly to individuals, businesses, governments and academia, for commercial and non-commercial purposes.
    It does not restrict the use of JPPF with commercial and proprietary applications.
    <br>
    </div>

  </div>

  <div class="column_right" style="text-align: justify">

    <br><div class="blockWithHighlightedTitle">
    <a name="downloads"></a>
    $template{name="title-with-icon" img="images/icons/download.png" title="Downloads" heading="h3"}$
    All files can be found from our <a href="/downloads.php">downloads page</a>.<br>
    <br>
    </div>

    <br><div class="blockWithHighlightedTitle">
    <a name="documentation"></a>
    $template{name="title-with-icon" img="images/icons/documentation.png" title="Documentation" heading="h3"}$
    <p>The JPPF documentation can be found <a href="/doc/v5">online</a>. You may also read it offline as <a href="/documents/JPPF-User-Guide.pdf">a PDF document</a>.
    <br>
    </div>

    <br><div class="blockWithHighlightedTitle">
    <a name="contacts"></a>
    $template{name="title-with-icon" img="images/icons/contact.png" title="Contacts" heading="h3"}$
    <p>For any press inquiry, please refer to our <a href="/contacts.php">contacts</a> page.
    <br>
    </div>
    <br>

  </div>

</div>

$template{name="about-page-footer"}$
