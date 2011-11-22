<?php $currentPage="Samples" ?>
$template{name="samples-page-header" title="Generation of Mandelbrot Fractals"}$

<div align="justify">

					<h3>What does the sample do?</h3>
					This sample generates <a href="http://en.wikipedia.org/wiki/Mandelbrot_set">Mandelbrot fractals</a> images by submitting the computation
					to a JPPF grid.

					<h3>How do I run it?</h3>
					Before running this sample application, you must have a JPPF server and at least one node running.<br>
					For information on how to set up a node and server, please refer to the <a href="http://www.jppf.org/wiki">JPPF documentation</a>.<br>
					Once you have a server and node, you can either run the "<b>run.bat</b>" script (on Windows), "<b>./run.sh</b>" script (on Linux/Unix) or, from a command prompt, type: <b>&quot;ant run&quot;</b>.

					<h3>How do I use it?</h3>
					<p>The GUI provides many options and a lot of interactivity.
					<p>The &quot;Compute&quot; button submits the image generation for processing by JPPF.
					<p>The image generation is governed by a set of 4 parameters:
					<ul>
						<li>center X: the X coordinate of the center of the image</li>
						<li>center Y: the Y coordinate of the center of the image</li>
						<li>diameter: this determines the range of possible values of both X and Y axis</li>
						<li>iterations: this is the maximum number of iterations of the algorithm, before considering that a point &quot;escapes&quot; the Mandelbrot set</li>
					</ul>
					Tip: modifying the cordinates of the center is equivalent to a "pan" functionality<br>
					Tip: modifying the diameter provides, in effect, a zooming capability<br>
					<p>You can interact with the image using the mouse:
					<ul>
						<li>a left click will zoom-in by the value of the zoom factor, and move the center to the selected point</li>
						<li>a right click will zoom-out by the value of the zoom factor, and move the center to the selected point</li>
					</ul>
					<p>The <img src="src/resources/icons/zoomIn.gif" border="0" alt="zoom-in"/> and <img src="src/resources/icons/zoomOut.gif" border="0" alt="zoom-out"/>
					buttons provide a static zoom functionality (they do not move the center).<br>
					Tip: setting the zoom factor to 1 and clicking left or right in the image is equivalent to panning with the mouse.

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
					
</div>

$template{name="about-page-footer"}$
