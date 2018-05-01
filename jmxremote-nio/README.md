# JPPF JMX remote connector.


This is a full-fledged, fast and scalable implementation of a JMX remote connector, with Java NIO-based networking on both client and server sides.

JMX service URLs are in the form `service:jmx:jppf://<host>:<port>`


## Environment properties

#### Misc:

* `jmx.remote.x.request.timeout`: Maximum time in milliseconds to wait for a JMX request to succeed, default to 15,000 ms

#### TLS properties:

* `jmx.remote.x.tls.enabled`:
  whether to use secure connections via TLS protocol, defaults to false 
* `jmx.remote.x.tls.context.protocol`:
  javax.net.ssl.SSLContext protocol, defaults to TLSv1.2
* `jmx.remote.x.tls.enabled.protocols`:
  A list of space-separated enabled protocols, defaults to TLSv1.2
* `jmx.remote.x.tls.enabled.cipher.suites`:
  Space-separated list of enabled cipher suites, defaults to SSLContext.getDefault().getDefaultSSLParameters().getCipherSuites()
* `jmx.remote.x.tls.client.authentication`:
  SSL client authentication level: one of 'none', 'want', 'need', defaults to 'none'
* `jmx.remote.x.tls.client.distinct.truststore`:
  Whether to use a separate trust store for client certificates (server only), defaults to 'false'
* `jmx.remote.x.tls.client.truststore.password`:
  Plain text client trust store password, defaults to null
* `jmx.remote.x.tls.client.truststore.password.source`:
  Client trust store location as an arbitrary source, default to null
* `jmx.remote.x.tls.client.truststore.file`:
  Path to the client trust store in the file system or classpath, defaults to null
* `jmx.remote.x.tls.client.truststore.source`:
  Client trust store location as an arbitrary source, defaults to null
* `jmx.remote.x.tls.client.truststore.type`:
  Trust store format, defaults to 'jks'
* `jmx.remote.x.tls.truststore.password`:
  Plain text trust store password, defaults to null
* `jmx.remote.x.tls.truststore.password.source`:
  Trust store password as an arbitrary source, defaults to null
* `jmx.remote.x.tls.truststore.file`:
  Path to the trust store in the file system or classpath, defaults to null
* `jmx.remote.x.tls.truststore.source`:
  Trust store location as an arbitrary source, defaults to null
* `jmx.remote.x.tls.truststore.type`:
  Trust store format, defaults to 'jks'
* `jmx.remote.x.tls.keystore.password`:
  Plain text key store password, defaults to null
* `jmx.remote.x.tls.keystore.password.source`:
  Key store password as an arbitrary source, defaults to null
* `jmx.remote.x.tls.keystore.file`:
  Path to the key store in the file system or classpath, defaults to null
* `jmx.remote.x.tls.keystore.source`:
  Key store location as an arbitrary source, defaults to null
* `jmx.remote.x.tls.keystore.type`:
  Key store format, defaults to 'jks'

#### Authentication and authorization

Authentication is provided as an instance of [JMXAuthenticator](https://docs.oracle.com/javase/7/docs/api/index.html?javax/management/remote/JMXAuthenticator.html), passed on via the server environment property `jmx.remote.authenticator` (also the Java constant [JMXConnectorServer.AUTHENTICATOR](https://docs.oracle.com/javase/7/docs/api/javax/management/remote/JMXConnectorServer.html#AUTHENTICATOR)).

On the client side, credentials are passed as a user-defined object via the connector environment property `jmx.remote.credentials` (also the Java constant
[JMXConnector.CREDENTIALS](https://docs.oracle.com/javase/7/docs/api/javax/management/remote/JMXConnector.html#CREDENTIALS)).

Authorization or ACL is provided via the server-side environment property `jmx.remote.x.authorization.checker` (also the Java constant
[JPPFJMXConnectorServer.AUTHORIZATION_CHECKER](https://www.jppf.org/javadoc/6.0/org/jppf/jmxremote/JPPFJMXConnectorServer.html#AUTHORIZATION_CHECKER)). The value can be either a `Class` object, representing an implementation
of the interface [JMXAuthorizationChecker](https://www.jppf.org/javadoc/6.0/index.html?org/jppf/jmxremote/JMXAuthorizationChecker.html), or a string that contains the fully qualified name of the class.

Authorization checks are performed against the [Subject](https://docs.oracle.com/javase/7/docs/api/index.html?javax/security/auth/Subject.html) created by the [JMXAuthenticator](https://docs.oracle.com/javase/7/docs/api/index.html?javax/management/remote/JMXAuthenticator.html), if any is present. It is passed on to the [JMXAuthorizationChecker](https://www.jppf.org/javadoc/6.0/index.html?org/jppf/jmxremote/JMXAuthorizationChecker.html) via its [setSubject(Subject)](https://www.jppf.org/javadoc/6.0/org/jppf/jmxremote/JMXAuthorizationChecker.html#setSubject(javax.security.auth.Subject)) method.

Instead of implementing the [JMXAuthorizationChecker](https://www.jppf.org/javadoc/6.0/index.html?org/jppf/jmxremote/JMXAuthorizationChecker.html) interface, you may also extend the [JMXAuthorizationCheckerAdapter](https://www.jppf.org/javadoc/6.0/index.html?org/jppf/jmxremote/JMXAuthorizationCheckerAdapter.html) (allows everything) or [JMXAuthorizationDeniedAdapter](https://www.jppf.org/javadoc/6.0/index.html?org/jppf/jmxremote/JMXAuthorizationDeniedAdapter.html) (denies everything) class if you don't need to implement all the methods.

Unit tests are found in the [test.org.jppf.jmxremote](../tests/src/tests/test/org/jppf/jmxremote) package of the [tests](../tests) module.
