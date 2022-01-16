# DNA / Protein Sequence Alignment

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
Please follow these steps:
<ol class="samplesList">
  <li>build the sample: open a command prompt in the <b>SequenceAlignment</b> folder and type "<b>mvn clean install</b>"</li>
  <li>Before running this sample application, you must also have a JPPF server and at least one node running.
  For information on how to set up a node and server, please refer to the <a href="https://www.jppf.org/doc/6.3">JPPF documentation</a>.</li>
  <li>Once you have a server and node, you can either run the "<b>run.bat</b>" script (on Windows), "<b>./run.sh</b>" script (on Linux/Unix)</li>
</ol>

<h3>How do I use it?</h3>
<p>The GUI is separated in two main parts, top and bottom.
<p>The &quot;Compute&quot; button submits the sequence database search for processing by JPPF.
<p>The &quot;Reset defaults&quot; button restores the target sequence, database location and substitution matrix to their original values
<p>The database search relies on 3 parameters:
<ul class="samplesList">
  <li>target sequence: the sequence to compare with those in the database; you can either type it, copy/paste it or load it from a file</li>
  <li>substitution matrix: it is used by the sequence alignment algorithm to compute the score</li>
  <li>database location: the location, in the file system of the file containing the sequences in FASTA format</li>
</ul>
When the search is over, the results, score and closest matching sequence, are displayed in the bottom panel.

<h3>I have additional questions and comments, where can I go?</h3>
<p>If you need more insight into the code of this demo, you can consult the source.
<p>In addition, There are 2 privileged places you can go to:
<ul class="samplesList">
  <li><a href="https://www.jppf.org/forums">The JPPF Forums</a></li>
  <li><a href="https://www.jppf.org/doc/6.3/index.php?title=Introduction#Running_the_standalone_modules">The JPPF documentation</a></li>
</ul>

