<?php $currentPage="About" ?>
$template{name="about-page-header" title="About JPPF"}$
  <h1 align="center">About</h1>

	<div align="justify">
    <div class="blockWithHighlightedTitle">
    $template{name="title-with-icon" img="images/icons/help.png" title="What it is" heading="h3"}$
		<p>Simply put, JPPF enables applications with large processing power requirements to be run on any number of computers, in order to dramatically reduce their processing time.
		This is done by splitting an application into smaller parts that can be executed simultaneously on different machines.
    <br></div>

    <br><div class="blockWithHighlightedTitle">
    $template{name="title-with-icon" img="images/icons/how.png" title="How it works" heading="h3"}$
		<p>There are 2 aspects to this:
    <p><i><b>Dividing an application into smaller parts that can be executed independently and in parallel.</b></i>
    <br>JPPF provides facilities that make this effort a lot easier, faster and much less painful than without them.
    The result is a JPPF object called a "job", itself made of smaller independent parts called "tasks".
    <p><i><b>Executing the application on the JPPF Grid.</b></i>
    <br>The simplest possible JPPF Grid is made of a server, to which any number of execution nodes are attached. A node is a JPPF software component that is generally installed and running on a separate machine.
    This is commonly called a master/slave architecture, where the work is distributed by the server (aka "master") to the nodes (aka "slaves").
    In JPPF terms, a unit of work is called a "job", and its constituting "tasks" are distributed by the server among the nodes for parallel execution.
    <br></div>

    <br><div class="blockWithHighlightedTitle">
    $template{name="title-with-icon" img="images/icons/personal2.png" title="Powered by the community" heading="h3"}$
    <p>With over 10 years of active development, JPPF boasts an architecture with a proven record of reliability, performance and scalability.
    A project committed to its community, it demonstrates an outstanding support to its users and engages in a continuous conversation with them.
    Every question, issue report or feature request turns into a contribution which, in the end, benefits the whole community.
    <br></div>

    <br><div class="blockWithHighlightedTitle">
    $template{name="title-with-icon" img="images/icons/default.png" title="Advantages" heading="h3"}$
		<p>Chief among JPPF benefits is its ease of installation, use and deployment. There is no need to spend days to write a "Hello World" application. A couple of minutes, up to a couple of hours at most, will suffice.
		Deploying JPPF components over a cluster is as simple as copying files over FTP or any network file system.
		JPPF allows developers to focus on their core software development, instead of wasting time on the complexities of parallel and distributed processing.

	`	<p>As a 100% Java framework, JPPF will run on any system that supports Java: MacOS, Windows, Linux, zOS, on any hardware from a simple laptop up to a mainframe computer.
		This does not mean that JPPF is limited to running Java jobs. You can run any application that is available on your platform as a JPPF job.
		For instance, you might want to run your favorite graphics suite in batch mode, to render multiple large, complex images all at once.

		<p>Another benefit of JPPF is a simplified, almost immediate, deployment process of your application on the grid.
		Even though your aplication will be run on many nodes at once, you only need to deploy it in a single location.
		By extending the Java class loading mechanism, JPPF removes most of the deployment burden from the application's life cycle, dramatically shortening the time-to-market and time-to-production.
    <br></div>

    <br><div class="blockWithHighlightedTitle">
    $template{name="title-with-icon" img="images/icons/view-list.png" title="Outstanding features" heading="h3"}$
    <p>There is a lot more to JPPF than running and deploying your applications on the grid: scalability, security, fault tolerance, load-balancing, job scheduling, monitoring and management of the grid, integration with popular platforms and frameworks,
    extensions and customization, etc. The <a href="features.php">features page</a> provides a comprenhensive overview of what JPPF has to offer.

    <p>For a complete list of everything you can do with JPPF, we invite you to consult our <a href="/doc/v5">full fledged documentation</a>.
    <br></div>

	</div>
	<br>
$template{name="about-page-footer"}$
