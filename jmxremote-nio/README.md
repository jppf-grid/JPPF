# JPPF JMX remote connector.

This is a full-fledged, fast and scalable implementation of a JMX remote connector, with Java NIO-based networking on both client and server sides.

JMX service URLs are in the form `service:jmx:jppf://<host>:<port>`

## How to get it

**Direct download**: from the [latest release](https://github.com/jppf-grid/JPPF/releases/tag/v_6_2), look for JPPF-6.2-jmxremote-nio.zip

**Maven Central**: [groupId: org.jppf, artifactId: jppf-jmxremote-nio](https://search.maven.org/search?q=g:org.jppf%20AND%20a:jppf-jmxremote-nio&core=gav)

```xml
<dependency>
  <groupId>org.jppf</groupId>
  <artifactId>jppf-jmxremote-nio</artifactId>
  <version>6.2</version>
</dependency>
```

## Environment properties

#### Misc:

- `jmx.remote.x.request.timeout`: maximum time in milliseconds to wait for a JMX request to succeed, default to 15,000 ms
- `jmx.remote.x.notifications.queue.size`: maximum size of the pending notifications queue for a JMX connection, defaults to 2000

#### TLS properties:

- `jmx.remote.x.tls.enabled`: whether to use secure connections via TLS protocol, defaults to false 
- `jmx.remote.x.tls.context.protocol`: javax.net.ssl.SSLContext protocol, defaults to TLSv1.2
- `jmx.remote.x.tls.enabled.protocols`: a list of space-separated enabled protocols, defaults to TLSv1.2
- `jmx.remote.x.tls.enabled.cipher.suites`: space-separated list of enabled cipher suites,<br>defaults to `SLContext.getDefault().getDefaultSSLParameters().getCipherSuites()`
- `jmx.remote.x.tls.client.authentication`: SSL client authentication level: one of `none`, `want`, `need`, defaults to `none`
- `jmx.remote.x.tls.client.distinct.truststore`: whether to use a separate trust store for client certificates (server only), defaults to 'false'
- `jmx.remote.x.tls.client.truststore.password`: plain text client trust store password, defaults to `null`
- `jmx.remote.x.tls.client.truststore.password.source`: Client trust store location as an arbitrary source, default to null
- `jmx.remote.x.tls.client.truststore.file`: path to the client trust store in the file system or classpath, defaults to `null`
- `jmx.remote.x.tls.client.truststore.source`: client trust store location as an arbitrary source, defaults to `null`
- `jmx.remote.x.tls.client.truststore.type`: trust store format, defaults to `jks`
- `jmx.remote.x.tls.truststore.password`: plain text trust store password, defaults to `null`
- `jmx.remote.x.tls.truststore.password.source`: trust store password as an arbitrary source, defaults to `null`
- `jmx.remote.x.tls.truststore.file`: path to the trust store in the file system or classpath, defaults to `null`
- `jmx.remote.x.tls.truststore.source`: trust store location as an arbitrary source, defaults to `null`
- `jmx.remote.x.tls.truststore.type`: trust store format, defaults to `jks`
- `jmx.remote.x.tls.keystore.password`: plain text key store password, defaults to `null`
- `jmx.remote.x.tls.keystore.password.source`: key store password as an arbitrary source, defaults to `null`
- `jmx.remote.x.tls.keystore.file`: path to the key store in the file system or classpath, defaults to `null`
- `jmx.remote.x.tls.keystore.source`: key store location as an arbitrary source, defaults to `null`
- `jmx.remote.x.tls.keystore.type`: key store format, defaults to `jks`

#### Authentication and authorization

Authentication is provided as an instance of [JMXAuthenticator](https://docs.oracle.com/javase/8/docs/api/index.html?javax/management/remote/JMXAuthenticator.html), passed on via the server environment property `jmx.remote.authenticator` (also the Java constant [JMXConnectorServer.AUTHENTICATOR](https://docs.oracle.com/javase/8/docs/api/javax/management/remote/JMXConnectorServer.html#AUTHENTICATOR)).

On the client side, credentials are passed as a user-defined object via the connector environment property `jmx.remote.credentials` (also the Java constant
[JMXConnector.CREDENTIALS](https://docs.oracle.com/javase/8/docs/api/javax/management/remote/JMXConnector.html#CREDENTIALS)).

Authorization or ACL is provided via the server-side environment property `jmx.remote.x.authorization.checker` (also the Java constant
[JPPFJMXConnectorServer.AUTHORIZATION_CHECKER](https://www.jppf.org/javadoc/6.1/org/jppf/jmxremote/JPPFJMXConnectorServer.html#AUTHORIZATION_CHECKER)). The value can be either a `Class` object, representing an implementation
of the interface [JMXAuthorizationChecker](https://www.jppf.org/javadoc/6.1/index.html?org/jppf/jmxremote/JMXAuthorizationChecker.html), or a string that contains the fully qualified name of the class.

Authorization checks are performed against the [Subject](https://docs.oracle.com/javase/8/docs/api/index.html?javax/security/auth/Subject.html) created by the [JMXAuthenticator](https://docs.oracle.com/javase/8/docs/api/index.html?javax/management/remote/JMXAuthenticator.html), if any is present. It is passed on to the [JMXAuthorizationChecker](https://www.jppf.org/javadoc/6.1/index.html?org/jppf/jmxremote/JMXAuthorizationChecker.html) via its [setSubject(Subject)](https://www.jppf.org/javadoc/6.1/org/jppf/jmxremote/JMXAuthorizationChecker.html#setSubject(javax.security.auth.Subject)) method.

Instead of implementing the [JMXAuthorizationChecker](https://www.jppf.org/javadoc/6.1/index.html?org/jppf/jmxremote/JMXAuthorizationChecker.html) interface, you may also extend the [JMXAuthorizationCheckerAdapter](https://www.jppf.org/javadoc/6.1/index.html?org/jppf/jmxremote/JMXAuthorizationCheckerAdapter.html) (allows everything) or [JMXAuthorizationDeniedAdapter](https://www.jppf.org/javadoc/6.1/index.html?org/jppf/jmxremote/JMXAuthorizationDeniedAdapter.html) (denies everything) class if you don't need to implement all the methods.

Unit tests are found in the [test.org.jppf.jmxremote](../tests/src/test/java/test/org/jppf/jmxremote) package of the [tests](../tests) module.
