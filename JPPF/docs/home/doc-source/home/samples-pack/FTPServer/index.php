$template{name="sample-readme-html-header" title="FTP Server demo"}$
<h3>What does the sample do?</h3>
<p>This sample illustrates how to embed an FTP server within a JPPF driver, and how to use a FTP client from a JPPF task to upload or download files as needed.
The FTP server implementation is <a href="http://mina.apache.org/ftpserver/">Apache's FtpServer</a>, and the FTP client is the one available in the <a href="http://commons.apache.org/net/">Apache Commons Net</a> library.

<h3>Related source files</h3>
<ul>
  <li><a href="src/org/jppf/example/ftp/service/FTPServerStartup.java.html">FTPServerStartup.java</a> : This is a <a href="http://www.jppf.org/doc/5.2/index.php?title=JPPF_startup_classes#Server_startup_classes">JPPF driver startup class</a>, which starts an Apache Mina FTPd server instance at driver startup time</li>
  <li><a href="src/org/jppf/example/ftp/service/CommandLineExt.java.html">CommandLineExt.java</a> : a utility class that reads the FTP server's configuration file and launches it</li>
  <li><a href="src/org/jppf/example/ftp/service/FTPClientWrapper.java.html">FTPClientWrapper.java</a> : a wrapper around the Apache Commons-Net FTP client, as a convenience to easily upload and download files</li>
  <li><a href="src/org/jppf/example/ftp/runner/FTPTask.java.html">FTPTask.java</a> : a JPPF task which downloads a file from the driver, processes it, then uploads a transformed file back to the server</li>
  <li><a href="src/org/jppf/example/ftp/runner/FTPRunner.java.html">FTPRunner.java</a> : the client application that submits the task to the JPPF grid</li>
</ul>

<h3>How do I run the sample?</h3>
Before running this sample application, you need to install a JPPF driver and at least one node.<br>
For information on how to set up a node and server, please refer to the <a href="http://www.jppf.org/doc/5.2/index.php?title=Introduction">JPPF documentation</a>.<br>
Once you have installed a server and node, perform the following steps:
<ol>
  <li>open a command prompt in JPPF-x.y-samples-pack/FTPServer</li>
  <li>to build a deployment archive, type:<br/>
  - "<b>ant zip</b>"; this will generate a zip file with all required libraries and configuration files<br/>
  - "<b>ant tar.gz</b>"; this will generate an equivalent tar.gz</li>
  <li>the next step is to extract the archive file into the JPPF driver root installation folder</li>
  <li>you can then start the driver and nodes</li>
  <li>to run the demo application, you can either use the batch script "run.bat" (on Windows) or "run.sh" (on Linux), or the Ant script: "ant run"</li>
</ol>

<h3>I have additional questions and comments, where can I go?</h3>
<p>If you need more insight into the code of this demo, you can consult the Java source files located in the <b>FTPServer/src</b> folder.
<p>In addition, There are 2 privileged places you can go to:
<ul>
  <li><a href="http://www.jppf.org/forums"/>The JPPF Forums</a></li>
  <li><a href="http://www.jppf.org/doc/5.2">The JPPF documentation</a></li>
</ul>
$template{name="sample-readme-html-footer"}$
