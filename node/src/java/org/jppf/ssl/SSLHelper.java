/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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
import java.util.concurrent.Callable;

import javax.net.ssl.*;

import org.jppf.comm.socket.SocketWrapper;
import org.jppf.utils.*;
import org.jppf.utils.streams.StreamUtils;
import org.slf4j.*;

/**
 * Utility class handling all aspects of the SSL configuration.
 * @author Laurent Cohen
 */
public class SSLHelper
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
   * Get default SSL parameters for testing purposes.
   * @return a {@link SSLParameters} instance.
   * @throws Exception if any error occurs.
   */
  public static SSLParameters getDefaultSSLParameters() throws Exception
  {
    //SSLParameters params = SSLContext.getDefault().getDefaultSSLParameters();
    SSLParameters params = new SSLParameters();
    //params.setCipherSuites(new String[] { "SSL_RSA_WITH_RC4_128_MD5", "SSL_RSA_WITH_RC4_128_SHA", "SSL_RSA_WITH_3DES_EDE_CBC_SHA", "SSL_RSA_WITH_DES_CBC_SHA" });
    params.setCipherSuites(new String[] { "SSL_RSA_WITH_DES_CBC_SHA" });
    params.setProtocols(new String[] { "SSLv2Hello", "SSLv3" });
    params.setNeedClientAuth(false);
    params.setWantClientAuth(false);

    if (debugEnabled) log.debug("SSL parameters : cipher suites=" + StringUtils.arrayToString(params.getCipherSuites()) +
        ", protocols=" + StringUtils.arrayToString(params.getProtocols()) + ", needCLientAuth=" + params.getNeedClientAuth() + ", wantClientAuth=" + params.getWantClientAuth());
    return params;
  }

  /**
   * Get a default SSL context for the client side, for testing purposes.
   * @return a {@link SSLContext} instance.
   * @throws Exception if any error occurs.
   */
  public static SSLContext getDefaultServerSSLContext() throws Exception
  {
    char[] pwd = "password".toCharArray();
    SSLContext sslContext = SSLContext.getInstance("SSL");
    KeyStore trustStore = getKeyOrTrustStore("config/ssl/truststore.ks", pwd);
    //TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(trustStore);
    KeyStore keyStore = getKeyOrTrustStore("config/ssl/keystore.ks", pwd);
    //KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(keyStore, pwd);
    sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
    return sslContext;
  }

  /**
   * Get a default SSL context for the client side, for testing purposes.
   * @return a {@link SSLContext} instance.
   * @throws Exception if any error occurs.
   */
  public static SSLContext getDefaultClientSSLContext() throws Exception
  {
    SSLContext sslContext = SSLContext.getInstance("SSL");
    KeyStore trustStore = getKeyOrTrustStore("config/ssl/truststore.ks", "password".toCharArray());
    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(trustStore);
    sslContext.init(null, tmf.getTrustManagers(), null);
    return sslContext;
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
   * Get a SSL context from the SSL configuration.
   * @return a {@link SSLContext} instance.
   * @throws Exception if any error occurs.
   */
  public static SSLContext getSSLContext() throws Exception
  {
    if (sslConfig == null) loadSSLProperties();

    char[] keyPwd = getPassword("jppf.ssl.keystore.password");
    KeyStore keyStore = getStore("jppf.ssl.keystore", keyPwd);
    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(keyStore, keyPwd);
    char[] trustPwd = getPassword("jppf.ssl.truststore.password");
    KeyStore trustStore = getStore("jppf.ssl.truststore", trustPwd);
    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(trustStore);
    SSLContext sslContext = SSLContext.getInstance(sslConfig.getString("jppf.ssl.context.protocol"));
    sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
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
    params.setNeedClientAuth("need".equals(s));
    params.setWantClientAuth("want".equals(s));

    if (debugEnabled) log.debug("SSL parameters : cipher suites=" + StringUtils.arrayToString(params.getCipherSuites()) +
      ", protocols=" + StringUtils.arrayToString(params.getProtocols()) + ", needCLientAuth=" + params.getNeedClientAuth() + ", wantClientAuth=" + params.getWantClientAuth());
    return params;
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
    InputStream is = (InputStream) callSource(s);
    return getKeyOrTrustStore(is, pwd);
  }

  /**
   * Use reflexion to compute data from the specified source.
   * @param value defines which class is invoked, with which arguments, in the form:<br/>
   * &nbsp;&nbsp;&nbsp;&nbsp;<code>mypackage.MyClass arg1 ... argN</code><br/>
   * where <code>MyClass</code> is an implementation of {@link Callable}.
   * @return the result of calling the instantiated class.
   * @throws Exception if any error occurs.
   */
  private static Object callSource(final String value) throws Exception
  {
    if (value == null) return null;
    String[] tokens = value.split("\\s");
    Class<?> clazz = Class.forName(tokens[0]);
    String[] args = null;
    if (tokens.length > 1)
    {
      args = new String[tokens.length - 1];
      System.arraycopy(tokens, 1, args, 0, args.length);
    }
    Constructor c = clazz.getConstructor(String[].class);
    Callable<?> callable = (Callable<?>) c.newInstance((Object) args);
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
      sslConfig = new TypedProperties();
      TypedProperties config = JPPFConfiguration.getProperties();
      String filename = config.getString("jppf.ssl.configuration", null);
      String configSource = config.getString("jppf.ssl.configuration.source", null);
      InputStream is = null;
      try
      {
        is = JPPFConfiguration.getConfigurationStream(filename, configSource);
        if (is != null) sslConfig.load(is);
      }
      finally
      {
        if (is != null) StreamUtils.closeSilent(is);
      }
    }
  }

  /**
   * Create an SSL connection over an established plain socket connection.
   * @param socketClient the plain connection, already connected.
   * @return a {@link SocketWrapper} whose socket is an {@link SSLSocket} wrapping the {@link Socket} of the plain connection.
   * @throws Exception if an error occurs while configure the SSL parameters.
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
}
