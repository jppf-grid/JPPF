<?php
  $currentPage="Features";
  $jppf_version = "6.2";
?>
$template{name="about-page-header" title="Features"}$
  <h1 align="center">JPPF Features</h1>

  <div class="column_left" style="text-align: justify">

    <div class="blockWithHighlightedTitle">
    <a name="feat01"></a>
    $template{name="title-with-icon" img="images/icons/easy.png" title="Ease of use" heading="h3"}$
    <p><a href="/doc/<?php echo $jppf_version; ?>/index.php?title=Introduction">Installing</a> a JPPF grid is as easy as running the web installer or un-zipping a few files.
    Launch as many nodes and servers as needed and get immediately ready to write your first JPPF application.
    The <a href="/doc/<?php echo $jppf_version; ?>/index.php?title=Development_guide">APIs</a> are easy to learn, yet very powerful, flexible and semantically consistent, and will allow you to get started in no time.
    <br></div>

    <br><div class="blockWithHighlightedTitle">
    <a name="feat02"></a>
    $template{name="title-with-icon" img="images/icons/topology.png" title="Dynamic flexible topology" heading="h3"}$
    <p>From master/worker to P2P, with anything in between, JPPF allows <a href="/doc/<?php echo $jppf_version; ?>/index.php?title=JPPF_Overview#Architecture_and_topology">any topology</a> that will suit your requirements.
    Furthermore, the topology is not static and can grow or shrink dynamically and on-demand, with a unique ability to adapt to any workload.
    Easily build any level of redundancy, avoid single points of failure and ensure the best performance and throughput for your needs.
    <br></div>

    <br><div class="blockWithHighlightedTitle">
    <a name="feat03"></a>
    $template{name="title-with-icon" img="images/icons/preferences-desktop-4.png" title="Fault tolerance, self-repair and recovery" heading="h3"}$
    <p>With built-in failure detection and fault tolerance mechanisms at all levels, a JPPF grid can survive massive failures in the topology, whatever the cause.
    From job requeuing to nodes rerouting, down to the ultimate failover to local execution - and even that has its own crash recovery capability -, JPPF ensures that the job is done even in extreme degradation conditions.
    <br></div>

    <br><div class="blockWithHighlightedTitle">
    <a name="feat09"></a>
    $template{name="title-with-icon" img="images/icons/integration.png" title=Android, "Android, .Net, J2EE integration" heading="h3"}$
    <p>
    Specialized client and node implementations bring JPPF grids to the <a href="/doc/<?php echo $jppf_version; ?>/index.php?title=Android_Node">Android</a>,
    <a href="/doc/<?php echo $jppf_version; ?>/index.php?title=.Net_Bridge">.Net</a> and <a href="/doc/<?php echo $jppf_version; ?>/index.php?title=J2EE_Connector">J2EE</a> worlds.
    Open up your grid implementation to the world of Android mobile devices.
    Write your jobs in any .Net language and execute them on .Net-enabled JPPF nodes.
    Use JPPF services from JEE enterprise applications or wrap them as Web or REST services. Make interoperability an operational reality.
    <br></div>

    <br><div class="blockWithHighlightedTitle">
    <a name="feat05"></a>
    $template{name="title-with-icon" img="images/icons/no-deployment.png" title="No deployment" heading="h3"}$
    <p>The built-in networked and <a href="/doc/<?php echo $jppf_version; ?>/index.php?title=Class_loading_in_JPPF">distributed class loader</a> transparently ensures that the nodes can download the Java code for your application from where it is running.
    New or changed code is automatically reloaded into the nodes without any deployment hassle.
    Not only is tweaking and tinkering with the code no longer a source of time-consuming problems, it is actively facilitated and encouraged.
    <br></div>

    <br><div class="blockWithHighlightedTitle">
    $template{name="title-with-icon" img="images/icons/anchor.png" title="Container-ready" heading="h3"}$
    <p>Fully integrated with container-based technologies, JPPF provides <a href="https://hub.docker.com/u/jppfgrid">Docker images</a> that can be deployed in Kubernetes clusters using a <a href="https://github.com/jppf-grid/JPPF/tree/master/containers/k8s/jppf">Helm chart</a>,
    as well as in a Docker swarm cluster, using a <a href="https://github.com/jppf-grid/JPPF/tree/master/containers#jppf-service-stack">docker compose service stack</a>.
    Deployment of a JPPF compute grid in a cloud infrastructure has never been easer.
    <br></div>

  </div>

  <div class="column_right" style="text-align: justify">

    <div class="blockWithHighlightedTitle">
    <a name="feat06"></a>
    $template{name="title-with-icon" img="images/icons/security.png" title="Security" heading="h3"}$
    <p>Communications between components of a JPPF grid support <a href="/doc/<?php echo $jppf_version; ?>/index.php?title=Configuring_SSL/TLS_communications">SSL/TLS</a> encrytpion and authentication all the way. Certificate-based authentication, 1 way or mutual, is fully supported.
    Additional extension points allow you to further <a href="/doc/<?php echo $jppf_version; ?>/index.php?title=Transforming_and_encrypting_networked_data">transform any data</a> that is transported over network connections: tunnel grid data within your own protocol, use an additional encryption layer, or any data transformation which can be of use.
    <br></div>

    <br><div class="blockWithHighlightedTitle">
    <a name="feat07"></a>
    $template{name="title-with-icon" img="images/icons/monitoring.png" title="Administration and monitoring" heading="h3"}$
    <p>The JPPF <a href="/screenshots/gallery-images/Admin%20Console%20-%20Desktop/Topology-TreeView.gif">administration console</a>, along with the public <a href="https://www.jppf.org/doc/<?php echo $jppf_version; ?>/index.php?title=Management_and_monitoring">API</a> it is based on,
    enable remote monitoring and management of the grid <a href="/screenshots/gallery-images/Admin%20Console%20-%20Desktop/GraphView.gif">topology</a>, <a href="/screenshots/gallery-images/Admin%20Console%20-%20Desktop/JobPriority.gif">jobs</a> life cycle,
    servers and nodes <a href="/screenshots/gallery-images/Admin%20Console%20-%20Desktop/RuntimeMonitoring.gif">health</a>, configuration of the <a href="/screenshots/gallery-images/Admin%20Console%20-%20Desktop/LoadBalancerSettings.gif">load-balancing</a>,
    server <a href="/screenshots/gallery-images/Admin%20Console%20-%20Desktop/ServerStats-01.gif">statistics</a>, etc.
    The console also provides the ability to <a href="/screenshots/gallery-images/Admin%20Console%20-%20Desktop/ChartsConfiguration-01.gif">define</a> your own dynamic <a href="/screenshots/gallery-images/Admin%20Console%20-%20Desktop/Charts-01.gif">charts</a> based on dozens of dynamically updated fields you can chose from.
    <br></div>

    <br><div class="blockWithHighlightedTitle">
    <a name="feat08"></a>
    $template{name="title-with-icon" img="images/icons/load-balancing.png" title="Load balancing" heading="h3"}$
    <p>Multiple built-in <a href="/doc/<?php echo $jppf_version; ?>/index.php?title=Load_Balancing">load-balancing</a> algorithms are available at client and server levels, to enable an optimal distribution of the workload over the entire grid topology.
    Load balancing can be statically defined, adaptive based on the the topology and jobs requirements or even user-defined thanks to the <a href="/doc/<?php echo $jppf_version; ?>/index.php?title=Creating_a_custom_load-balancer">dedicated extension point</a>.
    <br></div>

    <br><div class="blockWithHighlightedTitle">
    <a name="feat04"></a>
    $template{name="title-with-icon" img="images/icons/job-node2.png" title="Matching the Workload with the Grid" heading="h3"}$
    <p>The <a href="/doc/<?php echo $jppf_version; ?>/index.php?title=Job_Service_Level_Agreement">right tools</a> at the right time for the job. Ensure that jobs are executed where they are supposed to, without interfering with each other and in the best conditions.
    Fine-grained node filtering, job prioritization and scheduling, grid partitioning and many other features provide a dynamic way of matching heterogenous workloads to the grid's capacity.
    <br></div>

    <br><div class="blockWithHighlightedTitle">
    <a name="feat10"></a>
    $template{name="title-with-icon" img="images/icons/personal.png" title="Customization" heading="h3"}$
    <p>Whenever your specialized needs go beyond what is available out-of-the-box, JPPF provides many <a href="/doc/<?php echo $jppf_version; ?>/index.php?title=Extending_and_Customizing_JPPF">extension points, addons and plugins</a> which allow you to tailor and customize the behavior of any of its components.
    Make JPPF your grid computing solution, without ever being stuck because of missing features.
    <br></div>

    <br><div class="blockWithHighlightedTitle">
    $template{name="title-with-icon" img="images/icons/system-run.png" title="Continuous quality" heading="h3"}$
    <p>The quality of the JPPF code and deliverables is continuously ensured, thanks to a large set of unit and integration tests, using a home-grown test frameworks that mounts real-life JPPF grids.
    From simple to complex grid topologies, there is nothing that cannot be automatically tested. This guarantees that any issue is identified and fixed as early as possible.
    Furthermore, security scans ensure that potential security issues are quickly identified and remediated.
    <br></div>

  </div>

$template{name="about-page-footer"}$
