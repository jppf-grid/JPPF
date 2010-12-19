<?php $currentPage="Samples" ?>
$template{name="samples-page-header" title="DNA / Protein Sequence Alignment"}$


					<h3>What does the sample do?</h3>
					<p align="justify">This sample searches, in a database of DNA or protein sequences, a sequence that is as close as possible to an input sequence.
					The comparisons are performed through <a href="http://en.wikipedia.org/wiki/Sequence_alignment">sequence alignment</a> and result
					in a comparison score. The sequence with the highest score is given as the result. In this sample, the database search is performed
					in parallel, meaning that multiple sequence alignments are occurring concurrently.
					<p align="justify">All sequences used in this application are in <a href="http://en.wikipedia.org/wiki/FASTA_format">FASTA format</a>.
					The sequence database used by default is a database of protein sequences for the <a href="http://en.wikipedia.org/wiki/Escherichia_coli">
					Escherichia coli</a> bacteria.
					<p>You can download other (much larger) DNA or protein databases from the <a href="http://www.ncbi.nlm.nih.gov/blast/download.shtml">NCBI web site</a>.<br>
					Sequence alignments are performed using the <a href="http://jaligner.sourceforge.net/">JAligner project</a>.

					<h3>How do I run it?</h3>
					Before running this sample application, you must have a JPPF server and at least one node running.<br>
					For information on how to set up a node and server, please refer to the <a href="http://www.jppf.org/wiki">JPPF documentation</a>.<br>
					Once you have a server and node, from a command prompt, type: <b>&quot;ant run&quot;</b>

					<h3>How do I use it?</h3>
					<p>The GUI is separated in two main parts, top and bottom.
					<p>The &quot;Compute&quot; button submits the sequence database search for processing by JPPF.
					<p>The &quot;Reset defaults&quot; button restores the target sequence, database location and substitution matrix to their original values
					<p>The database search relies on 3 parameters:
					<ul>
						<li>target sequence: the sequence to compare with those in the database; you can either type it, copy/paste it or load it from a file</li>
						<li>substitution matrix: it is used by the sequence alignment algorithm to compute the score</li>
						<li>database location: the location, in the file system of the file containing the sequences in FASTA format</li>
					</ul>
					When the search is over, the results, score and closest matching sequence, are displayed in the bottom panel.

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
					

$template{name="about-page-footer"}$
