/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.ssl;

import static org.jppf.jmx.JPPFJMXProperties.*;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.Socket;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.Callable;

import javax.net.ssl.*;

import org.jppf.comm.socket.SocketWrapper;
import org.jppf.jmx.*;
import org.jppf.serialization.ObjectSerializer;
import org.jppf.utils.*;
import org.jppf.utils.configuration.*;
import org.jppf.utils.streams.StreamUtils;
import org.slf4j.*;

/**
 * Utility class handling all aspects of the SSL configuration.
 * @author Laurent Cohen
 * @exclude
 */
public final class SSLHelper2 {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(SSLHelper2.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The SSL configuration properties.
   */
  private final TypedProperties sslConfig;

  /**
   * @param sslConfig the SSL configuration.
   */
  public SSLHelper2(final TypedProperties sslConfig) {
    this.sslConfig = sslConfig;
  }

  /**
   * Get a SSL context from the SSL configuration.
   * @return a {@link SSLContext} instance.
   * @throws SSLConfigurationException if any error occurs.
   */
  private SSLContext getSSLContext() throws SSLConfigurationException {
    return getSSLContext("jppf.ssl");
  }

  /**
   * Get a SSL context from the SSL configuration.
   * @param identifier identifies the type of channel for which to get the SSL context.
   * @return a {@link SSLContext} instance.
   * @throws SSLConfigurationException if any error occurs.
   */
  public SSLContext getSSLContext(final int identifier) throws SSLConfigurationException {
    final boolean b = sslConfig.get(JPPFProperties.SSL_CLIENT_DISTINCT_TRUSTSTORE);
    if (debugEnabled) log.debug("using {} trust store for clients, identifier = {}", b ? "distinct" : "same", JPPFIdentifiers.asString(identifier));
    switch(identifier) {
      case JPPFIdentifiers.CLIENT_CLASSLOADER_CHANNEL:
      case JPPFIdentifiers.CLIENT_JOB_DATA_CHANNEL:
        return getSSLContext(b ? "jppf.ssl.client" : "jppf.ssl");
      case JPPFIdentifiers.NODE_CLASSLOADER_CHANNEL:
      case JPPFIdentifiers.NODE_JOB_DATA_CHANNEL:
        return getSSLContext("jppf.ssl");
      case JPPFIdentifiers.JMX_REMOTE_CHANNEL:
        return getSSLContext("jppf.ssl.client", "jppf.ssl");
    }
    throw new SSLConfigurationException("unknown channel identifier " + Integer.toHexString(identifier));
  }

  /**
   * Get a SSL context from the SSL configuration.
   * @param trustStorePropertyPrefixes the prefixes to use to get the the trustore(s) location and password.
   * @return a {@link SSLContext} instance.
   * @throws SSLConfigurationException if any error occurs.
   */
  private SSLContext getSSLContext(final String...trustStorePropertyPrefixes) throws SSLConfigurationException {
    try {
      final char[] keyPwd = getPassword("jppf.ssl.keystore.password");
      final KeyStore keyStore = getStore("jppf.ssl.keystore", keyPwd);
      KeyManagerFactory kmf = null;
      if (keyStore != null) {
        kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keyPwd);
      }
      final TrustManagerFactory[] tmfs = new TrustManagerFactory[trustStorePropertyPrefixes.length];
      for (int i=0; i<trustStorePropertyPrefixes.length; i++) tmfs[i] =  getTrustManagerFactory(trustStorePropertyPrefixes[i]);
      List<X509TrustManager> trustManagers = null;
      for (final TrustManagerFactory tmf: tmfs) {
        if (tmf != null) {
          for (final TrustManager mgr: tmf.getTrustManagers()) {
            if (mgr instanceof X509TrustManager) {
              if (trustManagers == null) trustManagers = new ArrayList<>();
              trustManagers.add((X509TrustManager) mgr);
            }
          }
        }
      }
      if (debugEnabled) log.debug(String.format("tmfs=%s, trustManagers=%s", Arrays.asList(tmfs), trustManagers));
      final SSLContext sslContext = SSLContext.getInstance(sslConfig.get(JPPFProperties.SSL_CONTEXT_PROTOCOL));
      sslContext.init(kmf == null ? null : kmf.getKeyManagers(), trustManagers == null ? null : new TrustManager[] { new CompositeX509TrustManager(trustManagers) }, null);
      if (debugEnabled) log.debug("initialized SSLContext = {}", sslContext);
      printSupportedParameters(sslContext);
      return sslContext;
    } catch(final SSLConfigurationException e) {
      throw e;
    } catch(final Exception e) {
      throw new SSLConfigurationException(e);
    }
  }

