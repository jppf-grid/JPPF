<?php $currentPage="Press" ?>
<?php $jppfVersion="2.5" ?>
$template{name="about-page-header" title="Press Kit"}$
$template{name="press-header"}$

<h3>Latest press release: JPPF <?php echo $jppfVersion ?></h3>

<p>Broadcast jobs, flexible serialization, data grid integration, embedded FTP server and many other enhancements and fixes. For the full list and details of the new features in JPPF <?php echo $jppfVersion ?>, do not hesitate to read the <a href="http://localhost:8880/release_notes.php?version=<?php echo $jppfVersion ?>">JPPF <?php echo $jppfVersion ?> release notes</a>. 

<p>A new <a href="/wiki/index.php?title=Specifying_alternate_object_streams#Generic_JPPF_serialization"><b>generic serialization framework</b></a> enables developers to use non-serializable classes in their JPPF tasks. This is especially useful when using third party libraries whose source code is not available.

<p><a href="/wiki/index.php?title=Job_Service_Level_Agreement#Broadcast_jobs"><b>Broadcast jobs</b></a> bring the ability to run the same JPPF job on all available grid nodes

<p><b>Class loading performance enhancements</b>: a new class definition cache on the server brings improved node startup time and faster job execution.

<p><b>Administration console enhancements</b>:
<ul>
	<li>elements are now sorted in the tree views</li>
	<li>ability to reset the server statistics</li>
	<li>rendering and display fixes and enhancements</li>
</ul>

<p><b>Data grid integration</b>: the <a href="/samples-pack/DataDependency/Readme.php">real-time portfolio updates</a> sample now uses Hazelcast as its distributed data fabric

<p>New integration sample: <a href="/samples-pack/FTPServer/Readme.php"><b>embedding a FTP server and client</b></a>, demonstrates how Apache ftpd server can be embedded within a JPPF server, and how FTP client APIs can access it from a JPPF task.

<p>New sample: <a href="/samples-pack/Nbody/Readme.php"><b>parallel N-body problem implementation</b></a>, a JPPF-powered implementation of the N-body problem applied to anti-protons trapped in a magnetic field.

$template{name="press-footer"}$
$template{name="about-page-footer"}$
