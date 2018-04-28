<?php $currentPage="Press" ?>
<?php $jppfVersion="5.2" ?>
$template{name="about-page-header" title="Press Kit"}$

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
$template{name="title-with-icon" img="images/icons/news.png" title="Press release: JPPF <?php echo $jppfVersion ?>" heading="h3"}$

<p><b>Administration console:</b>
<ul class="samplesList">
  <li><a href="/screenshots/gallery-images/Admin%20Console%20-%20Desktop/NodeFiltering-Active.gif">node filtering</a> with an execution policy editor with import/export capabilities</li>
  <li>ability to <a href="/screenshots/gallery-images/Admin%20Console%20-%20Desktop/VisibleServerStatistics.png">select the visible statistics</a> in the server statiscs view</li>
  <li>syntax hihghlighting in all the editors: properties/node filtering</li>
  <li>the admin console splash screen is now <a href="/doc/5.2/index.php?title=Client_and_administration_console_configuration#Customizing_the_administration_console.27s_splash_screen">customizable</a> via the configuration</li>
  <li>the administration console is now fully localized, with full <a href="/screenshots/gallery-images/Admin%20Console%20-%20Desktop/UpdateNodeConfiguration.gif">English</a> and <a href="/screenshots/gallery-images/Admin%20Console%20-%20Desktop/UpdateNodeConfigurationFrench.gif">French</a> translations available
</ul>

<p><b>Configuration:</b>
<ul class="samplesList">
  <li>all documented properties are now defined as <a href="/javadoc/5.2/index.html?org/jppf/utils/configuration/JPPFProperties.html">constants</a></li>
  <li>a new and elegant <a href="/doc/5.2/index.php?title=The_JPPF_configuration_API#Predefined_JPPF_properties">API</a> was created to handle them easily</li>
  <li>it is now possible to specify in the configuration which JVM to use for the nodes and servers. This also applies to master and slave nodes when they are (re)started</li>
</ul>

<p><b>Customization/extension:</b>
<ul class="samplesList">
  <li>ability to <a href="/doc/5.2/index.php?title=Composite_serialization">chain serialization schemes</a> to provide compression or encryption over actual serialization</li>
  <li><a href="/doc/5.2/index.php?title=Specifying_alternate_serialization_schemes#Generic_JPPF_serialization">the JPPF serialization</a> scheme was thouroughly optimized and is now faster than the Java serialization</li>
  <li>it is now possible to register for <a href="/doc/5.2/index.php?title=Receiving_server_statistics_events">statistics change events</a> in the server</li>
  <li><a href="/doc/5.2/index.php?title=Network_interceptors">Network communication interceptors</a> enable user-defined code to be executed on both sides of each new connection</li>
  <li>A <a href="/doc/5.2/index.php?title=Pluggable_MBeanServerForwarder">pluggable MBeanServerForwarder</a> can now be associated to the JMX remote connector servers created by JPPF drivers and nodes</li>
  <li><a href="/doc/5.2/index.php?title=Environment_providers_for_JMX_remote_connections">Pluggable environment providers</a> for JMX remote connector clients and servers</li>
</ul>

<p><b>Android node:</b>
<ul class="samplesList">
  <li>It is now possible to configure the node to stop working or terminate <a href="/doc/5.2/index.php?title=Android_Node#Battery_state_monitoring">when the device's battery is low</a></li></li>
  <li>Improved the <a href="/screenshots/gallery-images/Android/AndroidMainScreenBusy.gif">default feedback screen</a></li>
</ul>

<p><b>Job SLA:</b>
<ul class="samplesList">
  <li>The job SLA can now specifiy <a href="/doc/5.2/index.php?title=Job_Service_Level_Agreement#Grid_policy">filtering rules</a> based on the server properties and the number of nodes satisfying one or more conditions<br>
  Example: "execute when the server has at least 2 GB of heap memory and at least 3 nodes with more than 4 cores each"</li>
  <li>The job SLA can <a href="/doc/5.2/index.php?title=Job_Service_Level_Agreement#Specifying_the_desired_node_configuration">specify the desired configuration</a>
  of the nodes on which it will execute and force the nodes to reconfigure themselves accordingly</li>
  <li>execution policies based on server properties now have <a href="/doc/5.2/index.php?title=Execution_policy_properties#Server_statistics">access to the server statistics</a>
</ul>

<p><b>Management and Monitoring</b>
<p>Two new types of node selectors are now available: <a href="/doc/5.2/index.php?title=Nodes_management_and_monitoring_via_the_driver#Scripted_node_selector">scripted node selector</a> and <a href="/doc/5.2/index.php?title=Nodes_management_and_monitoring_via_the_driver#Custom_node_selector">custom node selector</a>

<p><b>Load-balancing</b>
<p>A <a href="/doc/5.2/index.php?title=Built-in_algorithms#.22rl2.22">new load-balancing algorithm</a>, named "rl2", was implemented

<p><b>Documentation</b>
<p>Added a complete section on <a href="/doc/5.2/index.php?title=Load_Balancing">load balancing</a>

<p><b>Samples</b>
<ul class="samplesList">
  <li>A new sample was added, illustrating a full-fledged management of <a href="/samples-pack/JobDependencies">dependencies between jobs</a></li>
  <li>The <a href="/samples-pack/NetworkInterceptor">Network Interceptor sample</a> shows how a network connection interceptor can be used to implement a simple authentication mechanism with symetric encryption</li>
</ul>

<p><b>Packaging</b>
<p>The JPPF jar files now include the version number in their name, e.g. jppf-common-5.2.jar

<p><b>Continuous Integration</b>
<ul class="samplesList">
  <li>A large amount of time and effort was invested in setting up a continuous integration environment based on Jenkins</li>
  <li>Automated builds are now in place with <a href="/ci.php">results</a> automatically published to the JPPF web site</li>
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
