<?php $currentPage="Press" ?>
<?php $jppfVersion="6.2" ?>
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
