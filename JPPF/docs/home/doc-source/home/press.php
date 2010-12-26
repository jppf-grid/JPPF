<?php $currentPage="Press" ?>
<?php $jppfVersion="2.4" ?>
$template{name="about-page-header" title="Press Kit"}$
$template{name="press-header"}$

	<h3>Latest press release: JPPF <?php echo $jppfVersion ?></h3>

	<p>This release provides critical bug fixes, performance and resources usage enhancements, along with new features that increase JPPF's ease of use and integration capabilities.
	For the full list and details of the new features in JPPF <?php echo $jppfVersion ?>, do not hesitate to read the <a href="/release_notes.php?version=<?php echo $jppfVersion ?>">JPPF <?php echo $jppfVersion ?> release notes</a>.

	<h5><a href="/wiki/index.php?title=JPPF_Executor_Services#Batch_modes" class="headerlink"><b>Executor service batch modes</b></a></h5>

	<p>The executor service facade to JPPF now enables the grouping of tasks submitted individually, according to space (number of tasks) or time (via timeout settings).
	Tasks submitted individually are grouped into JPPF jobs and thus benefit from full parallelism and a significant throughput increase.

	<h5><a href="/wiki" class="headerlink"><b>Documentation</b></a></h5>

	<p>A new <a href="/wiki/index.php?title=JPPF_Overview">JPPF overview</a> chapter was added.<br/>
	The online documentation was reorganized for an easier navigation experience.

	<h5><a href="/wiki/index.php?title=Receiving_notifications_of_node_life_cycle_events" class="headerlink"><b>New "node life cycle" extension point</b></a></h5>

	<p>It is now possible to subscribe to node life cycle events, and perform actions accordingly.

	<h5><a href="/samples-pack/NodeLifeCycle/Readme.php" class="headerlink"><b>New "Node Life Cycle" sample</b></a></h5>

	<p>The "Node Life Cycle" sample was added to the <a href="/samples-pack">JPPF samples pack</a>, illustrating how transaction management can be controlled via node life cycle events.

	<h5><b>Performance, memory footprint enhancements</b></h5>

	<p>Disk overflow capabilities were added to the nodes and clients.<br/>
	Reworked I/O processing results in faster network I/O and smaller memory footprint.

$template{name="press-footer"}$
$template{name="about-page-footer"}$
