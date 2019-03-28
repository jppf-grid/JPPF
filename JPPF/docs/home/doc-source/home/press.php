<?php $currentPage="Press" ?>
<?php $jppfVersion="6.1" ?>
$template{name="about-page-header" title="Press Kit"}$

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
$template{name="title-with-icon" img="images/icons/news.png" title="Press release: JPPF <?php echo $jppfVersion ?>" heading="h2"}$

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
    $template{name="title-with-icon" img="images/icons/view-list.png" title="Features" heading="h3"}$
    <div class="u_link" style="margin-left: 10px">
      <a href="release_notes.php?version=<?php echo $jppfVersion ?>">Release notes</a>: see everything that's new in JPPF <?php echo $jppfVersion ?><br>
      Our <a href="features.php">features page</a> provides a comprenhensive overview of what JPPF has to offer.<br>
    </div><br>
    </div>

    <br><div class="blockWithHighlightedTitle">
    <a name="downloads"></a>
    $template{name="title-with-icon" img="images/icons/download.png" title="Downloads" heading="h3"}$
    All files can be found from our <a href="/downloads.php">downloads page</a>.<br>
    <br>
    </div>

    <br><div class="blockWithHighlightedTitle">
    <a name="contacts"></a>
    $template{name="title-with-icon" img="images/icons/contact.png" title="Contacts" heading="h3"}$
    <p>For any press inquiry, please refer to our <a href="/contacts.php">contacts</a> page.
    <br>
    </div>

  </div>

  <div class="column_right" style="text-align: justify">

    <br><div class="blockWithHighlightedTitle">
    <a name="documentation"></a>
    $template{name="title-with-icon" img="images/icons/documentation.png" title="Documentation" heading="h3"}$
    <p>The JPPF documentation can be found <a href="/doc/<?php echo $jppfVersion ?>">online</a>. You may also read it offline as <a href="/download/JPPF-User-Guide.pdf">a PDF document</a>.
    <br>
    </div><br>

    <br><div class="blockWithHighlightedTitle">
    <a name="license"></a>
    $template{name="title-with-icon" img="images/icons/document-sign.png" title="License" heading="h3"}$
    <p>JPPF is released under the terms of the <a href="/license.php">Apachache v2.0</a> license.
    This <a href="http://www.opensource.org">OSI-approved</a> open source license is friendly to individuals, businesses, governments and academia, for commercial and non-commercial purposes.
    It does not restrict the use of JPPF with commercial and proprietary applications.
    <br>
    </div>

  </div>

</div>

$template{name="about-page-footer"}$
