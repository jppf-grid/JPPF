<?php $currentPage="Press" ?>
$template{name="about-page-header" title="Press Kit"}$
$template{name="press-header"}$

	<h3>Latest press release: JPPF 2.3</h3>

	<p>In this version, new major features raise JPPF to a new level of resilience, reliability and flexibility
	For the full list and details of the new features in JPPF 2.3, do not hesitate to read the <a href="release_notes.php?version=2.3">JPPF 2.3 release notes</a>.

	<p><b>Idle host scavenging</b>

	<p>JPPF now takes advantage of computers on which no user activity is occurring.<br/>
	It is very easy to configure a JPPF node to start when no keyboard or mouse activity has occurred for a specified time, and stop upon any new activity from the user.

	<p><b>Job scheduling enhancements</b>

	<p>The scheduling mechanism has been improved to provide more fairness among concurrent jobs with the same priority.<br/>

	<p><b>Remote loggers</b>

	<p>Logging traces are now available as JMX notifications via the JPPF management APIs.<br/>
	Developers can now receive, display and store traces from nodes and servers in a single location.

	<p><b>Detection and recovery from hardware failures of servers and nodes</b>

	<p>A long-awaited new mechanism enables the detection of hardware failures of a node or server, allowing recovery in a resonable time frame.<br/>
	This brings a new level of resilience and reliability to the JPPF grid.

	<p><b>In-VM nodes</b>

	<p>It is now possible to configure a node to run in the same JVM as a JPPF server, via a single on/off switch.

$template{name="press-footer"}$
$template{name="about-page-footer"}$