  /**
   * Get a {@code TrustManagerFactory} from the SSL configuration.
   * @param trustStorePropertyPrefix the prefix to use to get the the trustore's location and password.
   * @return a {@link TrustManagerFactory} instance.
   * @throws SSLConfigurationException if any error occurs.
   */
  private TrustManagerFactory getTrustManagerFactory(final String trustStorePropertyPrefix) throws SSLConfigurationException {
    try {
      final char[] trustPwd = getPassword(trustStorePropertyPrefix + ".truststore.password");
      final KeyStore trustStore = getStore(trustStorePropertyPrefix + ".truststore", trustPwd);
      TrustManagerFactory tmf = null;
      if (trustStore != null) {
        tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
      }
      return tmf;
    } catch(final SSLConfigurationException e) {
      throw e;
    } catch(final Exception e) {
      throw new SSLConfigurationException(e);
    }
  }

  /**
   * Get SSL parameters from the configuration.
   * @return a {@link SSLParameters} instance.
   * @throws Exception if any error occurs.
   */
  public SSLParameters getSSLParameters() throws Exception {
    final SSLParameters defaultParams = SSLContext.getDefault().getDefaultSSLParameters();
    final SSLParameters params = new SSLParameters();
    String[] tokens = sslConfig.get(JPPFProperties.SSL_CIPHER_SUITES);
    if ((tokens == null) || (tokens.length <= 0)) params.setCipherSuites(defaultParams.getCipherSuites());
    else params.setCipherSuites(tokens);
    tokens = sslConfig.get(JPPFProperties.SSL_PROTOCOLS);
    params.setProtocols(tokens);
    final String s = sslConfig.get(JPPFProperties.SSL_CLIENT_AUTH).toLowerCase();
    params.setWantClientAuth("want".equals(s));
    params.setNeedClientAuth("need".equals(s));
    if (debugEnabled) log.debug("SSL parameters: {}", dumpSSLParameters(params));
    return params;
  }

  /**
   * Create an SSL connection over an established plain socket connection.
   * @param socketClient the plain connection, already connected.
   * @return a {@link SocketWrapper} whose socket is an {@link SSLSocket} wrapping the {@link Socket} of the plain connection.
   * @throws Exception if an error occurs while configuring the SSL parameters.
   */
  public SocketWrapper createSSLClientConnection(final SocketWrapper socketClient) throws Exception {
    if (debugEnabled) log.debug("creating client SSL connection from {}", socketClient);
    final SSLContext context = getSSLContext();
    final SSLSocketFactory factory = context.getSocketFactory();
    final SSLSocket sslSocket = (SSLSocket) factory.createSocket(socketClient.getSocket(), socketClient.getHost(), socketClient.getPort(), true);
    final SSLParameters params = getSSLParameters();
    sslSocket.setSSLParameters(params);
    sslSocket.setUseClientMode(true);
    final ObjectSerializer serializer = socketClient.getSerializer();
    final Class<? extends SocketWrapper> clazz = socketClient.getClass();
    final Constructor<? extends SocketWrapper> c = clazz.getConstructor(Socket.class);
    final SocketWrapper target = c.newInstance(sslSocket);
    target.setSerializer(serializer);
    target.setHost(socketClient.getHost());
    target.setPort(socketClient.getPort());
    return target;
  }

