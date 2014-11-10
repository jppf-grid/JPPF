<?php $currentPage="Samples" ?>
$template{name="samples-page-header" title="Pluggable View Demo"}$

<div align="justify">

          <h3>What does the sample do?</h3>
          This sample demonstrates a <a href="">pluggable view</a> integrated into the JPPF administration and monitoring tool. The view shows a log of events occurring in the JPPF grid topology.
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
<pre class="samples"><span style="color: green"># enable / disable the custom view. defaults to true (enabled)</span>
jppf.admin.console.view.MyView.enabled = true
<span style="color: green"># name of a class extending org.jppf.ui.plugin.PluggableView</span>
jppf.admin.console.view.MyView.class = org.jppf.example.pluggableview.MyView
<span style="color: green"># the title for the view, seen as the tab label;</span>
jppf.admin.console.view.MyView.title = Events Log
<span style="color: green"># path to the icon for the view, seen as the tab icon</span>
jppf.admin.console.view.MyView.icon = /test.gif
<span style="color: green"># the built-in view it is attached to; it must be one of the tabbed panes of the console</span>
<span style="color: green"># possible values: Main | Topology | Charts (see section below for their definition)</span>
jppf.admin.console.view.MyView.addto = Main
<span style="color: green"># the position at which the custom view is inserted within the enclosing tabbed pane</span>
<span style="color: green"># a negative value means insert at the end; defaults to -1 (insert at the end)</span>
jppf.admin.console.view.MyView.position = 1
<span style="color: green"># whether to automatically select the view; defaults to false</span>
jppf.admin.console.view.MyView.autoselect = true</pre>
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
            <li><a href="http://www.jppf.org/doc/v4">The JPPF documentation</a></li>
          </ul>
          
</div>

$template{name="about-page-footer"}$
