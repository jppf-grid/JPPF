$template{name="sample-readme-html-header" title="Search Engine demo"}$
<h3>What does the sample do?</h3>
This sample application performs as a web search engine. It takes a search query and performs the search starting from a specified URL.<br>
The application will follow every link of each visited page, up to the specified link depth limit.<br>
It will then return the list of URLs matching the search query, ordered by relevance.<br>
The visiting of the pages is realized through the integration of JPPF with the <a href="https://crawler.dev.java.net">Smart and Simple Web Crawler</a> project.<br>
The search and page indexing are implemented thanks to the <a href="http://lucene.apache.org/">Lucene</a> project from the Apache Foundation.

<h3>How do I run it?</h3>
Please follow these steps:
<ol class="samplesList">
  <li>Build the sample: open a command prompt in the <b>WebSearchEngine</b> folder and type "<b>mvn clean install</b>"</li>
  <li>Before running this sample application, you must have a JPPF server and at least one node running.
  For information on how to set up a node and server, please refer to the <a href="https://www.jppf.org/doc/6.3/index.php?title=Introduction">JPPF documentation</a>.</li>
  <li>Once you have a server and node, from the command prompt, type: <b>"./run.sh"</b> on Linux/Unix/Mac, or <b>"run.bat"</b> on Windows</li>
</ol>

<h3>How do I use it?</h3>
<p>The GUI is separated in two main parts, the search parameters at the top and the search results at the bottom.
<p>The &quot;Compute&quot; button submits the web search for processing by JPPF.
<p>The &quot;Reset defaults&quot; button restores the start url, search query and link depth to their original values
<p>The web search relies on 3 parameters:
<ul class="samplesList">
  <li>start URL: this is the URL of the web page from which links will be followed recursively. To limit the scope (and length) of the search,
  a filter is set so only links to the same server will be followed</li>
  <li>search query: this is what to search on the visited pages; interpreted as <a href="https://lucene.apache.org/core/2_9_4/queryparsersyntax.html">Lucene query syntax</a></li>
  <li>search depth: this parameter also limits the scope of the search, by restricting the depth of the chains of links that can be followed.
  For example: if the depth is set to 1, only the links found on the start page will be followed. If it is set to 2, the links found in the
  pages specified by the links in the start page will also be followed</li>
</ul>
When the search is over, the results are displayed in the search results panel, in descending order of relevance.

<h3>What integration features of JPPF are demonstrated?</h3>
<ul class="samplesList">
  <li><b>Integration with other Java-based open source projects</b>.<br>
  Among other things, you will notice that the 3rd-party libraries (including Lucene and Smart and Simple Web Crawler) remain in the
  classpath of the client application.</li>
  <li><b>Using JPPF as part of a workflow</b>.<br>
    In this sample the search is realized through multiple invocations of JPPF:
    <ul class="samplesNestedList">
      <li>once for every search depth level to perform the link navigation and gather links for the next level</li>
      <li>once for the actual indexing and searching of the pages, once all links have been gathered</li>
    </ul>
    This design explains why the search can take a long time since, contrary to search engines such a Yahoo or Google, the indexes are
    recomputed during the search.
  </li>
  <li><b>Integration with a graphical user interface</b>.</li>
</ul>

<h3>How can I build the sample?</h3>
To compile the source code, from a command prompt, type: <b>&quot;ant compile&quot;</b><br>
To generate the Javadoc, from a command prompt, type: <b>&quot;ant javadoc&quot;</b>

<h3>I have additional questions and comments, where can I go?</h3>
<p>If you need more insight into the code of this demo, you can consult the source, or have a look at the
<a href="javadoc/index.html">API documentation</a>.
<p>In addition, There are 2 privileged places you can go to:
<ul class="samplesList">
  <li><a href="https://www.jppf.org/forums">The JPPF Forums</a></li>
  <li><a href="https://www.jppf.org/doc/6.2">The JPPF documentation</a></li>
</ul>
$template{name="sample-readme-html-footer"}$
