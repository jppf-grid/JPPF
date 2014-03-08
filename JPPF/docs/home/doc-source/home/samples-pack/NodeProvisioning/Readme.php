<?php $currentPage="Samples" ?>
$template{name="samples-page-header" title="Node Provisioning sample"}$

<div align="justify">

          <h3>What does the sample do?</h3>
          This sample customizes JPPF nodes into "master nodes", capable of starting new "slave nodes" on the same machine and stopping them on demand.
          This is basically a cloud computing-style JPPF node provisioning facility, which can be remotely invoked from a client application, or a JPPF server.
          <p>To achieve this, the master nodes will need to be able to start, stop and monitor slave node processes and provide a remotely accessible interface for these functionalities.
          <p>The remote interface is provided via a <a href="http://www.jppf.org/doc/v4/index.php?title=Pluggable_MBeans">custom node MBean</a>, which is defined in the <a href="src/master/org/jppf/example/provisioning/master/NodeProvisioningMBean.java.html">NodeProvisioningMBean</a> interface.
          The implementation of this interface is the class <a href="src/master/org/jppf/example/provisioning/master/NodeProvisioning.java.html">NodeProvisioning</a>. It delegates all its operations to a
          <a href="src/master/org/jppf/example/provisioning/master/SlaveNodeManager.java.html">SlaveNodeManager</a>, which holds a map of all running slave processes. Each slave process is wrapped into a <a href="src/master/org/jppf/example/provisioning/master/SlaveNodeLauncher.java.html">SlaveNodeLauncher</a>
          object, which actually starts and monitors the slave process. Whenever a slave process is terminated or restarted, the SlaveNodeLauncher will send a <a href="src/master/org/jppf/example/provisioning/master/SlaveNodeLauncherEvent.java.html">SlaveNodeLauncherEvent</a>
          to all registered <a href="src/master/org/jppf/example/provisioning/master/SlaveNodeLauncherListener.java.html">SlaveNodeLauncherListener</a>s. Here, the only registered listener is the SlaveNodeManager itself, so it will always know what happens to the processes.

          <p>Here is what happens when <tt>NodeProvisioningMBean.provisionSlaveNodes(nbNodes, configOverrides)</tt> is called:
          <ul class="samplesList">
            <li>if configOverrides is not null, all running slaves are stopped, since they need to run with a new configuration</li>
            <li>then the SlaveNodeManager determines the difference between nbNodes and the number of actually running slave nodes</li>
            <li>if more slave nodes than requested are already running, then it stops one or more slave nodes accordingly</li>
            <li>otherwise it will start one or more slave nodes, and for each new node:</li>
            <ul class="samplesList">
              <li>it creates a new folder named "slave_node_n", where n is a sequence number</li>
              <li>in this folder, it copies the master node's logging configuration files</li>
              <li>it reads the master node's JPPF configuration file, applies the supplied overrides (if any), then saves the configuration to a new file in the slave node's folder</li>
              <li>it finally launches the slave node, using the JVM options supplied in the configuration overrides</li>
            </ul>
          </ul>

          <p>Additionally, the slave nodes use an <a href="http://www.jppf.org/doc/v4/index.php?title=Node_initialization_hooks">InitializationHook</a> to redirect System.out and System.err to separate files. This hook is implemented in the class <a href="src/slave/org/jppf/example/provisioning/slave/OutputRedirectHook.java.html">OutputRedirectHook</a>

          <p>Finally, a client-side a demo is provided, which requests the start of 4 slave nodes for each master, submits a job that only executes on the slaves, then terminates the slaves.
          This demo is implemented in the class <a href="src/client/org/jppf/example/provisioning/client/Runner.java.html">Runner</a>

          <h3>Building and running the sampe</h3>
          Before running this sample application, you need to install a JPPF server.<br>
          For information on how to set up a server, please refer to the <a href="http://www.jppf.org/doc/v4/index.php?title=Introduction">JPPF documentation</a>.<br>
          Once you have installed a server, perform the following steps:
          <ol class="samplesList">
            <li>open a command prompt in JPPF-x.y-samples-pack/NodeProvisioning</li>
            <li>build the sample: type "<b>ant build</b>" or simply "<b>ant</b>"; this will create a file named <b>node-dist.zip</b></li>
            <li>unzip <b>node-dist.zip</b> in any location in your file system, this will uncompress the master node distribution in a folder named "<b>node-dist</b>"</li>
            <li>start the JPPF server</li>
            <li>start the master node by typing, in the "node-dist" folder, <b>startNode.bat</b> on Windows or <b>./startNode.sh</b> on Linux/Unix</li>
            <li>run the sample application: open a command prompt in JPPF-x.y-samples-pack/NodeProvisioning and type "<b>demo.bat</b>" on Windows or "<b>./demo.sh</b>" on Linux/Unix</li>
            <li>you should see the following output:
<pre class="samples">requesting 4 new slaves ...
master node B3E86DAC-F08A-B139-112D-C3698153A6F8 has 4 slaves
submitting job ...
got 20 results for job
shutting down all slaves ...
node B3E86DAC-F08A-B139-112D-C3698153A6F8 has 0 slaves</pre>
            <li>additionally, you will see that your node distribution has 4 new folders named "slave_node_0", ..., "slave_node_4", each containing the following files:
              <ul class="samplesList">
                <li>jppf-node.properties: this is the slave node's JPPF configuration, which includes the specified configuration overrides</li>
                <li>log4j-node.properties: the slave node's log4j configuration</li>
                <li>jppf-node.log: the slave's log file</li>
                <li>stdout.log: captures everything printed with SystemOut.out</li>
                <li>stderr.log: captures everything printed with SystemOut.err</li>
              </ul>
            </li>
          </ol>

          <h3>I have additional questions and comments, where can I go?</h3>
          <p>If you need more insight into the code of this demo, you can consult the Java source files located in the <b>NodeProvisioning/src</b> folder.
          <p>In addition, There are 2 privileged places you can go to:
          <ul class="samplesList">
            <li><a href="http://www.jppf.org/forums"/>The JPPF Forums</a></li>
            <li><a href="http://www.jppf.org/doc/v4">The JPPF documentation</a></li>
          </ul>
          
</div>

$template{name="about-page-footer"}$
