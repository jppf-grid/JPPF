JPPF JMX remote connector.

List of available environment properties:

jppf.jmxremote.request.timeout,                        Maximum time in milliseconds to wait for a JMX request to succeed, default to 15,000 ms

TLS properties:

- jppf.jmx.remote.tls.enabled:
  whether to use secure connections via TLS protocol, defaults to false 
- jppf.jmx.remote.tls.context.protocol:
  javax.net.ssl.SSLContext protocol, defaults to TLSv1.2
- jppf.jmx.remote.tls.enabled.protocols:
  A list of space-separated enabled protocols, defaults to TLSv1.2
- jppf.jmx.remote.tls.enabled.cipher.suites:
  Space-separated list of enabled cipher suites, defaults to SSLContext.getDefault().getDefaultSSLParameters().getCipherSuites()
- jppf.jmx.remote.tls.client.authentication:
  SSL client authentication level: one of 'none', 'want', 'need', defaults to 'none'
- jppf.jmx.remote.tls.client.distinct.truststore:
  Whether to use a separate trust store for client certificates (server only), defaults to 'false'
- jppf.jmx.remote.tls.client.truststore.password:
  Plain text client trust store password, defaults to null
- jppf.jmx.remote.tls.client.truststore.password.source:
  Client trust store location as an arbitrary source, default to null
- jppf.jmx.remote.tls.client.truststore.file:
  Path to the client trust store in the file system or classpath, defaults to null
- jppf.jmx.remote.tls.client.truststore.source:
  Client trust store location as an arbitrary source, defaults to null
- jppf.jmx.remote.tls.client.truststore.type:
  Trust store format, defaults to 'jks'
- jppf.jmx.remote.tls.truststore.password:
  Plain text trust store password, defaults to null
- jppf.jmx.remote.tls.truststore.password.source:
  Trust store password as an arbitrary source, defaults to null
- jppf.jmx.remote.tls.truststore.file:
  Path to the trust store in the file system or classpath, defaults to null
- jppf.jmx.remote.tls.truststore.source:
  Trust store location as an arbitrary source, defaults to null
- jppf.jmx.remote.tls.truststore.type:
  Trust store format, defaults to 'jks'
- jppf.jmx.remote.tls.keystore.password:
  Plain text key store password, defaults to null
- jppf.jmx.remote.tls.keystore.password.source:
  Key store password as an arbitrary source, defaults to null
- jppf.jmx.remote.tls.keystore.file:
  Path to the key store in the file system or classpath, defaults to null
- jppf.jmx.remote.tls.keystore.source:
  Key store location as an arbitrary source, defaults to null
- jppf.jmx.remote.tls.keystore.type:
  Key store format, defaults to 'jks'

Authentication and authorization

Authentication is provided as an instance of javax.management.remote.JMXAuthenticator, passed on via the server environment property
"jmx.remote.authenticator" (also the constant javax.management.remote.JMXConnectorServer.AUTHENTICATOR).

On the client side, credentials are passed via the connector environment property "jmx.remote.credentials" (also the Java constant
javax.management.remote.JMXConnector.CREDENTIALS).

Authorization or ACL is provided via the server-side environment property "jmx.remote.x.authorization.checker" (also the Java constant
org.jppf.jmxremote.JPPFJMXConnectorServer.AUTHORIZATION_CHECKER). The value can be either a Class object, representing an implementation
of the interface org.jppf.jmxremote.JMXAuthorizationChecker, or a string that contains the fully qualified name of the class.