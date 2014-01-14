<?php $currentPage="Press" ?>
<?php $jppfVersion="4.0" ?>
$template{name="about-page-header" title="Press Kit"}$
$template{name="press-header"}$

<h3>Press release: JPPF <?php echo $jppfVersion ?></h3>

<p><b>Support for Java 7 and later</b>: As of this release, JPPF will stop supporting Java 6 and will only run on Java 7 and later.<br/>
<span style="font-style: italic">Application code written and compiled with Java 6 will still run as is</span>.

<p><b>Full support for volunteer computing</b>: "JPPF@home" is now a reality. Build your volunteer computing project with all the benefits of a JPPF Grid and the underlying Java technologies.
This is made possible with the addition of new capabilities which can be combined or used individually and enhance the scalability and reliability of JPPF grids:
<ul class="samplesList">
  <li><a href="/doc/v4/index.php?title=Deployment_and_run_modes#Offline_nodes">Offline nodes</a> work disconnected from the grid and only connect to get more work</li>
  <li>New <a href="/doc/v4/index.php?title=Deployment_and_run_modes#Avoiding_stuck_jobs">fault-tolerance capabilities</a> handle cases when a node fails to return results</li>
  <li><a href="/doc/v4/index.php?title=JPPF_node_screensaver">Customizable screen saver</a> associated with each node, with entry points for receiving feedback from the tasks and jobs at any point of their life cycle.
  JPPF also includes a full-fledged, highly personalizable, <a href="/doc/v4/index.php?title=JPPF_node_screensaver#JPPF_built-in_screensaver">default animated screen saver</a></li>
  <li>The ability to <a href="/doc/v4/index.php?title=Job_Service_Level_Agreement#Setting_a_class_path_onto_the_job">transport Java libraries along with the jobs</a> enables the use of JPPF nodes as universal volunteer computing clients, enabling them to run multiple projects with a single installation</li>
</ul>

<p><b>Support for dynamic scripting</b>: JPPF 4.0 leverages the <a href="https://www.jcp.org/en/jsr/detail?id=223">JSR 223 specification</a> and corresponding <a href="http://docs.oracle.com/javase/7/docs/api/javax/script/package-summary.html">javax.script</a> APIs to enable dynamic scripting and raise its capabilities to a new level:
<ul class="samplesList">
  <li><a href="/doc/v4/index.php?title=Task_objects#Executing_dynamic_scripts:_ScriptedTask">scripted tasks</a> allow you to execute tasks entirely written in any JSR 223-compliant dynamic script language</a></li>
  <li><a href="/doc/v4/index.php?title=Execution_Policies#Scripted_policies">scripted execution policies</a> make node and server channel filtering easier and more powerful than ever</li>
</ul>

<p><b>Management console enhancements</b>:
<ul class="samplesList">
  <li>Every tab in the administration console can be displayed <a href="/screenshots.php?screenshot=Docking-3.gif&shotTitle=Docking%203">in a separate view</a></li>
  <li>New statistics were added to the <a href="/screenshots.php?screenshot=ServerStats-01.gif&shotTitle=Server%20Stats%2001">server statistics view</a>: class loading requests from the node and to the client, inbound and outbound network traffic to and from the nodes and clients</li>
  <li>Statistics can now be exported to the clipboard from the <a href="/screenshots.php?screenshot=ServerStats-01.gif&shotTitle=Server%20Stats%2001">server statistics view</a>, as either CSV or formatted plain text</li>
  <li>The statistics view now uses a flow layout for a better usability and user experience</li>
</ul>

<p><b>Revamped task API</b>:
<ul class="samplesList">
  <li>The base class for JPPF tasks was genericized and expanded into an interface/abstract class pattern. This results in <a href="/doc/v4/index.php?title=API_changes_in_JPPF_4.0">documented API changes</a>.
  A best effort was made to keep backward compatibility with JPPF 3.3, with a single and rare exception. The vast majority of existing applications will still run with the old deprecated APIs</li>
  <li>Exception handling: the <code>getException()</code> / <code>setException()</code> methods have been deprecated in favor of the more generic <code>getThrowable()</code> / <code>setThrowable()</code></li>
  <li>Tasks now have the native <a href="/doc/v4/index.php?title=Task_objects#Sending_notifications_from_a_task">ability to send notifications</a> to locally registered listeners, remote JMX listeners, or both</li>
