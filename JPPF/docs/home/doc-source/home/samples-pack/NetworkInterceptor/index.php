$template{name="sample-readme-html-header" title="Network Interceptor demo"}$
<h3>What does the sample do?</h3>
<p>This sample demonstrates the use of a <a href="http://www.jppf.org/doc/5.2/index.php?title=Network_interceptors">Network interceptor</a> in a very simple authentication scheme.
<p>The authentication consists in the client side of each connection sending a user name that must match the user name on the server side. The server then sends a response, either "OK" or an error message.
If the repsonse is "OK" then client will proceed with the conenction, otherwise it will exit.
<p>The user name is set as a system property -Djppf.user.name=&lt;user_name&gt; on all JPPF processes (client, server, nodes), and the interceptor encrypts it before send it over the network.
As in the <a href="../DataEncryption">Network Data Encryption demo</a>, the network data is encrypted using a DES cipher with a 56 bits symetric secret key.
This secret key is kept in a keystore included in the resulting jar file deployed to the nodes, servers and clients. Therefore, the jar file is the weakest point in the security chain.
This design should not be used in production, but it is sufficient for the needs of this demo.

<h3>How do I run it?</h3>
To build and run the demo, please follow these steps:
<ol class="samplesList">
  <li>The first thing to do is to generate the secret key used for encryption and decryption: open a command prompt in <b>JPPF-x.y.z-samples-pack/NetworkInterceptor</b> and type "<b>ant -Dpassword=&lt;keystore_password&gt;</b>"
    This will compile the demo's code, generate a secret key and store it in a keystore, store the provided keystore password in a file in Base64 format, and finally put all these files in the file <b>NetworkInterceptor.jar</b></li>
  <li>You will then need to have a JPPF server and at least one node installed. For information on how to set up a node and server, please refer to the <a href="http://www.jppf.org/doc/5.2/index.php?title=Introduction">JPPF documentation</a></li>
  <li>before starting the server and node, we will need to configure them so they can discover and use the interceptor:
    <ul class="samplesNestedList" style="list-style-type: hyphen">
      <li>add <b>NetworkInterceptor.jar</b> to their classpath, by simply dropping it into their <b>lib</b> directory</li>
      <li>edit their configuration file - <b>config/jppf-driver.properties</b> for the driver, <b>config/jppf-node.properties</b> for a node - and look for the <b>jppf.jvm.options</b> property.
        At the end of the property's value, add <b>"-Djppf.user.name=&lt;your_user_name&gt;"</b>. As an example, the property should now look like this:
        <pre class="prettyprint lang-conf" style="margin: 0">jppf.jvm.options = -server -Xmx256m <i><b>-Djppf.user.name=jppf_user</b></i></pre>
      </li>
    </ul>
  </li>
  <li>we can now start the driver and node. In their console output, there will be messages like this:
<pre class="prettyprint lang-text" style="margin: 0">
successful client authentication
successful server authentication
</pre>
    <b>Note</b>: we see client and server messages in the driver output, because the driver actually connects to the JMX remote server of each node.
  </li>
  <li>to run the demo, open a command prompt in <b>JPPF-x.y.z-samples-pack/NetworkInterceptor</b>, then there are several options:
    <ul class="samplesNestedList" style="list-style-type: hyphen">
      <li>to launch the demo with the Ant script on any platform: "<b>ant run -Djppf.user.name=&lt;your_user_name&gt;</b>"</li>
      <li>to launch the demo with a shell script on Linux/Unix/Mac: "<b>./run.sh &lt;your_user_name&gt;</b>"</li>
      <li>to launch the demo with a shell script on Windows: "<b>run.bat &lt;your_user_name&gt;</b>"</li>
    </ul>
  </li>
  <li>You can try running the demo with a user name that doesn't match the one configured for the driver. You will then see that the demo quickly terminates without submitting a job</li>
</ol>

<h3>Source files</h3>
<ul class="samplesList">
  <li><a href="src/org/jppf/example/interceptor/NetworkInterceptorDemo.java.html">NetworkInterceptorDemo.java</a>: The entry point for the demo</li>
  <li><a href="src/org/jppf/example/interceptor/DefaultNetworkConnectionInterceptor.java.html">DefaultNetworkConnectionInterceptor.java</a>: the network interceptor implementation</li>
  <li><a href="src/org/jppf/example/interceptor/CryptoHelper.java.html">CryptoHelper.java</a>: utility class used to create secret keys and encrypt/decrypt data</li>
  <li><a href="src/org/jppf/example/interceptor/MyTask.java.html">MyTask.java</a>: a simple JPPF task added to the job submitted by the demo</li>
</ul>

<h3>I have additional questions and comments, where can I go?</h3>
<p>If you need more insight into the code of this demo, you can consult the Java source files located in the <b>NetworkInterceptor/src</b> folder.
<p>In addition, There are 2 privileged places you can go to:
<ul>
  <li><a href="http://www.jppf.org/forums"/>The JPPF Forums</a></li>
  <li><a href="http://www.jppf.org/doc/5.2/">The JPPF documentation</a></li>
</ul>
$template{name="sample-readme-html-footer"}$
