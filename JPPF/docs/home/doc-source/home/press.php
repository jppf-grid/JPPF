<?php $currentPage="Press" ?>
$template{name="about-page-header" title="Press Kit"}$
$template{name="press-header"}$

	<h3>Latest press release: JPPF 2.2</h3>

	<p>In this version, many improvements and fixes increase the reliability, scalability and ease of use of JPPF.
	For the full list and details of the new features in JPPF 2.2, do not hesitate to read the <a href="release_notes.php?version=2.2">JPPF 2.2 release notes</a>.

	<p><b>JVM Support</b>: as of version 2.2, JPPF is dropping support for the JDK 1.5. Only JDK 1.6 and later are supported.

	<p><b>JPPF Executor Service</b>: this new API provides an executor service facade for the JPPF client.<br/>
	It implements all the functionality specified in <a href="http://java.sun.com/javase/6/docs/api/index.html?java/util/concurrent/ExecutorService.html" target="_blank">java.util.concurrent.ExecutorService</a>

	<p><b>Administration console</b>:<br/>
	- A new status bar indicates the number of currently connected servers and nodes<br>
	- The topology view includes two new buttons in the toolbar to select all servers or all nodes at once<br/>
	- Multiple bugs have been fixed, making the console mmore robust and reliable than ever<br>

	<p><b>Connectors</b>:<br/>
	- The J2EE connector now has the ability to connect to multiple servers and automatically discover servers on the network<br/>
	- The Gigaspace connector was upgraded to support Gigaspaces XAP 7.1<br/>
	- The Tomcat connector was upgraded to support Tomcat 7.0.0 beta<br/>
	- All three connectors have seen their demo web application revamped for a more enjoyable experience<br/>

	<p><b>Job SLA</b>: JPPF jobs can now be set to expire at a specified date or after a specified amount of time<br/>

	<p><b>Samples</b>: the network data encryption sample was upgraded to offer a much more secure solution.

$template{name="press-footer"}$
$template{name="about-page-footer"}$
