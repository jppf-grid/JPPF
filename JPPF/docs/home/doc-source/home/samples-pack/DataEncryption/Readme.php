<?php $currentPage="Samples" ?>
$template{name="samples-page-header" title="Network Data Encryption sample"}$

<h3>What does the sample do?</h3>
This samples illustrates the implementation of a custom transformation for all JPPF network traffic.
The network data is encrypted using a DES cipher with a 56 bits symetric secret key.

<h3>How do I use it?</h3>
First you need to build the sample jar file. To do this, perform the following steps:
<ol>
	<li>open a command prompt in JPPF-2.0-samples-pack/DataEncryption</li>
	<li>build the sample: type "<b>ant jar</b>"; this will create a file named <b>DataEncryption.jar</b></li>
</ol>
The next step is to deploy the jar file to <b><i>every component of the JPPF grid</i></b>, including servers, nodes, and client applications, and to hook it to the JPPF component:
<ol>
	<li>Add the jare file to the class path of each component: In the case of a server or node, it is simply a matter of dropping it into the "/lib" folder of the component's root installation path.
		For client applications, you may have to edit the script that launches the application to add the jar to the class path.</li>
	<li>Edit the JPPF configuration file of each component and add the following property:<br>
		<b>jppf.data.transform.class = org.jppf.example.dataencryption.DESCipherTransform</b></li>
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

$template{name="about-page-footer"}$
