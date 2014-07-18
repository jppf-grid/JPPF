<?php $currentPage="Press" ?>
<?php $jppfVersion="4.2" ?>
$template{name="about-page-header" title="Press Kit"}$
$template{name="press-header"}$

<h3>Press release: JPPF <?php echo $jppfVersion ?></h3>

<p><b>Simplification of the client APIs</b>
<ul class="samplesList">
  <li><a href="/doc/v4/index.php?title=Dealing_with_jobs#Cancelling_a_job">Cancelling</a> a job, as well as <a href="/doc/v4/index.php?title=Dealing_with_jobs#Job_execution_results">getting or monitoring</a> its results can now be done from the job itself, in a much simpler way</li>
  <li>Consequently, the <a href="/api/index.html?org/jppf/client/event/TaskResultListener.html">TaskResultListener</a> API and its <a href="/api/index.html?org/jppf/client/JPPFResultCollector.html">JPPFResultCollector</a> implementation
  are now deprecated and superseded by the <a href="/doc/v4/index.php?title=Jobs_runtime_behavior,_recovery_and_failover#Job_lifecycle_notifications:_JobListener">job listeners</a> API</li>
  <li><a href="/api/index.html?org/jppf/client/JPPFJob.html">JPPFJob</a> now implements <a href="http://docs.oracle.com/javase/7/docs/api/index.html?java/util/concurrent/Future.html">Future&lt;List&lt;Task&lt;?&gt;&gt;&gt;</a></li>
</ul>

<p><b>Greater focus on connections pools</b>
<ul class="samplesList">
  <li>Each <a href="/doc/v4/index.php?title=Connection_pools">connection pool</a> now manages an associated dynamic <a href="/doc/v4/index.php?title=Connection_pools#Associated_JMX_connection_pool">pool of JMX connections</a>,
  instead of having one JMX connection per JPPF connection. This results in much less threads created on the client and server sides and increases scalability</li>
  <li>The <a href="/doc/v4/index.php?title=Connection_pools#The_JPPFConnectionPool_class">JPPFConnectionPool</a> API has been greatly enriched, for an easier and more powerful handling of its capabilities</li>
  <li>Similarly, the API to <a href="/doc/v4/index.php?title=Connection_pools#Exploring_the_connection_pools">explore connections pools</a> is now broader and more flexible</li>
</ul>

<p><b>Emphasis on submitting jobs concurrently</b>
<ul class="samplesList">
  <li>a <a href="/doc/v4/index.php?title=Submitting_multiple_jobs_concurrently">new section of the documentation</a> is dedicated to parallel job execution</li>
  <li>a new <a href="/samples-pack/ConcurrentJobs/Readme.php">dedicated sample</a> illustrates the patterns explored in the documentation</li>
</ul>

<p><b>New execution policies</b>
<ul class="samplesList">
  <li>The new <a href="/doc/v4/index.php?title=Execution_Policy_Elements#IsInIPv4Subnet">IsInIPv4Subnew</a> policy filters nodes based on their membership in one or more IPv4 subnets</li>
  <li>The <a href="/doc/v4/index.php?title=Execution_Policy_Elements#IsInIPv6Subnet">IsInIPv6Subnet</a> policy plays the same role for IPv6 addresses</li>
</ul>

<p><b>Control of tasks resubmmission</b>
<p>The maximum number of times a task can schedule itself for resubmission is now configurable at the <a href="/doc/v4/index.php?title=Task_objects#Resubmitting_a_task">task level</a>, in addition to the job level.

<p><b>Tutorial updates</b>
<p>The <a href="/doc/v4/index.php?title=A_first_taste_of_JPPF">JPPF tutorial</a> was updated to account for the greatest and latest features.

$template{name="press-footer"}$
$template{name="about-page-footer"}$
