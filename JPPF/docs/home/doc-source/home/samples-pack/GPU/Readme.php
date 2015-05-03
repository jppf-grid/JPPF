<?php $currentPage="Samples" ?>
$template{name="samples-page-header" title="GPU computation sample"}$

<div align="justify">

					<h3>What does the sample do?</h3>
					This sample performs the multplication of 2 square dense matrices on a GPU.
					The GPU computation is handled thanks to the <a href="http://code.google.com/p/aparapi/">APARAPI</a> library, which provides Java bindings for <a href="http://www.khronos.org/opencl/">OpenCL</a>.

					<h3>How does it work?</h3>
					This sample submits a JPPF job with one or more tasks to be executed on a GPU.
					The tasks contain APARAPI-conformant code, whose bytecode is introspected at runtime to generate OpenCL code (see example <a href="src/org/jppf/example/aparapi/GeneratedOpenCL.c">here</a>).
					This generated code is then compiled and executed on an OpenCL device if any is available.

					<h3>How do I run it?</h3>
					Before running this sample application, you must have a JPPF server and at least one node running.<br>
					For information on how to set up a node and server, please refer to the <a href="http://www.jppf.org/doc/v4">JPPF documentation</a>.<br>

					<p>The node will require some additional configuration. In effect, since the APARAPI library load a native library, the file "aparapi.jar" must be added directly to the node's classpath.
					If you simply keep it in the client's classpath, the node will attempt to load it for each distinct client. This will only work the first time, and fail on subsequent attempts.<br/>

					<p>For your convenience, we have included a set of files that will take care of this:
					<ul class="samplesList">
						<li>copy the file <b>jppf-node.properties</b> in <b>GPU/config/node</b> to your node's <b>config/</b> folder to replace the existing config file with the new version.
						You will notice that this configuration file has APARAPI-specific settings in the <b>jppf.jvm.options</b> property</li>
						<li>copy the file <b>GPU/lib/aparapi.jar</b> to your node's <b>lib/</b> folder</li>
						<li>copy the appropriate native library from <b>GPU/lib/</b> for your platform to the node's <b>lib/</b> folder:
							<ul class="samplesList">
								<li><b>aparapi_x86.dll</b> or <b>aparapi_x86_64.dll</b> for Windows 32/64 bits platforms</li>
								<li><b>libaparapi_x86.so</b> or <b>libaparapi_x86_64.so</b> for Linux 32/64 bits</li>
								<li><b>libaparapi_x86_64.dylib</b> for 64 bits Mac OS</li>
							</ul>
						</li>
						<li>Once this is done, you can start the server and node, then run the sample by typing "run.bat" on Windows or "./run.sh" on Linux/Unix</li>
						<li>During the execution, the node will print out a message indicating whether the task was actually executed on a GPU, plus additional information on the OpenCL devices available to the platform</li>
						<li>if the task cannot be executed on a GPU, if will fall back to executing in a Java thread pool (i.e. CPU-bound)
					</ul>

					<h3>How do I use it?</h3>
					<p>This sample doesn't have a graphical user interface, however you can modify some of the parameters in the JPPF configuration file:
					<ol>
						<li>open the file "<b>config/jppf-client.properties</b>" in a text editor</li>
						<li>at the end of the file, you will see the following properties:
<pre class="samples"><font color="green"># number of jobs to submit in sequence</font>
iterations = 10
<font color="green"># number of tasks in each job</font>
tasksPerJob = 1
<font color="green"># the size of the matrices to multiply</font>
matrixSize = 1500
<font color="green"># execution mode, either GPU or JTP (Java Thread Pool)</font>
execMode = GPU</pre>
						</li>
						<li>You can experiment with various values, for instance to find out the JTP vs. GPU execution speedup.
						You may find out that for relatively small values of the matrix size there is no speedup, due to the overhead of generating and compiling the OpenCL code</li>
					</ol>

					<h3>Sample's source files</h3>
					<ul class="samplesList">
						<li><a href="src/org/jppf/example/aparapi/MatrixKernel.java.html">MatrixKernel.java</a>: this is the class that will be translated into OpenCL code</li>
						<li><a href="src/org/jppf/example/aparapi/GeneratedOpenCL.c">GeneratedOpenCL.c</a>: the OpenCL code generated from MatrixKernel which will be actually executed on the GPU</li>
						<li><a href="src/org/jppf/example/aparapi/AparapiTask.java.html">AparapiTask.java</a>: the JPPF task which invokes the GPU bindings API</li>
						<li><a href="src/org/jppf/example/aparapi/AparapiRunner.java.html">AparapiRunner.java</a>: the JPPF client application which submits the jobs to the grid</li>
						<li><a href="src/org/jppf/example/aparapi/SquareMatrix.java.html">SquareMatrix.java</a>: a simple representation of a square dense matrix, whose values are stored in a one-dimensional float array</li>
					</ul>

					<h3>How can I build the sample?</h3>
					To compile the source code, from a command prompt, type: <b>&quot;ant compile&quot;</b><br>
					To generate the Javadoc, from a command prompt, type: <b>&quot;ant javadoc&quot;</b>

					<h3>I have additional questions and comments, where can I go?</h3>
					<p>If you need more insight into the code of this demo, you can consult the source, or have a look at the
					<a href="javadoc/index.html">API documentation</a>.
					<p>In addition, There are 2 privileged places you can go to:
					<ul>
						<li><a href="http://www.jppf.org/forums"/>The JPPF Forums</a></li>
						<li><a href="http://www.jppf.org/wiki">The JPPF documentation</a></li>
					</ul>
					
</div>

$template{name="about-page-footer"}$
