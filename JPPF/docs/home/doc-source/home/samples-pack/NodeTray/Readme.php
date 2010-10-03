<?php $currentPage="Samples" ?>
$template{name="samples-page-header" title="Idle Mode for JPPF Nodes"}$

<h3>What does the sample do?</h3>
<p>This sample is an implementation of a JPPF API that allows a node to start when the machine on which it is running is idle, and to stop when the machine is busy again.
By idle we mean that no keyboard or mouse input has occurred for a specified time.

<h3>How does it work?</h3>
<p>This implementation uses the <a href="https://jna.dev.java.net/">JNA library</a>, which provides Java wrappers around native calls to the operating system.
This allows us to query the system for how long it has been idle, and make decisions to start or stop the node based on that.

<h3>Related source files</h3>
<ul>
	<li><a href="src/org/jppf/example/idleSystem/IdleTimeDetectorFactoryImpl.java.html">IdleTimeDetectorFactoryImpl.java</a> : this is the factory class that instantiates the platform-specific object detecting that the system is idle</li>
	<li><a href="src/org/jppf/example/idleSystem/X11IdleTimeDetector.java.html">X11IdleTimeDetector.java</a> : detects when an X11 (Linux, Unix, etc...) system is idle</li>
	<li><a href="src/org/jppf/example/idleSystem/WindowsIdleTimeDetector.java.html">WindowsIdleTimeDetector.java</a> : detects when a Windows-based system is idle</li>
	<li><a href="src/org/jppf/example/idleSystem/MacIdleTimeDetector.java.html">MacIdleTimeDetector.java</a> : detects when an Apple Mac system is idle (untested)</li>
</ul>

<h3>How do I use it?</h3>
First you need to build the sample jar file. To do this, perform the following steps:

<ol>
	<li>open a command prompt or shell console in <b>JPPF-x.y.z-samples-pack/IdleSystem</b></li>
	<li>run the build script: "<b>ant jar</b>", or simply "<b>ant</b>". This will download 2 jar files "<b>jna.jar</b>" and "<b>platform.jar</b>" and create a third one "<b>IdleSystem.jar</b>", into the <b>IdleSystem/lib</b> directory.</li>
	<li>when this is done, copy the 3 jar files in <b>IdleSystem/lib</b> into your node's library directory "<b>JPPF-x.y.z-node/lib</b>"</li>
	<li>to configure the node to run in idle mode, open the file "<b>JPPF-x.y.z-node/config/jppf-node.properties</b>" in a text editor and create or edit the following properties:
		<ul>
			<li><b>jppf.idle.mode.enabled = true</b> to enable the idle mode</li>
			<li><b>jppf.idle.timeout = 6000</b> to configure the time of keyboard and mouse inactivity to consider the node idle, expressed in milliseconds</li>
			<li><b>jppf.idle.poll.interval = 1000</b> to configure how often the node will check for inactivity, in milliseconds</li>
			<li><b>jppf.idle.detector.factory = org.jppf.example.idlesystem.IdleTimeDetectorFactoryImpl</b> please do not change this!</li>
		</ul>
	</li>
	<li>when this is all done, you can start the node and it will only run when the system has been idle for the configured time, and will stop as soon as any keyboard or mouse input occurs</li>
</ol>

<h3>What features of JPPF are demonstrated?</h3>
Abilitiy to run a node when the host is idle.

<h3>I have additional questions and comments, where can I go?</h3>
<p>There are 2 privileged places you can go to:
<ul>
	<li><a href="http://www.jppf.org/forums"/>The JPPF Forums</a></li>
	<li><a href="http://www.jppf.org/wiki">The JPPF documentation</a></li>
</ul>

$template{name="about-page-footer"}$
