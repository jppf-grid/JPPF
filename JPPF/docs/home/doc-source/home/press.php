<?php $currentPage="Press" ?>
<?php $jppfVersion="3.1" ?>
$template{name="about-page-header" title="Press Kit"}$
$template{name="press-header"}$

<h3>Latest press release: JPPF <?php echo $jppfVersion ?></h3>

<p align="justify"><b>Security</b>: JPPF 3.1 brings a new security layer by performing all <a href="http://www.jppf.org/doc/v3/index.php?title=Configuring_SSL/TLS_communications">network communications through SSL/TLS</a>, providing data encryption, data integrity and certificate-based authentication.

<p align="justify"><b>Management and monitoring</b>: this version adds a new graph view of the grid topology to the administration console, the ability to <a href="http://www.jppf.org/doc/v3/index.php?title=Server_management#Testing_an_execution_policy">test execution policies</a> against the current grid state,
and the possibility to cancel jobs directly from a standalone JPPF client or from the J2EE connector.

<p align="justify"><b>Executor services</b>: Job SLAs and metadata, along with task timeout and cancel handlers, can now be <a href="http://www.jppf.org/doc/v3/index.php?title=JPPF_Executor_Services#Configuring_jobs_and_tasks">dynamically configured</a>.

<p align="justify"><b>Clients</b>: the load balancing between local and remote execution is now <a href="http://www.jppf.org/doc/v3/index.php?title=Client_and_administration_console_configuration#Load-balancing_in_the_client">fully configurable</a>.

<p><b>Nodes</b>: the execution model can now be switched to a <a href="http://www.jppf.org/doc/v3/index.php?title=Fork/Join_thread_pool_in_the_nodes">local fork/join model</a>.

$template{name="press-footer"}$
$template{name="about-page-footer"}$