  /**
   * Configure the SSL environment parameters for a JMX connector server or client.
   * @param protocol the JMX remote protocol to use.
   * @param env the environment in which to add the SSL/TLS properties.
   * @throws Exception if any error occurs.
   */
  public void configureJMXProperties(final String protocol, final Map<String, Object> env) throws Exception {
    if (JMXHelper.JMXMP_PROTOCOL.equals(protocol)) configureJMXMPProperties(env);
    else configureJPPFJMXProperties(env);
  }

  /**
   * Configure the SSL environment parameters for a JMX connector server or client.
   * @param env the environment in which to add the SSL/TLS properties.
   * @throws Exception if any error occurs.
   */
  public void configureJMXMPProperties(final Map<String, Object> env) throws Exception {
    final Map<String, Object> newProps = new LinkedHashMap<>();
    final SSLContext sslContext = getSSLContext("jppf.ssl.client", "jppf.ssl");
    final SSLSocketFactory factory = sslContext.getSocketFactory();
    newProps.put("jmx.remote.profiles", "TLS");
    newProps.put("jmx.remote.tls.socket.factory", factory);
    final SSLParameters params = getSSLParameters();
    newProps.put("jmx.remote.tls.enabled.protocols", StringUtils.arrayToString(" ", null, null, params.getProtocols()));
    newProps.put("jmx.remote.tls.enabled.cipher.suites", StringUtils.arrayToString(" ", null, null, params.getCipherSuites()));
    newProps.put("jmx.remote.tls.need.client.authentication", "" + params.getNeedClientAuth());
    newProps.put("jmx.remote.tls.want.client.authentication", "" + params.getWantClientAuth());
    env.putAll(newProps);
    if (debugEnabled) log.debug("JMX SSL connection properties: {}", newProps);
  }

  /**
   * Configure the SSL environment parameters for a JMX connector server or client.
   * @param env the environment in which to add the SSL/TLS properties.
   * @throws Exception if any error occurs.
   */
  public void configureJPPFJMXProperties(final Map<String, Object> env) throws Exception {
    final Map<String, Object> newProps = new LinkedHashMap<>();
    final SSLContext sslContext = getSSLContext("jppf.ssl.client", "jppf.ssl");
    newProps.put("jppf.ssl", true);
    newProps.put(TLS_ENABLED.getName(), true);
    newProps.put(TLS_CONTEXT_PROTOCOL.getName(), sslContext.getProtocol());
    final SSLParameters params = getSSLParameters();
    newProps.put(TLS_ENABLED_PROTOCOLS.getName(), StringUtils.arrayToString(" ", null, null, params.getProtocols()));
    newProps.put(TLS_ENABLED_CIPHER_SUITES.getName(), StringUtils.arrayToString(" ", null, null, params.getCipherSuites()));
    if (params.getNeedClientAuth()) newProps.put(TLS_CLIENT_AUTHENTICATION.getName(), "need");
    else if (params.getWantClientAuth()) newProps.put(TLS_CLIENT_AUTHENTICATION.getName(), "want");
    convert(newProps, JPPFProperties.SSL_CLIENT_DISTINCT_TRUSTSTORE,        TLS_CLIENT_DISTINCT_TRUSTSTORE);
    convert(newProps, JPPFProperties.SSL_CLIENT_TRUSTSTORE_PASSWORD,        TLS_CLIENT_TRUSTSTORE_PASSWORD);
    convert(newProps, JPPFProperties.SSL_CLIENT_TRUSTSTORE_PASSWORD_SOURCE, TLS_CLIENT_TRUSTSTORE_PASSWORD_SOURCE);
    convert(newProps, JPPFProperties.SSL_CLIENT_TRUSTSTORE_FILE,            TLS_CLIENT_TRUSTSTORE_FILE);
    convert(newProps, JPPFProperties.SSL_CLIENT_TRUSTSTORE_SOURCE,          TLS_CLIENT_TRUSTSTORE_SOURCE);
    convert(newProps, JPPFProperties.SSL_CLIENT_TRUSTSTORE_TYPE,            TLS_CLIENT_TRUSTSTORE_TYPE);
    convert(newProps, JPPFProperties.SSL_TRUSTSTORE_PASSWORD,               TLS_TRUSTSTORE_PASSWORD);
    convert(newProps, JPPFProperties.SSL_TRUSTSTORE_PASSWORD_SOURCE,        TLS_TRUSTSTORE_PASSWORD_SOURCE);
    convert(newProps, JPPFProperties.SSL_TRUSTSTORE_FILE,                   TLS_TRUSTSTORE_FILE);
    convert(newProps, JPPFProperties.SSL_TRUSTSTORE_SOURCE,                 TLS_TRUSTSTORE_SOURCE);
    convert(newProps, JPPFProperties.SSL_TRUSTSTORE_TYPE,                   TLS_TRUSTSTORE_TYPE);
    convert(newProps, JPPFProperties.SSL_KEYSTORE_PASSWORD,                 TLS_KEYSTORE_PASSWORD);
    convert(newProps, JPPFProperties.SSL_KEYSTORE_PASSWORD_SOURCE,          TLS_KEYSTORE_PASSWORD_SOURCE);
    convert(newProps, JPPFProperties.SSL_KEYSTORE_FILE,                     TLS_KEYSTORE_FILE);
    convert(newProps, JPPFProperties.SSL_KEYSTORE_SOURCE,                   TLS_KEYSTORE_SOURCE);
    convert(newProps, JPPFProperties.SSL_KEYSTORE_TYPE,                     TLS_KEYSTORE_TYPE);

    env.putAll(newProps);
    if (debugEnabled) log.debug("JMX SSL connection properties: {} from props={}", newProps, sslConfig);
  }

