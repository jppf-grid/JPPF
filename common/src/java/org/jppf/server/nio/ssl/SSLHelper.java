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

package org.jppf.server.nio.ssl;

import java.io.*;
import java.security.KeyStore;

import javax.net.ssl.*;

import org.jppf.utils.StringUtils;
import org.jppf.utils.streams.StreamUtils;
import org.slf4j.*;

/**
 * 
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
        ", protocols=" + StringUtils.arrayToString(params.getProtocols()) + ", neddCLientAuth=" + params.getNeedClientAuth() + ", wantClientAuth=" + params.getWantClientAuth());
    return params;
  }

  /**
   * Get a default SSL context for the client side, for testing purposes.
   * @return a {@link SSLContext} instance.
   * @throws Exception if any error occurs.
   */
  public static SSLContext getDefaultServerSSLContext() throws Exception
  {
    String pwd = "password";
    SSLContext sslContext = SSLContext.getInstance("SSL");
    KeyStore trustStore = getKeyOrTrustStore("config/ssl/truststore.ks", pwd);
    TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
    tmf.init(trustStore);
    KeyStore keyStore = getKeyOrTrustStore("config/ssl/keystore.ks", pwd);
    KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
    kmf.init(keyStore, pwd.toCharArray());
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
    KeyStore trustStore = getKeyOrTrustStore("config/ssl/truststore.ks", "password");
    TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
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
  private static KeyStore getKeyOrTrustStore(final String filename, final String pwd) throws Exception
  {
    KeyStore ks = KeyStore.getInstance("jks");
    InputStream is = new BufferedInputStream(new FileInputStream(filename));
    try
    {
      ks.load(is, pwd.toCharArray());
    }
    finally
    {
      StreamUtils.close(is, log);
    }
    return ks;
  }
}
