<?php $currentPage="Samples" ?>
$template{name="samples-page-header" title="Kryo Serialization sample"}$

<div align="justify">

          <h3>What does the sample do?</h3>
          This sample provides an implementation of a <a href="http://www.jppf.org/doc/v4/index.php?title=Specifying_alternate_serialization_schemes">custom serialization scheme</a> which uses the <a href="https://github.com/EsotericSoftware/kryo">Kryo</a> library for serializing and deserializing Java objects.

          <h3>How do I use it?</h3>
          <ol class="samplesList">
            <li>First you will need to build the serialization scheme extension: from a shell or comand prompt in <b>KryoSerializer/</b> type: <b>./build.sh</b> (on Linux), <b>build.bat</b> (on Windows) or <b>ant build</b></li>
            <li>This will create the file <b>KryoSerializer.jar</b></li>
            <li>For each JPPF node, server and client that you use, add <b>KryoSerialzer.jar</b>, along with all the jars in <b>KryoSerializer/lib</b>, to the classpath</li>
            <li>For each node, server and client, edit the JPPF configuration file and add the following property:<br/>
<pre class="samples"><font color="green"># Use the Kryo serializer as JPPF serialization scheme</font>
jppf.object.serialization.class = org.jppf.serialization.kryo.KryoSerialization</pre>
            </li>
            <li>example configuration files are provided in <b>KryoSerializer/config</b></li>
            <li>when this is done, the JPPF is ready to work with Kryo serialization</li>
          </ol>

          <h3>Sample's source files</h3>
          <ul class="samplesList">
            <li><a href="src/org/jppf/serialization/kryo/KryoSerialization.java.html">KryoSerialization.java</a>: this is our implementation of the serialization scheme using Kryo</li>
          </ul>

          <h3>I have additional questions and comments, where can I go?</h3>
          <p>There are 2 privileged places you can go to:
          <ul class="samplesList">
            <li><a href="http://www.jppf.org/forums"/>The JPPF Forums</a></li>
            <li><a href="http://www.jppf.org/wiki">The JPPF documentation</a></li>
          </ul>
          
</div>

$template{name="about-page-footer"}$
