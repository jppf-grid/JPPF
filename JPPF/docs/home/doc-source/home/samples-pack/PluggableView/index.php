$template{name="sample-readme-html-header" title="Pluggable View demo"}$
<h3>What does the sample do?</h3>
This sample demonstrates a <a href="">pluggable view</a> integrated into the JPPF administration and monitoring tool. The view shows a log of grid topology and job monitoring events occurring in the JPPF grid.
Each log entry is timestamped and the view also has two action buttons to clear the log and copy the log to the clipboard.

<p>The view looks like this:
<p><img src="images/EventsLog.gif"/>

<h3>How do I run it?</h3>
Before running this sample, you need to install a JPPF server, at least one node, and the JPPF amdinistration console.<br>
For information on how to set the JPPF components, please refer to the <a href="http://www.jppf.org/doc/v5/index.php?title=Introduction">JPPF documentation</a>.<br>
Once you have installed the console, a server and a node, perform the following steps:
<ol class="samplesList">
  <li>Open a command prompt in JPPF-x.y-samples-pack/PluggableView</li>
  <li>Build the sample's jar file: type "<b>ant jar</b>". This will create a file named <b>PluggableView.jar</b>.
  <li>Copy <b>PluggableView.jar</b> in the "<b>lib</b>" folder of the JPPF amdinistration console, to add it to the console's classpath.</li>
  <li>add the following configuration properties to the admin console's configuration file "<b>JPPF-x.y-admin/config/jppf-gui.properties</b>":
<pre class="prettyprint lang-conf">
# enable / disable the custom view; defaults to true (enabled)
jppf.admin.console.view.MyView.enabled = true
# name of a class extending org.jppf.ui.plugin.PluggableView
jppf.admin.console.view.MyView.class = org.jppf.example.pluggableview.MyView
# the title for the view, seen as the tab label;
jppf.admin.console.view.MyView.title = Events Log
# path to the icon for the view, seen as the tab icon
jppf.admin.console.view.MyView.icon = /test.gif
# the built-in view it is attached to; it must be one of the tabbed panes of the console
# possible values: Main | Topology | Charts (see section below for their definition)
jppf.admin.console.view.MyView.addto = Main
# the position at which the custom view is inserted within the enclosing tabbed pane
# a negative value means insert at the end; defaults to -1 (insert at the end)
jppf.admin.console.view.MyView.position = 1
# whether to automatically select the view; defaults to false
jppf.admin.console.view.MyView.autoselect = true
</pre>
  </li>
  <li>Start the driver</li>
  <li>Start one or more node(s).</li>
  <li>Start the admin console.</li>
  <liUpon startup, the console should show the custom view</li>
</ol>

<h3>Related source files</h3>
<a href="src/org/jppf/example/pluggableview/MyView.java.html">MyView.java</a> : this is the implementation of our pluggable view.</li>

<h3>I have additional questions and comments, where can I go?</h3>
<p>There are 2 privileged places you can go to:
<ul class="samplesList">
  <li><a href="http://www.jppf.org/forums"/>The JPPF Forums</a></li>
  <li><a href="http://www.jppf.org/doc/v5">The JPPF documentation</a></li>
</ul>
$template{name="sample-readme-html-footer"}$
