<?php $currentPage="Press" ?>
<?php $jppfVersion="3.2" ?>
$template{name="about-page-header" title="Press Kit"}$
$template{name="press-header"}$

<h3>Latest press release: JPPF <?php echo $jppfVersion ?></h3>

<p><b>Load balancing in the client</b>:
As for the server with the nodes, <a href="/doc/v3/index.php?title=Client_and_administration_console_configuration#Load-balancing_in_the_client">the client can now distribute jobs</a> over multiple server channels and the local execution channel.
Load-balancing / scheduling is applied in exactly the same way as within the server, with the same APIs and configuration properties and the ability to implement custom class loaders.

<p><b>Client-side SLA</b>:
All jobs now have a distinct <a href="/doc/v3/index.php?title=Job_Service_Level_Agreement">service level agreement</a> for the client-side dispatching of their tasks.

<p><b>Executor services enhancements</b>:
data providers can now be sent with the jobs submitted by the executor services, job listeners can be registered and completion listeners can be attached to the jobs.

<p><b>Class loading improvements</b>: 
new optimizations bring a new level of performance and resilience to the JPPF class loading mechanism

<p><b>IPv6 ready</b>:
All areas of JPPF networking have been updated to handle IPv6 addresses properly and transparently.

<p><b>Automated testing</b>:
A major effort has taken place to provide automated testing of the JPPF features, to ensure that the quality of each release meets the expectations.

$template{name="press-footer"}$
$template{name="about-page-footer"}$
