$template{name="sample-readme-html-header" title="Configuration documentation demo"}$
<h3>What does the sample do?</h3>
<p>This sample demonstrates how to use the <a href="https://www.jppf.org/doc/6.3/index.php?title=The_JPPF_configuration_API"> JPPF configuration API</a> to generate the documentation of all predefined configuration properties.
<h3>How do I run it?</h3>
<p>To run the demo, in a command prompt or shell console, type "./run.sh" on Linux/Unix/Mac or "run.bat" on Windows.
This will generate a <a href="JPPFConfiguration.html">JPPFConfiguration.html</a> file which shows the documentation of all predefined JPPF configuration properties, grouped by categories/tags</p>
<h3>Source file</h3>
<a href="target/tohtml/src/org/jppf/example/configuration/ConfigurationHTMLPrinter.java.html">ConfigurationHTMLPrinter.java</a>: the source code of the HTML documentation generator
<h3>Related</h3>
<p>The <a href="https://www.jppf.org/doc/6.3/index.php?title=Configuration_properties_reference">configuration properties reference</a> section of the documentation is generated using the same API
(<a href="https://github.com/jppf-grid/JPPF/blob/master/common/src/java/org/jppf/doc/WikiConfigurationPrinter.java">source code here</a>) and in a similar fashion.
The main difference is that, instead of straight HTML, the code generates MediaWiki text.
<h3>I have additional questions and comments, where can I go?</h3>
<p>There are 2 privileged places you can go to:
<ul>
  <li><a href="https://www.jppf.org/forums">The JPPF Forums</a></li>
  <li><a href="https://www.jppf.org/doc/6.3/">The JPPF documentation</a></li>
</ul>
$template{name="sample-readme-html-footer"}$
