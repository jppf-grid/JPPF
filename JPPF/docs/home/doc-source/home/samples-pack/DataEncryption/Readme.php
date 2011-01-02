<?php $currentPage="Samples" ?>
$template{name="samples-page-header" title="Network Data Encryption sample"}$

<div align="justify">

					<h3>What does the sample do?</h3>
					This samples illustrates the implementation of a custom transformation for all JPPF network traffic.
					The network data is encrypted using a DES cipher with a 56 bits symetric secret key.<br/>
					The initial secret key is kept in a keystore, that is included in the resulting jar file deployed to the nodes, servers and clients.
					This key is not used to actually encrypt or decrypt the data. Instead, it s used to generate and encrypt a new secret key for each new block of data to encrypt.<br/>
					This means that each block of data (i.e a task or data provider) is encrypted with a different key. The resulting block structure is thus as follows:
					<ul>
						<li>length of the new key</li>
						<li>content of the new key (encrypted with the initial key)</li>
						<li>actual data (encrypted with the new key)</li>
					</ul>

					There remains, however, one vulnerability: we still need the keystore password to be stored somewhere, so that we can use the keystore.
					To avoid storing this password in clear, we obfuscate it by using a Base64 encoding, thanks to the excellent <a href="http://iharder.sourceforge.net/current/java/base64/">iHarder.net public domain Base64 library</a>.
					The obfuscated password is then stored in a file, which is also included in the jar file to deploy.

					<h3>Related source files</h3>
					<ul>
						<li><a href="src/org/jppf/example/dataencryption/SecureKeyCipherTransform.java.html">SecureKeyCipherTransform.java</a> : the data transformer that performs the encryption and decryption</li>
						<li><a href="src/org/jppf/example/dataencryption/helper/Helper.java.html">Helper.java</a> : a set of utility methods to generate and retrieve passwords and secret keys, create and manipulate a keystore, and provide the encryption parameters</li>
						<li><a href="src/org/jppf/example/dataencryption/helper/Base64.java.html">Base64.java</a> : this is the API for Base64 encoding and decoding</li>
					</ul>

					<h3>How do I use it?</h3>
					First you need to build the sample jar file. To do this, perform the following steps:
					<ol>
						<li>open a command prompt in JPPF-x.y.z-samples-pack/DataEncryption</li>
						<li>open the file &quot;<b>build.xml</b>&quot; with a text editor, and set the value of the &quot;<b>password</b>&quot; property to the password you want to use for the keystore (it will not be included with the deployed jar file)</li>
						<li>save the file you just edited and build the sample: type &quot;<b>ant jar</b>&quot;; this will create a file named <b>DataEncryption.jar</b></li>
					</ol>
					The next step is to deploy the jar file to <b><i>every component of the JPPF grid</i></b>, including servers, nodes, and client applications, and to hook it to the JPPF component:
					<ol>
						<li>Add the jar file to the class path of each component: In the case of a server or node, it is simply a matter of dropping it into the "/lib" folder of the component's root installation path.
							For client applications, you may have to edit the script that launches the application to add the jar to the class path.</li>
						<li>Edit the JPPF configuration file of each component and add the following property:<br>
							<b>jppf.data.transform.class = org.jppf.example.dataencryption.SecureKeyCipherTransform</b></li>
					</ol>
					Once this is done, you can restart the servers, nodes and clients, and any data sent over the network will be automatically encrypted and secure.

					<h3>What features of JPPF are demonstrated?</h3>
					Custom transformation and encryption of the JPPF network traffic, allowing to work securely with a JPPF grid.
					For a detailed explanation, please refer to the related documentation in the
					<a href="http://www.jppf.org/wiki/index.php?title=Extending_and_Customizing_JPPF#Transforming_and_encrypting_networked_data">Extending and Customizing JPPF &gt; Transforming and encrypting networked data</a> section.

					<h3>I have additional questions and comments, where can I go?</h3>
					<p>If you need more insight into the code of this demo, you can consult the Java source files located in the <b>DataEncryption/src</b> folder.
					<p>In addition, There are 2 privileged places you can go to:
					<ul>
						<li><a href="http://www.jppf.org/forums"/>The JPPF Forums</a></li>
						<li><a href="http://www.jppf.org/wiki">The JPPF documentation</a></li>
					</ul>
					
</div>

$template{name="about-page-footer"}$
