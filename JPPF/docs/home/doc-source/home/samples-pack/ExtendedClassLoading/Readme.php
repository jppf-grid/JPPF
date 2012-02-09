<?php $currentPage="Samples" ?>
$template{name="samples-page-header" title="Extended Class Loading sample"}$

<div align="justify">

					<h4 style="margin: 0px">Quick navigation</h4>
					<table border="0" cellspacing="4px">
						<tr><td><a href="#1">1. What does the sample do?</a>&nbsp;</td><td><a href="#5">5. Source files</a></td></tr>
						<tr><td><a href="#2">2. Description of the problem</a>&nbsp;</td><td><a href="#6">6. Features demonstrated</a></td></tr>
						<tr><td><a href="#3">3. Description of the solution</a>&nbsp;</td><td><a href="#7">7. Help and support</a></td></tr>
						<tr><td><a href="#4">4. How do I run it?</a>&nbsp;</td><td></td></tr>
					</table>

					<a name="1"></a>
					<h3>What does the sample do?</h3>
					This sample uses the JPPF class loading extensions to automate the management of a repository of application libraries at runtime, for all the nodes in the grid.

					<a name="2"></a>
					<h3>Description of the problem to solve</h3>
					<p>Some applications require a large number of internal or external libraries to run.
					When executed in a JPPF grid, they may incur a significant startup time, due to the loading of a very large number of classes across the network, which is the way JPPF works by default.
					Futhermore, this startup overhead may occur every time a change occurs, not only in the application's code but also in any of the libraries it relies on.

					<p>A solution to the startup time issue is to deploy the libraries locally on each node.
					However, this causes a management and deployment overhead, when one or more of the libraries is added, updated or removed.
					When the number of nodes in the grid is large, the overhead of managing the libraries can be prohibitive.

					<a name="3"></a>
					<h3>Description of the solution</h3>
					<p>In this sample, we implement a mechanism that will automatically detect libraries that were added, updated or removed on the client side.
					This information is communicated to the nodes by annotating a JPPF job with <a href="http://www.jppf.org/doc/v3/index.php?title=Job_Metadata">metadata</a> describing the changes in the repository of libraries.
					There will be two kinds of repositories: one that is on the client side and maintained by the users, the other one local to each node, which will be automatically updated by comparing vith the job metadata.

					<p>To be able to determine if a library was updated, each library is associated with a signature, such as an MD5 of SHA-256 signature.
					The association between library files and signatures is maintained and stored in an index file in the repository, by default called "<tt>index.txt</tt>".
					Additionally, each library file on the node side will be suffixed with its corresponding signature in hexadecimal representation.
					For instance, a file "<tt>MyLibrary.jar</tt>" will be named "<tt>MyLibrary-B48D97023F956A93282DB8B12C47443B.jar</tt>".
					This "trick" is necessary since the files are added to the node's classpath, and the JVM may be holding a lock on them (especially on Windows systems), thus preventing us from overwriting them.
					For the same reason, it may not be possible to immediately delete a file that was removed from the repository.
					Thus, the files to delete are stored in another text file "<tt>toDelete.txt</tt>", so they can be deleted the next time the node is restarted.

          <p>On the node, we use a <a href="http://www.jppf.org/doc/v3/index.php?title=Receiving_notifications_of_node_life_cycle_events">node life cycle listener</a> to perform the maintenance operations on the repository:
          <ul class="samplesList">
          	<li>On "node starting" events, the node  deletes the files listed in the "<tt>toDelete.txt</tt>" file, and adds the libraries listed in the "<tt>index.txt</tt>" file to the classpath</li>
          	<li>On "node ending" events, the node simply stores the latest state of the "<tt>toDelete.txt</tt>" file</li>
          	<li>On "job starting" events, the node reads the metadata associated with the jobs, and compares them with the content of the repository.
          	It will then download the new or updated libraries from the client and store them in its repository, updating the repository's index at the same time.<br/>
          	It will also add  the new libraries to its classpath. It doesn't make sense to do that with updated libraries, because the old version is already in the classpath.
          	Thus, old versions are scheduled for deletion at the next startup, by adding them to the "<tt>toDelete.txt</tt>" file.<br/>
          	At this point, if there are any changes to the repository, the node will save both the index file and the "toDelete" file.<br/>
          	Finally, the node will check for a "node restart" flag in the job metadata.
          	If the flag is true, then the job is cancelled, causing it to be requeued on the server, and the node is immediately restarted so the changes to the classpath can be taken into account.</li>
          	<li>On "job ending" events, there is no specific processing taking place.</li>
          </ul>

          <p>On the client side, we have a mechanism that scans the repository, and compares the scan result with the content of the index file, to determine which libraries were added, updated or removed.
          This information is then added to a job's metadata, and the job is submitted to the JPPF grid.
          When the job is dispatched to a node, the node will then be able to process the metadata to update its own repository and classpath.

          <p>The repository is a simple flat folder that contains jar files. This folder is added to the JPPF client's classpath, but <i>not the jar files it contains</i>.
          Using this structure, it is very easy to update the repository: simply drop a jar file into the repository folder, or remove one from the folder, and all the rest is automated.

					<a name="4"></a>
					<h3>How do I run it?</h3>
					Before running this sample, you need to install a JPPF server and at least one node.<br>
					For information on how to set up a node and server, please refer to the <a href="http://www.jppf.org/doc/v3/index.php?title=Introduction">JPPF documentation</a>.<br>
					Once you have installed a server and node, perform the following steps:
					<ol class="samplesList">
						<li>open a command prompt or shell console in JPPF-x.y-samples-pack/ExtendedClassLoading</li>
						<li>build the sample: type "<b>ant jar</b>" or simply "<b>ant</b>"; this will create a 3 jar files:<br/>
							<ul class="samplesNestedList">
								<li><b>NodeListener.jar</b> in this sample's root folder. This is our node life cycle listener implementation</li>
								<li><b>ClientLib1.jar</b> and <b>ClientLib2.jar</b> in the "<b>dynamicLibs</b>" folder (this is the client's repository).
								These are here for demonstration purposes. Each of these libraries contains a single class used by the JPPF task in the submitted job.
								When running this sample the first time, these classes will initially not be in the classpath of either the client or the node.
								However, our repository management mechanism will automatically download these libraries to the node, so the task can be executed without error.</li>
							</ul>
						</li>
						<li>copy "NodeListener.jar" in the "lib" folder of the JPPF driver installation, to add it to the driver's classpath. This will cause the nodes to download its classes from the server.</li>
						<li>start the server and the node</li>
						<li>from the command prompt previously opened, run the sample by typing "ant run"</li>
						<li>in the client's console, you should see the following messages displayed (ignoring the driver connection messages):
<pre class="samples">found 2 new or updated libraries
  - NEW   : ClientLib2.jar, signature = 651DC2B98EAEFD159755786BDB5DF316
  - NEW   : ClientLib1.jar, signature = 0010A74AEF47E32B138CA842893679DD
there are no deleted libraries
restart node flag set to false
...
Result: Successful execution</pre>
						</li>
						<li>in the node's console, you should see the following messages displayed:
<pre class="samples">processing metadata for job 'Extended Class Loading'
  found 2 libraries to update:
  - NEW   : ClientLib2.jar, signature = 651DC2B98EAEFD159755786BDB5DF316
  - NEW   : ClientLib1.jar, signature = 0010A74AEF47E32B138CA842893679DD
  no library to remove
Hello from class 1 loaded from the client
Hello from class 2 loaded from the client</pre>
						</li>
						<li>Now remove the file "ClientLib1.jar" from the "dynamicLibs" folder</li>
						<li>Run the sample again, but this time asking that the node be restarted:<br/>type the command "<tt>ant -Drestart.node=true run</tt>"</li>
						<li>This time you will see the following in the client's console:
<pre class="samples">there are no new or updated libraries
found 1 deleted library
  - ClientLib1.jar
restart node flag set to true
Got exception: org.jppf.JPPFException:
  java.lang.NoClassDefFoundError: org/jppf/.../MyClientDynamicClass1
... stack trace ...</pre>
						</li>
						<li>and in the node:
<pre class="samples">processing metadata for job 'Extended Class Loading'
  no library updates found
  found 1 libraries to delete:
  - ClientLib1.jar
canceling the job
*** restarting this node ***
node process id: 1448
[ ... connection to the server ...]
found 1 library in the store:
  added ClientLib2.jar to the classpath
Node successfully initialized
processing metadata for job 'Extended Class Loading'
  no library updates found</pre>
						</li>
						<li>What happened here? From the messages on the node and client side, we can see the following:
							<ul class="samplesNestedList">
								<li>upon receiving the job, the node detects that "ClientLib1.jar" is to be deleted from its repository</li>
								<li>after performing this update, the job is cancelled and the node restarted as requested</li>
								<li>upon starting again, the node adds the libraries found in its repository to the classpath: only ClientLib2.jar is left</li>
								<li>We don't see any message from the task executing, because an exception occurred</li>
								<li>the client reports the exception, caused by the fact that "MyClientDynamicClass1" was not in the classpath anymore</li>
							</ul>
						</li>
						<li>Now, you can continue experimenting with the content of the client's repository. You may re-create or update the ClientLib1.jar and ClientLib2.jar libraries by running the corresponding ant scripts: "ant jar.1" or "ant jar.2".
						You may also play with the "restart node" flag to see how the changes take effect. You might also want to try adding <i>any</i> other jar file to the client's repository.</li>
					</ol>

					<a name="5"></a>
					<h3>Commented source files</h3>
					<ul class="samplesList">
						<li><a href="src/org/jppf/example/extendedclassloading/LibraryManager.java.html">LibraryManager.java</a>: This is the utility class which performs the repository management operations</li>
						<li><a href="src/org/jppf/example/extendedclassloading/node/NodeListener.java.html">NodeListener.java</a>: our node life cycle listener implementation, which uses the LibraryManager</li>
						<li><a href="src/org/jppf/example/extendedclassloading/client/MyRunner.java.html">MyRunner.java</a>: the JPPF client application, which uses the LibraryManager</li>
						<li><a href="src/org/jppf/example/extendedclassloading/client/MyTask.java.html">MyTask.java</a>: a sample JPPF task which explicitely loads classes dynamically</li>
					</ul>

					<a name="6"></a>
					<h3>What features of JPPF are demonstrated?</h3>
					<ul class="samplesList">
						<li><a href="http://www.jppf.org/doc/v3/index.php?title=Class_loading_in_JPPF#JPPF_class_loading_extensions">Class loading extensions</a></li>
						<li><a href="http://www.jppf.org/doc/v3/index.php?title=Receiving_notifications_of_node_life_cycle_events">Receiving notifications of node life cycle events</a></li>
						<li><a href="http://www.jppf.org/doc/v3/index.php?title=Job_Metadata">Job metadata</a></li>
					</ul>

					<a name="7"></a>
					<h3>I have additional questions and comments, where can I go?</h3>
					<p>If you need more insight into the code of this demo, you can consult the Java source files located in the <b>ExtendedClassLoading/src</b> folder.
					<p>In addition, There are 2 privileged places you can go to:
					<ul>
						<li><a href="http://www.jppf.org/forums"/>The JPPF Forums</a></li>
						<li><a href="http://www.jppf.org/wiki">The JPPF documentation</a></li>
					</ul>
					
</div>

$template{name="about-page-footer"}$
