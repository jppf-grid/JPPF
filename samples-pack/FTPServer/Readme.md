# FTP Server demo

<h3>What does the sample do?</h3>
<p>This sample illustrates how to embed an FTP server within a JPPF driver, and how to use a FTP client from a JPPF task to upload or download files as needed.
The FTP server implementation is <a href="http://mina.apache.org/ftpserver/">Apache's FtpServer</a>, and the FTP client is the one available in the <a href="http://commons.apache.org/net/">Apache Commons Net</a> library.

<h3>Related source files</h3>
<ul>
  <li><a href="target/tohtml/src/org/jppf/example/ftp/service/FTPServerStartup.java">FTPServerStartup.java</a> : This is a <a href="https://www.jppf.org/doc/6.3/index.php?title=JPPF_startup_classes#Server_startup_classes">JPPF driver startup class</a>, which starts an Apache Mina FTPd server instance at driver startup time</li>
  <li><a href="target/tohtml/src/org/jppf/example/ftp/service/CommandLineExt.java">CommandLineExt.java</a> : a utility class that reads the FTP server's configuration file and launches it</li>
  <li><a href="target/tohtml/src/org/jppf/example/ftp/service/FTPClientWrapper.java">FTPClientWrapper.java</a> : a wrapper around the Apache Commons-Net FTP client, as a convenience to easily upload and download files</li>
  <li><a href="target/tohtml/src/org/jppf/example/ftp/runner/FTPTask.java">FTPTask.java</a> : a JPPF task which downloads a file from the driver, processes it, then uploads a transformed file back to the server</li>
  <li><a href="target/tohtml/src/org/jppf/example/ftp/runner/FTPRunner.java">FTPRunner.java</a> : the client application that submits the task to the JPPF grid</li>
</ul>

<h3>How do I run the sample?</h3>
Before running this sample application, you need to install a JPPF driver and at least one node.<br>
For information on how to set up a node and server, please refer to the <a href="https://www.jppf.org/doc/6.3/index.php?title=Introduction">JPPF documentation</a>.<br>
Once you have installed a server and node, perform the following steps:
<ol>
  <li>open a command prompt in JPPF-x.y-samples-pack/FTPServer</li>
  <li>to build the deployment archives, type "<b>mvn clean install</b>" in a shell prompt in the "<b>FTPServer</b>" folder
  <li>this will generate a the "<b>FTPServer.zip</b>" and "<b>FTPServer.tar.gz</b>" files in the "<b>target</b>" folder, containing all required libraries and configuration files.
  You can use the archive file that is most appropriate for your platform</li>
  <li>the next step is to extract the archive file into the JPPF driver root installation folder</li>
  <li>you can then start the driver and nodes</li>
  <li>to run the demo application, you can either use the batch script "run.bat" (on Windows) or "run.sh" (on Linux), or the Ant script: "ant run"</li>
</ol>

<h3>I have additional questions and comments, where can I go?</h3>
<p>If you need more insight into the code of this demo, you can consult the Java source files located in the <b>FTPServer/src</b> folder.
<p>In addition, There are 2 privileged places you can go to:
<ul>
  <li><a href="https://www.jppf.org/forums">The JPPF Forums</a></li>
  <li><a href="https://www.jppf.org/doc/6.2">The JPPF documentation</a></li>
</ul>

