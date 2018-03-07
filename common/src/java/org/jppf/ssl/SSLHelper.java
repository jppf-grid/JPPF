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
import java.net.Socket;
import java.util.Map;

import javax.net.ssl.*;

import org.jppf.comm.socket.SocketWrapper;
import org.jppf.jmx.JMXEnvHelper;
import org.jppf.utils.*;
import org.jppf.utils.configuration.*;
import org.jppf.utils.streams.StreamUtils;
import org.slf4j.*;

/**
 * Utility class handling all aspects of the SSL configuration.
 * @author Laurent Cohen
 * @exclude
 */
public final class SSLHelper {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(SSLHelper.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The SSL configuration properties.
   */
  //private static TypedProperties sslConfig = null;
  private static SSLHelper2 helper = null;

  /**
   * Instantiating this class is not permitted.
   */
  public SSLHelper() {
  }

  /**
   * Get a SSL context from the SSL configuration.
   * @param identifier identifies the type of channel for which to get the SSL context.
   * @return a {@link SSLContext} instance.
   * @throws Exception if any error occurs.
   */
  public static SSLContext getSSLContext(final int identifier) throws Exception {
    checkSSLProperties();
    return helper.getSSLContext(identifier);
  }

  /**
   * Get SSL parameters from the configuration.
   * @return a {@link SSLParameters} instance.
   * @throws Exception if any error occurs.
   */
  public static SSLParameters getSSLParameters() throws Exception {
    checkSSLProperties();
    return helper.getSSLParameters();
  }

  /**
   * Create an SSL connection over an established plain socket connection.
   * @param socketClient the plain connection, already connected.
   * @return a {@link SocketWrapper} whose socket is an {@link SSLSocket} wrapping the {@link Socket} of the plain connection.
   * @throws Exception if an error occurs while configuring the SSL parameters.
   */
  public static SocketWrapper createSSLClientConnection(final SocketWrapper socketClient) throws Exception {
    checkSSLProperties();
    return helper.createSSLClientConnection(socketClient);
  }

  /**
   * Configure the SSL environment parameters for a JMX connector server or client.
   * @param protocol the JMX remote protocol to use.
   * @param env the environment in which to add the SSL/TLS properties.
   * @throws Exception if any error occurs.
   */
  public static void configureJMXProperties(final String protocol, final Map<String, Object> env) throws Exception {
    checkSSLProperties();
    helper.configureJMXProperties(protocol, env);
  }

  /**
   * Load the SSL properties form the source specified in the JPPF configuration.
   * @throws Exception if any error occurs.
   */
  private static synchronized void checkSSLProperties() throws Exception {
    if (helper == null) loadSSLProperties();
  }

  /**
   * Load the SSL properties form the source specified in the JPPF configuration.
   * @throws Exception if any error occurs.
   */
  private static void loadSSLProperties() throws Exception {
    if (helper == null) {
      String source = null;
      final TypedProperties sslConfig = new TypedProperties();
      InputStream is = null;
      final TypedProperties config = JPPFConfiguration.getProperties();
      source = config.get(JPPFProperties.SSL_CONFIGURATION_SOURCE);
      if (source != null) is = SSLHelper2.callSource(source);
      else {
        source = config.get(JPPFProperties.SSL_CONFIGURATION_FILE);
        if (source == null) throw new SSLConfigurationException("no SSL configuration source is configured");
        is = FileUtils.getFileInputStream(source);
      }
      if (is == null) throw new SSLConfigurationException("could not load the SSL configuration '" + source + "'");
      try {
        sslConfig.load(is);
        helper = new SSLHelper2(sslConfig);
        if (debugEnabled) log.debug("successfully loaded the SSL configuration from '{}'", source);
      } finally {
        StreamUtils.closeSilent(is);
      }
    }
  }

  /**
   * Reset the SSL configuration.
   */
  public static void resetConfig() {
    if (helper != null) helper = null;
  }

  /**
   * Get the ssl configuration id for the specified driver name in the client configuration.
   * @param driverName the name of the driver to build the id for.
   * @return an id composed of a property name and its value, in the form "driverName.property_suffix=value".
   */
  public static String getClientConfigId(final String driverName) {
    return SSLHelper2.getClientConfigId(driverName);
  }

  /**
   * Get an SSL context based on the configuration specified in the JMX environment map.
   * @param env the env to get SSL proeprties from.
   * @return an {@link SSLHelper2} instance.
   */
  public static SSLHelper2 getJPPFJMXremoteSSLHelper(final Map<String, ?> env) {
    final TypedProperties props = new TypedProperties();
    props.set(JPPFProperties.SSL_ENABLED, true);
    convert(env, props, TLS_CONTEXT_PROTOCOL,                  JPPFProperties.SSL_CONTEXT_PROTOCOL);
    convert(env, props, TLS_ENABLED_PROTOCOLS,                 JPPFProperties.SSL_PROTOCOLS);
    convert(env, props, TLS_ENABLED_CIPHER_SUITES,             JPPFProperties.SSL_CIPHER_SUITES);
    convert(env, props, TLS_CLIENT_AUTHENTICATION,             JPPFProperties.SSL_CLIENT_AUTH);
    convert(env, props, TLS_CLIENT_DISTINCT_TRUSTSTORE,        JPPFProperties.SSL_CLIENT_DISTINCT_TRUSTSTORE);
    convert(env, props, TLS_CLIENT_TRUSTSTORE_PASSWORD,        JPPFProperties.SSL_CLIENT_TRUSTSTORE_PASSWORD);
    convert(env, props, TLS_CLIENT_TRUSTSTORE_PASSWORD_SOURCE, JPPFProperties.SSL_CLIENT_TRUSTSTORE_SOURCE);
    convert(env, props, TLS_CLIENT_TRUSTSTORE_FILE,            JPPFProperties.SSL_CLIENT_TRUSTSTORE_FILE);
    convert(env, props, TLS_CLIENT_TRUSTSTORE_SOURCE,          JPPFProperties.SSL_CLIENT_TRUSTSTORE_SOURCE);
    convert(env, props, TLS_CLIENT_TRUSTSTORE_TYPE,            JPPFProperties.SSL_CLIENT_TRUSTSTORE_TYPE);
    convert(env, props, TLS_TRUSTSTORE_PASSWORD,               JPPFProperties.SSL_TRUSTSTORE_PASSWORD);
    convert(env, props, TLS_TRUSTSTORE_PASSWORD_SOURCE,        JPPFProperties.SSL_TRUSTSTORE_PASSWORD_SOURCE);
    convert(env, props, TLS_TRUSTSTORE_FILE,                   JPPFProperties.SSL_TRUSTSTORE_FILE);
    convert(env, props, TLS_TRUSTSTORE_SOURCE,                 JPPFProperties.SSL_TRUSTSTORE_SOURCE);
    convert(env, props, TLS_TRUSTSTORE_TYPE,                   JPPFProperties.SSL_TRUSTSTORE_TYPE);
    convert(env, props, TLS_KEYSTORE_PASSWORD,                 JPPFProperties.SSL_KEYSTORE_PASSWORD);
    convert(env, props, TLS_KEYSTORE_PASSWORD_SOURCE,          JPPFProperties.SSL_KEYSTORE_PASSWORD_SOURCE);
    convert(env, props, TLS_KEYSTORE_FILE,                     JPPFProperties.SSL_KEYSTORE_FILE);
    convert(env, props, TLS_KEYSTORE_SOURCE,                   JPPFProperties.SSL_KEYSTORE_SOURCE);
    convert(env, props, TLS_KEYSTORE_TYPE,                     JPPFProperties.SSL_KEYSTORE_TYPE);
    if (debugEnabled) log.debug("env={} converted to props={}", env, props);
    return new SSLHelper2(props);
  }

  /**
   * Convert the source property from the JMX environment into the destination SSL config property.
   * @param env the JMX enviromentproperties from which to convert.
   * @param props the properties to convert into.
   * @param src the source property.
   * @param dest the destination property.
   */
  private static void convert(final Map<String, ?> env, final TypedProperties props, final JPPFProperty<String> src, final JPPFProperty<?> dest) {
    final String s = JMXEnvHelper.getString(src, env, null);
    if (s != null) props.setString(dest.getName(), s);
  }
}
