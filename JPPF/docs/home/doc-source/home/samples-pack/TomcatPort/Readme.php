<?php $currentPage="Samples" ?>
$template{name="samples-page-header" title="Tomcat Port of the JPPF Client"}$

<div align="justify">

					<h3>What does the sample do?</h3>
					This sample is a simple demonstration of a JPPF client embedded in a web application and running within a Tomcat container.<br>
					It has been tested with <a href="http://tomcat.apache.org">Apache Tomcat</a> versions 5.5 and 6.0.

					<h3>How do I run it?</h3>
					Before running this sample application, you must have a JPPF server and at least one node running.<br>
					For information on how to set up a node and server, please refer to the <a href="http://www.jppf.org/doc/v3/index.php?title=Introduction">JPPF documentation</a>.<br>
					Once you have a server and node, from a command prompt, perform the following steps:
					<ul class="samplesList">
						<li>configure the application settings, by editing the configuration files (JPPF and logging) in the <b>TomcatPort/src/java</b> folder</li>
						<li>from a shell or command prompt, type: <b>&quot;ant build&quot;</b>. This will build a WAR file that embeds a JPPF client, in the <b>TomcatPort/build</b> folder.</li>
						<li>deploy the WAR file to Tomcat (usually by copying it to TOMCAT_HOME/webapps)</li>
						<li>start Tomcat</li>
					</ul>

					<h3>How do I use it?</h3>
					To use this sample, open a browser at this URL: http://<i>tomcat_host</i>:<i>tomcat_port</i>/jppftest<br>
					If your Tomcat installation is local and uses the default port, you can go directly to this URL: <a href="http://localhost:8080/jppftest" target="_blank">http://localhost:8080/jppftest</a>

					<h3>What integration features of JPPF are demonstrated?</h3>
					Integration with the Tomcat Servlets/JSP container.

					<h3>I have additional questions and comments, where can I go?</h3>
					<p>If you need more insight into the code of this demo, you can consult the JSP source files located in the <b>TomcatPort/src/resources</b> folder.
					<p>In addition, There are 2 privileged places you can go to:
					<ul class="samplesList">
						<li><a href="http://www.jppf.org/forums"/>The JPPF Forums</a></li>
						<li><a href="http://www.jppf.org/wiki">The JPPF documentation</a></li>
					</ul>
					
</div>

$template{name="about-page-footer"}$
