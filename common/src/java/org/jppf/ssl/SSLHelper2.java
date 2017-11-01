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

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.Socket;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.Callable;

import javax.net.ssl.*;

import org.jppf.comm.socket.SocketWrapper;
import org.jppf.jmx.JMXHelper;
import org.jppf.serialization.ObjectSerializer;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
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
   * @throws Exception if any error occurs.
   */
  private SSLContext getSSLContext() throws Exception {
    return getSSLContext("jppf.ssl");
  }

  /**
   * Get a SSL context from the SSL configuration.
   * @param identifier identifies the type of channel for which to get the SSL context.
   * @return a {@link SSLContext} instance.
   * @throws Exception if any error occurs.
   */
  public SSLContext getSSLContext(final int identifier) throws Exception {
    boolean b = sslConfig.get(JPPFProperties.SSL_CLIENT_DISTINCT_TRUSTSTORE);
    if (debugEnabled) log.debug("using {} trust store for clients, identifier = {}", b ? "distinct" : "same", JPPFIdentifiers.asString(identifier));
    switch(identifier) {
      case JPPFIdentifiers.CLIENT_CLASSLOADER_CHANNEL:
      case JPPFIdentifiers.CLIENT_JOB_DATA_CHANNEL:
        return getSSLContext(b ? "jppf.ssl.client" : "jppf.ssl");
      case JPPFIdentifiers.NODE_CLASSLOADER_CHANNEL:
      case JPPFIdentifiers.NODE_JOB_DATA_CHANNEL:
      case JPPFIdentifiers.JMX_REMOTE_CHANNEL:
        return getSSLContext("jppf.ssl");
    }
    throw new IllegalStateException("unknown channel identifier " + Integer.toHexString(identifier));
  }

  /**
   * Get a SSL context from the SSL configuration.
   * @param trustStorePropertyPrefix the prefix to use to get the the trustore's location and password.
   * @return a {@link SSLContext} instance.
   * @throws Exception if any error occurs.
   */
  private SSLContext getSSLContext(final String trustStorePropertyPrefix) throws Exception {
    try {
      char[] keyPwd = getPassword("jppf.ssl.keystore.password");
      KeyStore keyStore = getStore("jppf.ssl.keystore", keyPwd);
      KeyManagerFactory kmf = null;
      if (keyStore != null) {
        kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keyPwd);
      }
      char[] trustPwd = getPassword(trustStorePropertyPrefix + ".truststore.password");
      KeyStore trustStore = getStore(trustStorePropertyPrefix + ".truststore", trustPwd);
      TrustManagerFactory tmf = null;
      if (trustStore != null) {
        tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
      }
      SSLContext sslContext = SSLContext.getInstance(sslConfig.get(JPPFProperties.SSL_CONTEXT_PROTOCOL));
      sslContext.init(kmf == null ? null : kmf.getKeyManagers(), tmf == null ? null : tmf.getTrustManagers(), null);
      return sslContext;
    } catch(Exception e) {
      throw (e instanceof SSLConfigurationException) ? e : new SSLConfigurationException(e);
    }
  }

  /**
   * Get SSL parameters from the configuration.
   * @return a {@link SSLParameters} instance.
   * @throws Exception if any error occurs.
   */
  public SSLParameters getSSLParameters() throws Exception {
    SSLParameters params = new SSLParameters();
    String[] tokens = sslConfig.get(JPPFProperties.SSL_CIPHER_SUITES);
    params.setCipherSuites(tokens);
    tokens = sslConfig.get(JPPFProperties.SSL_PROTOCOLS);
    params.setProtocols(tokens);
    String s = sslConfig.get(JPPFProperties.SSL_CLIENT_AUTH).toLowerCase();
    params.setWantClientAuth("want".equals(s));
    params.setNeedClientAuth("need".equals(s));
    if (log.isTraceEnabled()) log.trace(String.format("SSL parameters : protocols=%s, needCLientAuth=%b, wantClientAuth=%b, cipher suites=%", 
      StringUtils.arrayToString(params.getProtocols()), params.getNeedClientAuth(), params.getWantClientAuth(), StringUtils.arrayToString(params.getCipherSuites())));
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
    SSLContext context = getSSLContext();
    SSLSocketFactory factory = context.getSocketFactory();
    SSLSocket sslSocket = (SSLSocket) factory.createSocket(socketClient.getSocket(), socketClient.getHost(), socketClient.getPort(), true);
    SSLParameters params = getSSLParameters();
    sslSocket.setSSLParameters(params);
    sslSocket.setUseClientMode(true);
    ObjectSerializer serializer = socketClient.getSerializer();
    Class<? extends SocketWrapper> clazz = socketClient.getClass();
    Constructor<? extends SocketWrapper> c = clazz.getConstructor(Socket.class);
    SocketWrapper target = c.newInstance(sslSocket);
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
  private void configureJMXMPProperties(final Map<String, Object> env) throws Exception {
    Map<String, Object> newProps = new LinkedHashMap<>();
    SSLContext sslContext = getSSLContext();
    SSLSocketFactory factory = sslContext.getSocketFactory();
    newProps.put("jmx.remote.profiles", "TLS");
    newProps.put("jmx.remote.tls.socket.factory", factory);
    SSLParameters params = getSSLParameters();
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
  private void configureJPPFJMXProperties(final Map<String, Object> env) throws Exception {
    Map<String, Object> newProps = new LinkedHashMap<>();
    SSLContext sslContext = getSSLContext();
    newProps.put("jppf.jmx.remote.tls.enabled", true);
    newProps.put("jppf.jmx.remote.tls.socket.factory", sslContext.getSocketFactory());
    SSLParameters params = getSSLParameters();
    newProps.put("jppf.jmx.remote.tls.enabled.protocols", StringUtils.arrayToString(" ", null, null, params.getProtocols()));
    newProps.put("jppf.jmx.remote.tls.enabled.cipher.suites", StringUtils.arrayToString(" ", null, null, params.getCipherSuites()));
    if (params.getNeedClientAuth()) newProps.put("jppf.jmx.remote.tls.client.authentication", "need");
    else if (params.getWantClientAuth()) newProps.put("jppf.jmx.remote.tls.client.authentication", "want");

    String s = sslConfig.getString("jppf.ssl.truststore.password");
    if (s != null) newProps.put("jppf.jmx.remote.tls.truststore.password", s);
    s = sslConfig.getString("jppf.ssl.truststore.password.source");
    if (s != null) newProps.put("jppf.jmx.remote.tls.truststore.password.source", s);
    s = sslConfig.getString("jppf.ssl.truststore.file");
    if (s != null) newProps.put("jppf.jmx.remote.tls.truststore.file", s);
    s = sslConfig.getString("jppf.ssl.truststore.source");
    if (s != null) newProps.put("jppf.jmx.remote.tls.truststore.source", s);
    s = sslConfig.getString("jppf.ssl.keystore.password");
    if (s != null) newProps.put("jppf.jmx.remote.tls.keystore.password", s);
    s = sslConfig.getString("jppf.ssl.keystore.password.source");
    if (s != null) newProps.put("jppf.jmx.remote.tls.keystore.password.source", s);
    s = sslConfig.getString("jppf.ssl.keystore.file");
    if (s != null) newProps.put("jppf.jmx.remote.tls.keystore.file", s);
    s = sslConfig.getString("jppf.ssl.keystore.source");
    if (s != null) newProps.put("jppf.jmx.remote.tls.keystore.source", s);

    env.putAll(newProps);
    if (debugEnabled) log.debug("JMX SSL connection properties: {}", newProps);
  }

  /**
   * Create and load a keystore from the specified input stream.
   * @param is the input stream from which to load the store.
   * @param pwd the store password.
   * @param storeType the typr of keystore to use, e.g. JKS, PKCS12, BCKS etc.
   * @return a {@link KeyStore} instance.
   * @throws Exception if any error occurs.
   */
  private KeyStore getKeyOrTrustStore(final InputStream is, final char[] pwd, final String storeType) throws Exception {
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
    String storeType = sslConfig.getString(baseProperty + ".type", KeyStore.getDefaultType());
    String keyOrTrust = baseProperty.contains("keystore") ? "keystore" : "truststore";
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
    String[] tokens = RegexUtils.SPACES_PATTERN.split(value);
    Class<? extends Callable<E>> clazz = (Class<? extends Callable<E>>) Class.forName(tokens[0]);
    String[] args = null;
    if (tokens.length > 1) {
      args = new String[tokens.length - 1];
      System.arraycopy(tokens, 1, args, 0, args.length);
    }
    Constructor<? extends Callable<E>> c = null;
    try {
      c = clazz.getConstructor(String[].class);
    } catch (@SuppressWarnings("unused") NoSuchMethodException ignore) {
    }
    Callable<E> callable = c == null ? clazz.newInstance() : c.newInstance((Object) args);
    return callable.call();
  }

  /**
   * Get the ssl configuration id for the specified driver name in the client configuration.
   * @param driverName the name of the driver to build the id for.
   * @return an id composed of a property name and its value, in the form "driverName.property_suffix=value".
   */
  public static String getClientConfigId(final String driverName) {
    TypedProperties config = JPPFConfiguration.getProperties();
    String suffix = (driverName == null) || "".equals(driverName) ? "" : driverName + ".";
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
}
