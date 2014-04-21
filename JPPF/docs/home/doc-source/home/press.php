<?php $currentPage="Press" ?>
<?php $jppfVersion="4.1" ?>
$template{name="about-page-header" title="Press Kit"}$
$template{name="press-header"}$

<h3>Press release: JPPF <?php echo $jppfVersion ?></h3>

<p><b>Node provisioning</b>: not enough nodes in your grid when the workload peaks? Start new nodes on demand wih the <a href="/doc/v4/index.php?title=Node_provisioning">node provisioning facility</a>!
<ul class="samplesList">
  <li>grow or shrink your JPPF grid dynamically</li>
  <li>accessible via API and the administration console</li>
  <li>see it in action with the <a href="/samples-pack/AdaptiveGrid/Readme.php">Adaptive Grid</a> example</li>
</ul>

<p><b>Customizable node connection strategies</b>: define which server your nodes will connect and failover to with the <a href="/doc/v4/index.php?title=Defining_the_node_connection_strategy">connection strategy API</a>.
A <a href="/doc/v4/index.php?title=Defining_the_node_connection_strategy#Built-in_strategies">built-in implementation</a> is provided, which relies on a CSV file of server
definitions and fails over to the configuration-based default when no server is available.

<p><b>Client connection pools</b>:
<ul class="samplesList">
  <li>A <a href="/doc/v4/index.php?title=Connection_pools">new client API</a> enables exploring and growing or shrinking client to server connections dynamically</li>
  <li>See it in action in the <a href="/samples-pack/AdaptiveGrid/Readme.php">Adaptive Grid</a> example</li>
  <li>Connection pools of auto-discovered servers, as well as the local executor, can now be assigned a priority</li>
  <li>Connection failover based on the priority of the server connections was integrated back into the core client functionality. The <a href="/doc/v4/index.php?title=The_ClientWithFailover_wrapper_class">ClientWithFailover</a>
  feature was deprecated accordingly</li>
</ul>

<p><b>Powerful configuration enhancements</b>:
<ul class="samplesList">
  <li><a href="/doc/v4/index.php?title=Includes,_substitutions_and_scripted_values_in_the_configuration#Substitutions_in_the_values_of_properties">Variable substitutions</a>
  for property values: the syntax ${property} can be used anywhere in the configuration files</li>
  <li>Similarly, the ${env.VARIABLE} syntax enables environment variables substitution in configuration files</li>
  <li><a href="/doc/v4/index.php?title=Includes,_substitutions_and_scripted_values_in_the_configuration#Scripted_property_values">Scripting of configuration properties</a>:
  the property values can now be defined as an expression in any <a href="https://www.jcp.org/aboutJava/communityprocess/pr/jsr223/">JSR 223</a>-compliant dynamic script language</li>
</ul>

<p><b>On-demand task resubmission</b>: any JPPF task can now <a href="/doc/v4/index.php?title=Task_objects#Resubmitting_a_task">schedule itself for re-submission</a>.

<p><b>Capture of nodes and servers console output to files</b>: The output of the JPPF nodes and server processes can now be <a href="/doc/v4/index.php?title=Configuring_a_JPPF_server#Redirecting_the_console_output">redirected to files</a>, for later retrieval and analysis.

<a name="_clientQueue"></a>
<p><b>New client job queue listener API</b>: a <a href="/doc/v4/index.php?title=Notifications_of_client_job_queue_events">new client API</a> allows client applications to receive notifications of jobs added to or removed from the job queue.

<p><b>Documentation improvements</b>:
<ul class="samplesList">
  <li>A new section "<a href="/doc/v4/index.php?title=Putting_it_all_together">Putting it all together</a>" was added to the <a href="/doc/v4/index.php?title=Configuration_guide">configuration guide</a>.</li>
  <li>The <a href="/doc/v4/index.php?title=A_first_taste_of_JPPF">JPPF tutorial</a> was updated to reflect the greatest and latest functionalities</li>
</ul>

$template{name="press-footer"}$
$template{name="about-page-footer"}$
