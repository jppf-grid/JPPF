<?php $currentPage="Samples" ?>
$template{name="samples-page-header" title="Word Count sample"}$

<div align="justify">

          <h3>What does the sample do?</h3>
          This sample performs a word count on a full or partial Wikipedia database. It illustrates how JPPF can be used to process large datasets in a very efficient way.
          The actual processing is vaguely similar to a mpa/reduce process, which is not surprising given what we are trying to accomplish.

          <p>The actual processing is as follows:
          <ol class="samplesList">
            <li>The client application reads the wikipedia file line by line and generates articles. Each article is delimited by the XML tags &lt;page&gt; and &lt;/page&gt;.
            Furthermore, we only look for the text of each article (we ignore metadata), so within each &lt;page&gt; we only keep the part delimited by &lt;text ...&gt; and &lt;/text&gt;.</li>
            <li>Full articles are then grouped into JPPF tasks, and then tasks into JPPF jobs. This results in a constant stream of jobs until all articles are read.</li>
            <li>Each generated job is offered to a submit queue. This queue has a limited capacity, to avoid an explosion of the memory footprint in case jobs are created faster than they are processed:
            when the capacity is reached, the next job submission - and reading of articles - is blocked until a slot becomes available.</li>
            <li>Once a job is submitted, it will be processed by one or more nodes, depending on its number of tasks and the load-balancing settings of the server.
            Each task in the set processed by the node will perform the word count of all the articles it contains and store the counts into a simple map of words to corresponding count:
            basically a Map&lt;String, Long&gt;. This is the equivalent of a 'map' step in a map/reduce strategy.</li>
            <li>To produce results that make sense, there are constraints on what is considered a word: the characters must belong to a predefined set (in this demo ['a'...'z', 'A'...'Z', '-'], we don't count numbers as words),
            and each word must be part of a predefined dictionary. In this demo we use a dictionary based on Hunspell en_US v7.1 , which can be found from <a href="http://wordlist.sourceforge.net/">this page</a>.
            Additionally, the redirect articles are excluded from the word counts, but counted nonetheless, for statistics purposes.</li>
            <li>Once a node has processed a set of tasks, it will perform a first 'reduce' step by simply aggregating therir results. The first task in the set will hold the aggregated results, while ther other tasks will have none.</li>
            <li>When the client application receives results from a node, it will aggregate them into its own global word count map: this is the second 'reduce' step.</li>
            <li>Once all results are received, the application sorts them by descending count value, then ascending alphabetical order of the words within each count grouping, and finally format and print these sorted results to a file.</li>
          </ol>

          <h3>How do I run it?</h3>
          Before running this sample, you need to install a JPPF server and at least one node.<br>
          For information on how to set up a node and server, please refer to the <a href="http://www.jppf.org/doc/v4/index.php?title=Introduction">JPPF documentation</a>.<br>
          Once you have installed a server and node, perform the following steps:
          <ol class="samplesList">
            <li>Open a command prompt in JPPF-x.y-samples-pack/WordCount</li>
            <li>Build the sample's node add-on: type "<b>ant jar</b>". This will create a file named <b>WordCountNodeListener.jar</b>.
            This add-on is a <a href="http://www.jppf.org/doc/v4/index.php?title=Receiving_notifications_of_node_life_cycle_events">node life cycle listener</a> which accomplishes 2 goals:
            load the dictionary when the node starts (in the <i>nodeStarting()</i> notification) and aggregate the results of tasks that have just been processed (in the <i>jobEnding()</i> notification)</li>
            <li>Copy <b>WordCountNodeListener.jar</b> in the "<b>lib</b>" folder of the JPPF driver installation, to add it to the driver's classpath. The nodes will download the node listener code from the server.</li>
            <li>Start the driver</li>
            <li>Start one or more node(s). Each node should output a "<tt>loaded dictionary: 46986 entries</tt>" message, indicating that the node add-on is working properly</li>
            <li>When the server and nodes are started, type "run.bat" on Windows or "./run.sh" on Linux/Unix to start the word count demo. The results will be written to a file Named "<b>WordCountResult.txt</b>"</li>
          </ol>

          <h3>Tuning and other considerations</h3>
          <ul class="samplesList">
            <li>Configuration of the demo. The client configuration file <b>config/jppf.properties</b> allows you to change a number of parameters:
<pre class="samples"><font color="green"># path to the wikipedia file</font>
wordcount.file = data/wikipedia_en_small.xml
<font color="green"># how many articles per JPPF task</font>
wordcount.articles.per.task = 50
<font color="green"># how many task in each JPPF job</font>
wordcount.tasks.per.job = 100
<font color="green"># how many server connections can each job be distributed over (parallel I/O)</font>
wordcount.channels = 4
<font color="green"># how many concurrent job can be executing at the same time before reading of articles blocks</font>
wordcount.job.capacity = 2</pre>
            These parameters allow you to tune the demo's behavior, and optimize the memory footprint vs. throughput tradeoff. Feel free to experiment!.
            The initial values provided in this sample are fit for a client application with 1 GB or heap (-Xmx1024m). If you were to increase the job capacity, you might have to increase the application's heap size as well,
            in order to avoid out of memory conditions</li>
            <li>You should also consider carefully how the job capacity, number of channels per job, number of connections to the server, and load-balancing settings interact with each other.
            The default settings in this sample are a best attempt at maximizing the throughput and finding a balance between how many jobs can be submitted concurrently, and how fast each job can be sent to to or received
            from the server by using multiple parallel I/O channels</li>
            <li>In the same way, it may be needed to increase the memory available to each node. This mostly depends on how many article per task and tasks per job you configured, and the load balancing settings in the server.
            For reference, this demo also provides configuration file for the server and nodes, which you can find in the <b>config/driver</b> and <b>config/node</b> folders</li>
            <li>The provided Wikipedia file is an extract from the full English <a href="http://en.wikipedia.org/wiki/Wikipedia:Database_download">Wikipedia dump</a> from May 2012 with only the latest revision and no talk pages.
            The full dump is pretty large: around 8 GB compressed (bzip2) and 36 GB uncompressed. The extract provided in this demo contains the articles that fit into the first 10,000 lines.</li>
            <li>The grid topology can also play an important role. In particular, if you have very few nodes, you might consider running multiple JPPF nodes on each physical or virtual machine, each with less processing threads than for a single node.
            Because of the concurrent I/O involved, you should notice a significant throughput increase.</li>
          </ul>

          <h3>I have additional questions and comments, where can I go?</h3>
          <p>There are 2 privileged places you can go to:
          <ul class="samplesList">
            <li><a href="http://www.jppf.org/forums"/>The JPPF Forums</a></li>
            <li><a href="http://www.jppf.org/doc/v4">The JPPF documentation</a></li>
          </ul>
          
</div>

$template{name="about-page-footer"}$
