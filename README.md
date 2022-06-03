[![Build Status](https://api.travis-ci.com/jppf-grid/JPPF.svg?branch=master)](https://travis-ci.com/github/jppf-grid/JPPF)
&nbsp;[![Maven central](https://maven-badges.herokuapp.com/maven-central/org.jppf/jppf-common/badge.svg)](http://search.maven.org/#search|ga|1|org.jppf)
[![Apache License 2.0](http://img.shields.io/badge/license-Apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![Project Stats](https://www.openhub.net/p/jppf-project/widgets/project_thin_badge.gif)](https://www.openhub.net/p/jppf-project?ref=github)

<img src="https://www.jppf.org/images/logo3.gif"/>

# **The open source grid computing solution**

This is the home for the JPPF source code. Other services can be found at the following locations:

* **[JPPF web site](https://www.jppf.org)**
* **[Downloads](https://www.jppf.org/downloads.php)**
* **[Documentation](https://www.jppf.org/doc/)**
* **[User forums](https://www.jppf.org/forums)**
* **[Issue tracker](https://www.jppf.org/tracker/tbg)**

## JPPF modules:

* **[JPPF](JPPF)**: contains the build scripts, web site and associated templates, documentation as LibreOffice text documents
* **[admin](admin)**: the code and resources for the desktop (Swing-based) admin console
* **[admin-web](admin-web)**: the code and resources for the web admin console
* **[application-template](application-template)**: source code for the JPPF client application template
* **[client](client)**: source code for the JPPF client APIs
* **[common](common)**: utilities and classes common to the other modules
* **[containers](containers)**: configuration files and scripts to build JPPF Docker images and deploy them in cluster environments
* **[demo](demo)**: some demos and tests of JPPF features, which may be useful as code samples
* **[jca-client](jca-client)**: source code for the J2EE connector
* **[jmxremote-nio](jmxremote-nio)**: the JPPF JMX remote connector, based on NIO
* **[node](node)**: source code for the nodes
* **[samples-pack](samples-pack)**: source code and docs for the JPPF samples
* **[server](server)**: source code for the JPPF driver/server
* **[stress-tests](stress-tests)**: a framework for starting and using local JPPF grids with complex topologies
* **[tests](tests)**: JUnit-based tests for JPPF grids, with the associated homegrown test framework

## Building JPPF

***Requirements***

* **Java 8** or later
* **[Apache Maven 3.6.0](https://maven.apache.org)** or later

***Steps***

* clone the repository:<br> `git clone git@github.com:jppf-grid/JPPF.git` or `git clone https://github.com/jppf-grid/JPPF.git`
* from the repository root: `mvn clean install`
* an HTML test report is available in `<repo_root>/tests/target/junit-report`, the test logs are in `<repo_root>/tests/target/logs` (one set of logs per test class)

## JPPF Maven artifacts

* [**Maven Central**](http://search.maven.org/#search|ga|1|org.jppf)
* [**Snapshots at Sonatype**](https://oss.sonatype.org/content/repositories/snapshots/org/jppf/)
