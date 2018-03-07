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

package org.jppf.jmx;

import java.util.List;

import org.jppf.utils.configuration.*;

/**
 * An enumeration of {@link JPPFProperty} constants that apply to the JPPF jmx remote connector.
 * @author Laurent Cohen
 */
public class JPPFJMXProperties {
  /** Maximum time in milliseconds to wait for a JMX request to succeed, default to 15,000 ms. */
  public static final JPPFProperty<String> REQUEST_TIMEOUT = new StringProperty("jmx.remote.x.request.timeout", "60000", "jppf.jmxremote.request.timeout", "jppf.jmx.request.timeout");
  /** whether to use secure connections via TLS protocol, defaults to false. */
  public static final JPPFProperty<String> TLS_ENABLED = new StringProperty("jmx.remote.x.tls.enabled", null, "jppf.ssl.enabled");
  /** javax.net.ssl.SSLContext protocol, defaults to TLSv1.2. */
  public static final JPPFProperty<String> TLS_CONTEXT_PROTOCOL = new StringProperty("jmx.remote.x.tls.context.protocol", "TLSv1.2");
  /** A list of space-separated enabled protocols, defaults to TLSv1.2. */
  public static final JPPFProperty<String> TLS_ENABLED_PROTOCOLS = new StringProperty("jmx.remote.x.tls.enabled.protocols", null);
  /** Space-separated list of enabled cipher suites, defaults to SSLContext.getDefault().getDefaultSSLParameters().getCipherSuites(). */
  public static final JPPFProperty<String> TLS_ENABLED_CIPHER_SUITES = new StringProperty("jmx.remote.x.tls.enabled.cipher.suites", null);
  /** SSL client authentication level: one of 'none', 'want', 'need', defaults to 'none'. */
  public static final JPPFProperty<String> TLS_CLIENT_AUTHENTICATION = new StringProperty("jmx.remote.x.tls.client.authentication", null);
  /** Whether to use a separate trust store for client certificates (server only), defaults to 'false'. */
  public static final JPPFProperty<String> TLS_CLIENT_DISTINCT_TRUSTSTORE = new StringProperty("jmx.remote.x.tls.client.distinct.truststore", null);
  /** Plain text client trust store password, defaults to null. */
  public static final JPPFProperty<String> TLS_CLIENT_TRUSTSTORE_PASSWORD = new StringProperty("jmx.remote.x.tls.client.truststore.password", null);
  /** Client trust store password as an arbitrary source, default to null. */
  public static final JPPFProperty<String> TLS_CLIENT_TRUSTSTORE_PASSWORD_SOURCE = new StringProperty("jmx.remote.x.tls.client.truststore.password.source", null);
  /** Path to the client trust store in the file system or classpath, defaults to null. */
  public static final JPPFProperty<String> TLS_CLIENT_TRUSTSTORE_FILE = new StringProperty("jmx.remote.x.tls.client.truststore.file", null);
  /** Client trust store location as an arbitrary source, defaults to null. */
  public static final JPPFProperty<String> TLS_CLIENT_TRUSTSTORE_SOURCE = new StringProperty("jmx.remote.x.tls.client.truststore.source", null);
  /** Client trust store format, defaults to 'jks'. */
  public static final JPPFProperty<String> TLS_CLIENT_TRUSTSTORE_TYPE = new StringProperty("jmx.remote.x.tls.client.truststore.type", "jks");
  /** Plain text trust store password, defaults to null. */
  public static final JPPFProperty<String> TLS_TRUSTSTORE_PASSWORD = new StringProperty("jmx.remote.x.tls.truststore.password", null);
  /** Trust store password as an arbitrary source, defaults to null. */
  public static final JPPFProperty<String> TLS_TRUSTSTORE_PASSWORD_SOURCE = new StringProperty("jmx.remote.x.tls.truststore.password.source", null);
  /** Path to the trust store in the file system or classpath, defaults to null. */
  public static final JPPFProperty<String> TLS_TRUSTSTORE_FILE = new StringProperty("jmx.remote.x.tls.truststore.file", null);
  /** Trust store location as an arbitrary source, defaults to null. */
  public static final JPPFProperty<String> TLS_TRUSTSTORE_SOURCE = new StringProperty("jmx.remote.x.tls.truststore.source", null);
  /** Trust store format, defaults to 'jks'. */
  public static final JPPFProperty<String> TLS_TRUSTSTORE_TYPE = new StringProperty("jmx.remote.x.tls.truststore.type", "jks");
  /** Plain text key store password, defaults to null. */
  public static final JPPFProperty<String> TLS_KEYSTORE_PASSWORD = new StringProperty("jmx.remote.x.tls.keystore.password", null);
  /** Key store password as an arbitrary source, defaults to null. */
  public static final JPPFProperty<String> TLS_KEYSTORE_PASSWORD_SOURCE = new StringProperty("jmx.remote.x.tls.keystore.password.source", null);
  /** Path to the key store in the file system or classpath, defaults to null. */
  public static final JPPFProperty<String> TLS_KEYSTORE_FILE = new StringProperty("jmx.remote.x.tls.keystore.file", null);
  /** Key store location as an arbitrary source, defaults to null. */
  public static final JPPFProperty<String> TLS_KEYSTORE_SOURCE = new StringProperty("jmx.remote.x.tls.keystore.source", null);
  /** Key store format, defaults to 'jks'. */
  public static final JPPFProperty<String> TLS_KEYSTORE_TYPE = new StringProperty("jmx.remote.x.tls.keystore.type", "jks");
  /** The list of all predefined properties in this class. */
  private static List<JPPFProperty<?>> properties = allProperties();

  /**
   * Get the list of all predefined configuration properties in this class.
   * @return A list of {@link JPPFProperty} instances.
  */
  public synchronized static List<JPPFProperty<?>> allProperties() {
    if (properties == null) properties = ConfigurationUtils.allProperties(JPPFJMXProperties.class);
    return properties;
  }
}
