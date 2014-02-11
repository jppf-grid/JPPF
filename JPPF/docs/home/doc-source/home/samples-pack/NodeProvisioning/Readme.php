<?php $currentPage="Samples" ?>
$template{name="samples-page-header" title="Node Provisioning sample"}$

<div align="justify">

          <h3>What does the sample do?</h3>
          This sample customizes JPPF nodes into "master nodes", capable of starting new "slave nodes" onthe same machine and stopping them on demand.
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

          <h3>How do I run it?</h3>
          Before running this sample application, you need to install a JPPF server and at least one node.<br>
          For information on how to set up a node and server, please refer to the <a href="http://www.jppf.org/doc/v4/index.php?title=Introduction">JPPF documentation</a>.<br>
          Once you have installed a server and node, perform the following steps:
          <ol class="samplesList">
            <li>open a command prompt in JPPF-x.y-samples-pack/NodeLifeCycle</li>
            <li>build the sample: type "<b>ant jar</b>" or simply "<b>ant</b>"; this will create a file named <b>NodeLifeCycle.jar</b></li>
            <li>copy NodeLifeCycle.jar in the "lib" folder of the JPPF driver installation, as well as all the *.jar files in <tt>NodeLifeCycle/lib</tt>, to add them to the driver's classpath. This is enough to deploy the add-on.</li>
            <li>start the database server: open a command prompt in NodeNodeLifeCycle/db and type "<b>startH2.bat</b>" (on Windows) or "<b>./startH2.sh</b>" (on Linux). Alternatively you can run an Ant target instead: "<b>ant start.db.server</b>"</li>
            <li>start the server and node</li>
            <li>run the sample application: open a command prompt in JPPF-x.y-samples-pack/NodeNodeLifeCycle and type "<b>ant run</b>"</li>
            <li>you should see a display of the tasks execution results, followed by a display of all the rows inserted in the database table.<br/>
                Additionally, the node's console will show the sequence of events that took place, including the node shutdown and restart events</li>
            <li>to stop the database server: open a command prompt in NodeNodeLifeCycle/db and type "<b>stopH2.bat</b>" (on Windows) or "<b>./stopH2.sh</b>" (on Linux). Alternatively you can run an Ant target instead: "<b>ant stop.db.server</b>"</li>
            <li>to reset the database: open a command prompt in NodeNodeLifeCycle and run the Ant target: "<b>ant reset.db</b>". This will re-create the database with an empty table</li>
          </ol>

          <h3>What features of JPPF are demonstrated?</h3>
          <ul class="samplesList">
            <li><a href="http://www.jppf.org/doc/v4/index.php?title=Receiving_notifications_of_node_life_cycle_events">Subscribing to node life cycle events</a></li>
            <li>Integration with a JTA-compliant transaction manager and implementation of node crash recovery</li>
          </ul>

          <h3>I have additional questions and comments, where can I go?</h3>
          <p>If you need more insight into the code of this demo, you can consult the Java source files located in the <b>CustomMBeans/src</b> folder.
          <p>In addition, There are 2 privileged places you can go to:
          <ul class="samplesList">
            <li><a href="http://www.jppf.org/forums"/>The JPPF Forums</a></li>
            <li><a href="http://www.jppf.org/doc/v4">The JPPF documentation</a></li>
          </ul>
          
</div>

$template{name="about-page-footer"}$
