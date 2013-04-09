<?php $currentPage="Press" ?>
<?php $jppfVersion="3.3" ?>
$template{name="about-page-header" title="Press Kit"}$
$template{name="press-header"}$

<h3>Latest press release: JPPF <?php echo $jppfVersion ?></h3>

<p><b>Forwarding of node management requests through the server</b>:
<a href="/doc/v3/index.php?title=Nodes_management_and_monitoring_via_the_driver">This feature</a> resolves a long-standing issue which prevented management and monitoring of the nodes not directly reachable by the clients and administration console.
It also opens up new possibilities that make monitoring and management of a JPPF grid easier, more flexible and more powerful by an order of magnitude.

<p><b>JVM monitoring and diagnostics help</b>:
A new <a href="/doc/v3/index.php?title=JVM_health_monitoring">JVM diagnostic MBean</a> allows users to monitor the JVM health of remote servers and nodes. Both management APIs and administration console now provide a set of JVM telemetry and diagnostics features.

<p><b>GPU computing</b>:
<p>A new <a href="/samples-pack/GPU/Readme.php">GPU computing demo</a> was added to the JPPF samples pack, which demonstrates how JPPF tasks can submit work to OpenCL-compatible devices with the <a href="http://code.google.com/p/aparapi/">APARAPI</a> library.

<p><b>Class loading improvements</b>:
The nodes now have the ability to reset a client-linked class loader without the need to restart, allowing complete control over the classpath of each class loader with minimal performance impact.
The <a href="/samples-pack/ExtendedClassLoading/Readme.php">Extended class loading</a> demo was updated to use this feature.

<p><b>Class loading events</b>: this new <a href="/doc/v3/index.php?title=Receiving_notifications_of_class_loader_events">extension point</a> enables users to receive notifications when a JPPF class loader loads a class or fails to load it.

<p><b>JPPF artifacts published to Maven Central</b>: as of JPPF 3.3, the JPPF jar files and associated sources and javadoc are available on Maven Central.

$template{name="press-footer"}$
$template{name="about-page-footer"}$
