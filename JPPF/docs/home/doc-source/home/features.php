<?php $currentPage="Home" ?>
$template{name="about-page-header" title="Features"}$
  <br>
  <h1 align="center">JPPF Features</h1>

  <div class="column_left" style="text-align: justify">

    <a name="feat01"></a>
    <h4>Ease of use</h4>
    <p><a href="/doc/v5/index.php?title=Introduction">Installing</a> a JPPF grid is as easy as running the web installer or un-zipping a few files.
    Launch as many nodes and servers as needed and get immediately ready to write your first JPPF application.
    The <a href="/doc/v5/index.php?title=Development_guide">APIs</a> are easy to learn, yet very powerful, flexible and semantically consistent, and will allow you to get started in no time.

    <a name="feat02"></a>
    <h4>Dynamic flexible topology</h4>
    <p>From master/worker to P2P, with anything in between, JPPF allows <a href="/doc/v5/index.php?title=JPPF_Overview#Architecture_and_topology">any topology</a> that will suit your requirements.
    Furthermore, the topology is not static and can grow or shrink dynamically and on-demand, with a unique ability to adapt to any workload.
    Easily build any level of redundancy, avoid single points of failure and ensure the best performance and throughput for your needs.

    <a name="feat03"></a>
    <h4>Fault tolerance, self-repair and recovery</h4>
    <p>With built-in failure detection and fault tolerance mechanisms at all levels, a JPPF grid can survive massive failures in the topology, whatever the cause.
    From job requeuing to nodes rerouting, down to the ultimate failover to local execution - and even that has its own crash recovery capability -, JPPF ensures that the job is done even in extreme degradation conditions.

    <a name="feat04"></a>
    <h4>Matching of nodes to jobs</h4>
    <p>The <a href="/doc/v5/index.php?title=Job_Service_Level_Agreement">right tools</a> at the right time for the job. Ensure that jobs are executed where they are supposed to, without interfering with each other and in the best conditions.
    Fine-grained node filtering, job prioritization and scheduling, grid partitioning and many other features provide a dynamic way of matching heterogenous workloads to the grid's capacity.

    <a name="feat05"></a>
    <h4>No deployment</h4>
    <p>The built-in networked and <a href="/doc/v5/index.php?title=Class_loading_in_JPPF">distributed class loader</a> transparently ensures that the nodes can download the Java code for your application from where it is running.
    New or changed code is automatically reloaded into the nodes without any deployment hassle.
    Not only is tweaking and tinkering with the code no longer a source of time-consuming problems, it is actively facilitated and encouraged.

  </div>

  <div class="column_right" style="text-align: justify">

    <a name="feat06"></a>
    <h4>Security</h4>
    <p>Communications between components of a JPPF grid support <a href="/doc/v5/index.php?title=Configuring_SSL/TLS_communications">SSL/TLS</a> encrytpion and authentication all the way. Certificate-based authentocation, 1 way or mutual, is fully supported.
    Additional extension points allow you to further <a href="/doc/v5/index.php?title=Transforming_and_encrypting_networked_data">transform any data</a> that is transported over network connections: tunnel grid data within your own protocol, use an additional encryption layer, or any data transformation which can be of use.

    <a name="feat07"></a>
    <h4>Administration and monitoring</h4>
    <p>The JPPF graphical <a href="/screenshots.php?screenshot=NodeThreads-01.gif&shotTitle=Node Threads 01">administration console</a>, along with the public <a href="http://www.jppf.org/doc/v5/index.php?title=Management_and_monitoring">API</a> it is based on,
    enable remote monitoring and management of the grid <a href="/screenshots.php?screenshot=GraphView.gif&shotTitle=Graph View">topology</a>, <a href="/screenshots.php?screenshot=JobPriority.gif&shotTitle=Job Priority">jobs</a> life cycle,
    servers and nodes <a href="/screenshots.php?screenshot=RuntimeMonitoring.gif&shotTitle=Runtime Monitoring">health</a>, <a href="/screenshots.php?screenshot=LoadBalancerSettings.gif&shotTitle=Load Balancer Settings">load-balancing</a> configuration,
    server <a href="/screenshots.php?screenshot=ServerStats-01.gif&shotTitle=Server Stats 01">statistics</a>, and a lot more.
    The console also provides the ability to <a href="/screenshots.php?screenshot=ChartsConfiguration-01.gif&shotTitle=Charts Configuration 01">define</a> your own dynamic <a href="/screenshots.php?screenshot=Charts-01.gif&shotTitle=Charts 01">charts</a> based on dozens of dynamically updated fields you can chose from.

    <a name="feat08"></a>
    <h4>Load balancing</h4>
    <p>Multiple built-in <a href="/doc/v5/index.php?title=Configuring_a_JPPF_server#Load-balancing">load-balancing</a> algorithms are available at client and server levels, to enable an optimal distribution of the workload over the entire grid topology.
    Load balancing can be statically defined, adaptive based on the the topology and jobs requirements or even user-defined thanks to the <a href="/doc/v5/index.php?title=Creating_a_custom_load-balancer">dedicated extension point</a>.

    <a name="feat09"></a>
    <h4>.Net and J2EE integration</h4>
    <p>Specialized client implementations bring JPPF grids to the <a href="/doc/v5/index.php?title=.Net_Bridge">.Net</a> and <a href="/doc/v5/index.php?title=J2EE_Connector">J2EE</a> worlds. Write your jobs in any .Net language and execute them on .Net-enabled JPPF nodes.
    Use JPPF services from JEE enterprise applications or wrap them as Web or REST services. Make interoperability an operational reality.

    <a name="feat10"></a>
    <h4>Customization</h4>
    <p>Whenever your specialized needs go beyond what is available out-of-the-box, JPPF provides many <a href="/doc/v5/index.php?title=Extending_and_Customizing_JPPF">extension points, addons and plugins</a> which allow you to tailor, extend and customize the behavior of any of its components.
    Make JPPF your grid computing solution, without ever being stuck because of missing features.

  </div>

$template{name="about-page-footer"}$