  /**
   * Convert the source property from the ssl config into the destination property of the specified properties.
   * @param env the properties to convert into.
   * @param src the source property.
   * @param dest the destination property.
   */
  private void convert(final Map<String, Object> env, final JPPFProperty<?> src, final JPPFProperty<String> dest) {
    @SuppressWarnings("unchecked")
    final String s = (src.valueType() == String.class) ? JMXEnvHelper.getString((JPPFProperty<String>) src, null, sslConfig): sslConfig.getString(src.getName());
    if (s != null) env.put(dest.getName(), s);
  }

  /**
   * Create and load a keystore from the specified input stream.
   * @param is the input stream from which to load the store.
   * @param pwd the store password.
   * @param storeType the typr of keystore to use, e.g. JKS, PKCS12, BCKS etc.
   * @return a {@link KeyStore} instance.
   * @throws Exception if any error occurs.
   */
  private static KeyStore getKeyOrTrustStore(final InputStream is, final char[] pwd, final String storeType) throws Exception {
    if (is == null) return null;
    KeyStore ks = null;
    try {
      ks = KeyStore.getInstance(storeType);
      ks.load(is, pwd);
    } finally {
      StreamUtils.close(is, log);
    }
    return ks;
  }

  /**
   * Get a password for the specified based property.
   * @param baseProperty determines whether the password is for a key or trust store.
   * @return the secure store password.
   * @throws Exception if the password could not be retrieved for any reason.
   */
  private char[] getPassword(final String baseProperty) throws Exception {
    String s = sslConfig.getString(baseProperty, null);
    if (s != null) return s.toCharArray();
    s = sslConfig.getString(baseProperty + ".source", null);
    return (char[]) callSource(s);
  }