</ul>

<p><b>Configuration</b>:
<ul class="samplesList">
  <li>The <a href="/doc/v4/index.php?title=Configuration_file_specification_and_lookup">configuration plugin API</a> was extended to enable reading the configuration from character streams (Java readers) in addition to byte streams</li>
  <li>Configuration sources can now <a href="/doc/v4/index.php?title=Includes_in_the_configuration">include other sources</a> at any level of nesting, to enhance the readability, modularity and maintenance of JPPF configuration files</li>
</ul>

<p><b>J2EE connector enhancements</b>:
<ul class="samplesList">
  <li>The J2EE connector client API was refactored to use the same code base as the standalone client</li>
  <li>The JPPF configuration can now be <a href="/doc/v4/index.php?title=How_to_use_the_connector_API#Reset_of_the_JPPF_client">updated dynamically without restarting the application server</a></li>
</ul>

<p><b>New and enhanced extension points</b>:
<ul class="samplesList">
  <li>A new <a href="/doc/v4/index.php?title=Specifying_alternate_serialization_schemes">serialization scheme API</a> was implemented, to enable integration with a broader range of serialization frameworks, while keeping backward compatibility with older serialization schemes.
  A <a href="/samples-pack/KryoSerializer/Readme.php">new sample</a> illustrates how the integration with the <a href="https://github.com/EsotericSoftware/kryo">Kryo</a> framework can result in a major performance improvement.</li>
  <li><a href="/doc/v4/index.php?title=Receiving_notifications_from_the_tasks">Listeners to task notifications</a> can now be registered with the nodes via the service provider interface</li>
  <li>A <a href="/doc/v4/index.php?title=JPPF_node_screensaver">screen saver</a> can now be associated with a node, for use in a volunteer computing model, to add meaningful animated graphics, or even just for fun</li>
</ul>

<p><b>New and enhanced samples</b>:
<ul class="samplesList">
  <li>The <a href="/samples-pack/Fractals/Readme.php">fractals generation sample</a> was enhanced to enable recording, replaying, saving and loading sets of points in the Mandelbrot space. This enables creating slide-shows of Mandlebrot images with just a few clicks.</li>
  <li>The new <a href="/samples-pack/FractalMovieGenerator/Readme.php">Mandelbrot.Movie@home</a> sample produces a full-fledged node distribution that is ready to install in a volunteer computing grid.
  The sample generates movies based on record sets produced by the <a href="/samples-pack/Fractals/Readme.php">fractals generation sample</a></li>
  <li>The <a href="/samples-pack/WordCount/Readme.php">Wikipedia word count sample</a> illustrates how JPPF can tackle big data and job streaming</li>
  <li>The new <a href="/samples-pack/KryoSerializer/Readme.php">Kryo serialization sample</a> demonstrates how to replace the default Java serialization with <a href="https://github.com/EsotericSoftware/kryo">Kryo</a></li>
</ul>

<p><b>Automated testing coverage</b>: Automated testing is a vital part of the JPPF development life cycle. Our automated testing framework creates small, but real, JPPF grids on the spot and uses the JPPF documented APIs to execute test cases based on JUnit.
<ul class="samplesList">
  <li>The range of automated test cases was broadened to include all major features, and most minor ones</li>
  <li>Various grid topologies are now included on demand in the tests, incuding single servers or multiple servers in P2P, offline nodes, etc., with and without SSL/TLS communication</li>
  <li>The J2EE connector is now automatically tested using scripts which download and install the application server, deploy the connector and test application, execute the JUnit-based tests and report the results</li>
</ul>

$template{name="press-footer"}$
$template{name="about-page-footer"}$
