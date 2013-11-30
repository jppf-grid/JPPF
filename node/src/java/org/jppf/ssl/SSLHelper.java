/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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
import java.util.Map;
import java.util.concurrent.Callable;

import javax.net.ssl.*;

import org.jppf.comm.socket.SocketWrapper;
import org.jppf.serialization.ObjectSerializer;
import org.jppf.utils.*;
import org.jppf.utils.streams.StreamUtils;
import org.slf4j.*;

/**
 * Utility class handling all aspects of the SSL configuration.
 * @author Laurent Cohen
 * @exclude
 */
public final class SSLHelper
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(SSLHelper.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The SSL configuration properties.
   */
  private static TypedProperties sslConfig = null;

  /**
   * Instantiating this class is not permitted.
   */
  private SSLHelper()
  {
  }

  /**
   * Get a SSL context from the SSL configuration.
   * @return a {@link SSLContext} instance.
   * @throws Exception if any error occurs.
   */
  public static SSLContext getSSLContext() throws Exception
  {
    return getSSLContext("jppf.ssl");
  }

  /**
   * Get a SSL context from the SSL configuration.
   * @param identifier identifies the type of channel for which to get the SSL context.
   * @return a {@link SSLContext} instance.
   * @throws Exception if any error occurs.
   */
  public static SSLContext getSSLContext(final int identifier) throws Exception
  {
    if (sslConfig == null) loadSSLProperties();
    boolean b = sslConfig.getBoolean("jppf.ssl.client.distinct.truststore", false);
    if (debugEnabled) log.debug("using {} trust store for clients, identifier = {}", b ? "distinct" : "same", JPPFIdentifiers.asString(identifier));
    switch(identifier)
    {
      case JPPFIdentifiers.CLIENT_CLASSLOADER_CHANNEL:
      case JPPFIdentifiers.CLIENT_JOB_DATA_CHANNEL:
        return getSSLContext(b ? "jppf.ssl.client" : "jppf.ssl");
      case JPPFIdentifiers.NODE_CLASSLOADER_CHANNEL:
      case JPPFIdentifiers.NODE_JOB_DATA_CHANNEL:
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
  private static SSLContext getSSLContext(final String trustStorePropertyPrefix) throws Exception
  {
    if (sslConfig == null) loadSSLProperties();
    char[] keyPwd = getPassword("jppf.ssl.keystore.password");
    KeyStore keyStore = getStore("jppf.ssl.keystore", keyPwd);
    KeyManagerFactory kmf = null;
    if (keyStore != null)
    {
      kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      kmf.init(keyStore, keyPwd);
    }
    char[] trustPwd = getPassword(trustStorePropertyPrefix + ".truststore.password");
    KeyStore trustStore = getStore(trustStorePropertyPrefix + ".truststore", trustPwd);
    TrustManagerFactory tmf = null;
    if (trustStore != null)
    {
      tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      tmf.init(trustStore);
    }
    SSLContext sslContext = SSLContext.getInstance(sslConfig.getString("jppf.ssl.context.protocol"));
    sslContext.init(kmf == null ? null : kmf.getKeyManagers(), tmf == null ? null : tmf.getTrustManagers(), null);
    return sslContext;
  }

  /**
   * Get SSL parameters from the configuration.
   * @return a {@link SSLParameters} instance.
   * @throws Exception if any error occurs.
   */
  public static SSLParameters getSSLParameters() throws Exception
  {
    if (sslConfig == null) loadSSLProperties();
    SSLParameters params = new SSLParameters();
    String s = sslConfig.getString("jppf.ssl.cipher.suites");
    String[] tokens = (s == null) ? null : s.trim().split("\\s");
    params.setCipherSuites(tokens);
    s = sslConfig.getString("jppf.ssl.protocols");
    tokens = (s == null) ? null : s.trim().split("\\s");
    params.setProtocols(tokens);
    s = sslConfig.getString("jppf.ssl.client.auth", "none").toLowerCase();
    params.setWantClientAuth("want".equals(s));
    params.setNeedClientAuth("need".equals(s));

    if (debugEnabled) log.debug("SSL parameters : cipher suites=" + StringUtils.arrayToString(params.getCipherSuites()) +
        ", protocols=" + StringUtils.arrayToString(params.getProtocols()) + ", needCLientAuth=" + params.getNeedClientAuth() + ", wantClientAuth=" + params.getWantClientAuth());
    return params;
  }

  /**
   * Create an SSL connection over an established plain socket connection.
   * @param socketClient the plain connection, already connected.
   * @return a {@link SocketWrapper} whose socket is an {@link SSLSocket} wrapping the {@link Socket} of the plain connection.
   * @throws Exception if an error occurs while configuring the SSL parameters.
   */
  public static SocketWrapper createSSLClientConnection(final SocketWrapper socketClient) throws Exception
  {
    SSLContext context = SSLHelper.getSSLContext();
    SSLSocketFactory factory = context.getSocketFactory();
    SSLSocket sslSocket = (SSLSocket) factory.createSocket(socketClient.getSocket(), socketClient.getHost(), socketClient.getPort(), true);
    SSLParameters params = SSLHelper.getSSLParameters();
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
   * @param env the environment in which to add the SSL/TLS properties.
   * @throws Exception if any error occurs.
   */
  public static void configureJMXProperties(final Map<String, Object> env) throws Exception
  {
    SSLContext sslContext = SSLHelper.getSSLContext();
    SSLSocketFactory factory = sslContext.getSocketFactory();
    env.put("jmx.remote.profiles", "TLS");
    env.put("jmx.remote.tls.socket.factory", factory);
    SSLParameters params = SSLHelper.getSSLParameters();
    env.put("jmx.remote.tls.enabled.protocols", StringUtils.arrayToString(" ", null, null, params.getProtocols()));
    env.put("jmx.remote.tls.enabled.cipher.suites", StringUtils.arrayToString(" ", null, null, params.getCipherSuites()));
    env.put("jmx.remote.tls.need.client.authentication", "" + params.getNeedClientAuth());
    env.put("jmx.remote.tls.want.client.authentication", "" + params.getWantClientAuth());
  }

  /**
   * Create and load a keystore from the specified file.
   * @param filename the name of the keystore file.
   * @param pwd the key store password.
   * @return a {@link KeyStore} instance.
   * @throws Exception if any error occurs.
   */
  private static KeyStore getKeyOrTrustStore(final String filename, final char[] pwd) throws Exception
  {
    return getKeyOrTrustStore(new FileStoreSource(filename).call(), pwd);
  }

  /**
   * Create and load a keystore from the specified input stream.
   * @param is the input stream from which to load the store.
   * @param pwd the store password.
   * @return a {@link KeyStore} instance.
   * @throws Exception if any error occurs.
   */
  private static KeyStore getKeyOrTrustStore(final InputStream is, final char[] pwd) throws Exception
  {
    if (is == null) return null;
    KeyStore ks = null;
    try
    {
      ks = KeyStore.getInstance(KeyStore.getDefaultType());
      ks.load(is, pwd);
    }
    finally
    {
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
  private static char[] getPassword(final String baseProperty) throws Exception
  {
    String s = sslConfig.getString(baseProperty, null);
    if (s != null) return s.toCharArray();
    s = sslConfig.getString(baseProperty + ".source", null);
    //return (char[]) callSource(s);
    return (char[]) callSource(s);
  }

  /**
   * Create and load a keystore from the specified file.
   * @param baseProperty the name of the keystore file.
   * @param pwd the key store password.
   * @return a {@link KeyStore} instance.
   * @throws Exception if any error occurs.
   */
  private static KeyStore getStore(final String baseProperty, final char[] pwd) throws Exception
  {
    String s = sslConfig.getString(baseProperty + ".file", null);
    if (s != null) return getKeyOrTrustStore(s, pwd);
    s = sslConfig.getString(baseProperty + ".source", null);
    //InputStream is = (InputStream) callSource(s);
    InputStream is = callSource(s);
    return getKeyOrTrustStore(is, pwd);
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
  private static <E> E callSource(final String value) throws Exception
  {
    if (value == null) return null;
    String[] tokens = value.split("\\s");
    Class<? extends Callable<E>> clazz = (Class<? extends Callable<E>>) Class.forName(tokens[0]);
    String[] args = null;
    if (tokens.length > 1)
    {
      args = new String[tokens.length - 1];
      System.arraycopy(tokens, 1, args, 0, args.length);
    }
    Constructor<? extends Callable<E>> c = null;
    try
    {
      c = clazz.getConstructor(String[].class);
    }
    catch (NoSuchMethodException ignore)
    {
    }
    Callable<E> callable = c == null ? clazz.newInstance() : c.newInstance((Object) args);
    return callable.call();
  }

  /**
   * Load the SSL properties form the source specified in the JPPF configuration.
   * @throws Exception if any error occurs.
   */
  private static synchronized void loadSSLProperties() throws Exception
  {
    if (sslConfig == null)
    {
      String source = null;
      sslConfig = new TypedProperties();
      InputStream is = null;
      TypedProperties config = JPPFConfiguration.getProperties();
      source = config.getString("jppf.ssl.configuration.source", null);
      if (source != null) is = callSource(source);
      else
      {
        source = config.getString("jppf.ssl.configuration.file", null);
        if (source == null) throw new SSLConfigurationException("no SSL configuration source is configured");
        is = FileUtils.getFileInputStream(source);
      }
      if (is == null) throw new SSLConfigurationException("could not load the SSL configuration '" + source + "'");
      try
      {
        sslConfig.load(is);
        if (debugEnabled) log.debug("successfully loaded the SSL configuration from '{}'", source);
      }
      finally
      {
        StreamUtils.closeSilent(is);
      }
    }
  }

  /**
   * Reset the SSL configuration.
   */
  public static void resetConfig()
  {
    if (sslConfig != null)
    {
      sslConfig.clear();
      sslConfig = null;
    }
  }
}