  /**
   * Create and load a keystore from the specified file.
   * @param baseProperty the name of the keystore file.
   * @param pwd the key store password.
   * @return a {@link KeyStore} instance.
   * @throws Exception if any error occurs.
   */
  private KeyStore getStore(final String baseProperty, final char[] pwd) throws Exception {
    final String storeType = sslConfig.getString(baseProperty + ".type", KeyStore.getDefaultType());
    final String keyOrTrust = baseProperty.contains("keystore") ? "keystore" : "truststore";
    String s = sslConfig.getString(baseProperty + ".file", null);
    if (s != null) {
      if (debugEnabled) log.debug(String.format("getting %s of type %s from file %s", keyOrTrust, storeType, s));
      return getKeyOrTrustStore(new FileStoreSource(s).call(), pwd, storeType);
    }
    s = sslConfig.getString(baseProperty + ".source", null);
    if (debugEnabled) log.debug(String.format("getting %s of type %s from source %s", keyOrTrust, storeType, s));
    return getKeyOrTrustStore((InputStream) callSource(s), pwd, storeType);
  }

  /**
   * Use reflexion to compute data from the specified source.
   * @param value defines which class is invoked, with which arguments, in the form:<br/>
   * &nbsp;&nbsp;&nbsp;&nbsp;<code>mypackage.MyClass arg1 ... argN</code><br/>
   * where <code>MyClass</code> is an implementation of {@link Callable}.
   * @return the result of calling the instantiated class.
   * @param <E> the of the object returned by invoking the instantiated class..
   * @throws Exception if any error occurs.
   */
  @SuppressWarnings("unchecked")
  static <E> E callSource(final String value) throws Exception {
    if (value == null) return null;
    final String[] tokens = RegexUtils.SPACES_PATTERN.split(value);
    final Class<? extends Callable<E>> clazz = (Class<? extends Callable<E>>) Class.forName(tokens[0]);
    String[] args = null;
    if (tokens.length > 1) {
      args = new String[tokens.length - 1];
      System.arraycopy(tokens, 1, args, 0, args.length);
    }
    Constructor<? extends Callable<E>> c = null;
    try {
      c = clazz.getConstructor(String[].class);
    } catch (@SuppressWarnings("unused") final NoSuchMethodException ignore) {
    }
    final Callable<E> callable = c == null ? clazz.newInstance() : c.newInstance((Object) args);
    return callable.call();
  }

  /**
   * Get the ssl configuration id for the specified driver name in the client configuration.
   * @param driverName the name of the driver to build the id for.
   * @return an id composed of a property name and its value, in the form "driverName.property_suffix=value".
   */
  public static String getClientConfigId(final String driverName) {
    final TypedProperties config = JPPFConfiguration.getProperties();
    final String suffix = (driverName == null) || "".equals(driverName) ? "" : driverName + ".";
    String name = suffix + JPPFProperties.SSL_CONFIGURATION_FILE.getName();
    String value = config.getString(name);
    if ((value == null) || "".equals(value.trim())) {
      name = suffix + JPPFProperties.SSL_CONFIGURATION_SOURCE.getName();
      value = config.getString(name);
      // use the global definition
      if (value == null) {
        if ((driverName == null) || "".equals(driverName)) return null;
        return getClientConfigId(null);
      }
    }
    return new StringBuilder().append(name).append('=').append(value).toString();
  }

  /**
   * Print the specified SSL parameters into a string.
   * @param params the parameters to print.
   * @return a formatted string describing the SSL parameters.
   */
  public static String dumpSSLParameters(final SSLParameters params) {
    return String.format("protocols=%s, needCLientAuth=%b, wantClientAuth=%b, cipher suites=%s", 
      StringUtils.arrayToString(params.getProtocols()), params.getNeedClientAuth(), params.getWantClientAuth(), StringUtils.arrayToString(params.getCipherSuites()));
  }

  /**
   * 
   * @param context .
   */
  private static void printSupportedParameters(final SSLContext context) {
    final SSLParameters params = context.getSupportedSSLParameters();
    if (debugEnabled) log.debug("supported protocols: {}, supported cipher suites: {}", Arrays.asList(params.getProtocols()), Arrays.asList(params.getCipherSuites()));
  }
}
