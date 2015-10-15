<?php $currentPage="Press" ?>
<?php $jppfVersion="5.1" ?>
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

<p><b>Android integration</b>
<p>The main focus of this release, the <a href="/doc/v5/index.php?title=Android_Node">Android node</a> for JPPF, enables execution of Java or Android workloads on Android devices.
<ul class="samplesList">
  <li>Execute <a href="/doc/v5/index.php?title=Android_Node#Creating_and_submitting_jobs">arbitrary Java workloads</a> on Android Kitkat or later devices</li>
  <li><a href="/doc/v5/index.php?title=Android_Node#Getting_and_providing_feedback_from_the_node_and_tasks.">customize the visual feedback</a> on the device based on tasks notifications and node events</li>
  <li>Secure all operations with <a href="/doc/v5/index.php?title=Android_Node#Using_SSL_.2F_TLS">dedicated SSL/TLS settings</a></li>
  <li>Includes a dedicated demo that works with the <a href="/samples-pack/Fractals/Readme.php">Mandelbrot fractal sample</a></li>
  <li><a href="/samples-pack/AndroidDemo/Readme.php">Dedicated sample</a> illustrating how to package a Java workload for execution on Android</li>
</ul>

<p><b>Client-side Job monitoring API</b>
<p>A new <a href="/doc/v5/index.php?title=Job_monitoring_API">job monitoring API</a>, which builds on, and complements, the <a href="/doc/v5/index.php?title=Grid_topology_monitoring">topology monitoring</a>, provides an automatically updated view of the jobs hierarchy by server / jobs / node dispatches.
<ul class="samplesList">
  <li>Navigate and manipulate the <a href="/doc/v5/index.php?title=Job_monitoring_API#Job_monitor_and_jobs_hierarchy">job hierarchy</a></li>
  <li>Receive <a href="/doc/v5/index.php?title=Job_monitoring_API#Receiving_job_monitoring_events">notifications</a> of job events</li>
  <li>Configure the <a href="/doc/v5/index.php?title=Job_monitoring_API#Update_modes">granularity and frequency of updates</a> for an optimal tradeoff between accuracy and performance</li>
  <li>The administration console's code was refactored to use the job monitoring API, with update mode settings in its <a href="/doc/v5/index.php?title=Client_and_administration_console_configuration#UI_refresh_intervals_in_the_administration_tool">configuration</a></li>
</ul>

<p><b>Management and monitoring</b>
<ul class="samplesList">
  <li>Job monitoring and management operations now accept a <a href="/doc/v5/index.php?title=Server_management#Job_selectors">job selector</a> parameter, allowing bulk operations with fine-grained filtering</li>
  <li>It is now possible to dynamically <a href="/doc/v5/index.php?title=Server_management#Updating_the_job_SLA_and_metadata">update the SLA and metadata</a> of a job, even while it is executing</li>
  <li>Node connection events can now be received as remote <a href="/doc/v5/index.php?title=Receiving_node_connection_events_in_the_server#JMX_notifications">JMX notifications</a></li>
  <li>The <a href="/api-5/index.html?org/jppf/management/forwarding/JPPFNodeForwardingMBean.html">node forwarding MBean</a> now has dedicated methods for node provisioning, providing a much less cumbersome API</li>
</ul>

<p><b>Server extensions and improvements</b>
<ul class="samplesList">
  <li>It is now possible to receive notifications of <a href="/doc/v5/index.php?title=Receiving_the_status_of_tasks_dispatched_to_or_returned_from_the_nodes">tasks status at dispatch time</a> in the server</li>
  <li>Servers are now notified in real time of node connection events in other servers, and may decide to <a href="/doc/v5/index.php?title=Configuring_a_JPPF_server#Orphan_servers">exclude orphan servers</a> from job scheduling</li>
</ul>

<p><b>Other enhancements</b>
<ul class="samplesList">
  <li>The <a href="/doc/v5/index.php?title=The_JPPF_configuration_API">configuration API</a> now has a fluent interface for setting properties with typed values</li>
  <li><a href="/doc/v5/index.php?title=Creating_a_custom_load-balancer#Job-aware_load_balancers">Job-aware</a> load balancers now have full access to the information provided by the <a href="/api-5/index.html?org/jppf/node/protocol/JPPFDistributedJob.html">JPPFDistributedJob</a> interface</li>
  <li>The "nodethreads" load-balancing algorithm now takes into account the sum of the threads in all the nodes attached to a peer server</li>
</ul>

<p><b>Documentation improvements</b>
<p>All code in the online documentation and samples now benefits from a full-fledged syntax highlighting, in the hope of making your reading experience an even more pleasant one.

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
