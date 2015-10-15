/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package org.jppf.management;

import java.lang.management.ManagementFactory;
import java.net.*;
import java.util.*;

import javax.management.remote.*;
import javax.management.remote.generic.GenericConnector;

import org.jppf.ssl.SSLHelper;
import org.jppf.utils.*;
import org.jppf.utils.configuration.ConfigurationHelper;
import org.slf4j.*;

/**
 * Wrapper around the JMXMP remote connector server implementation.
 * @author Laurent Cohen
 * @exclude
 */
public class JMXMPServer extends AbstractJMXServer {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JMXMPServer.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * An ordered set of configuration properties to use for looking up the desired management port.
   */
  private final String[] portProperties;

  /**
   * Initialize this JMX server.
   */
  public JMXMPServer() {
    this(new JPPFUuid().toString(), false);
  }

  /**
   * Initialize this JMX server with the specified uuid.
   * @param id the unique id of the driver or node holding this jmx server.
   * @param ssl specifies whether JMX should be used over an SSL/TLS connection.
   * @param portProperties an ordered set of configuration properties to use for looking up the desired management port.
   */
  public JMXMPServer(final String id, final boolean ssl, final String...portProperties) {
    this.id = id;
    this.ssl = ssl;
    if ((portProperties == null) || (portProperties.length <= 0)) this.portProperties = new String[] { ssl ? "jppf.management.ssl.port" : "jppf.management.port" };
    else this.portProperties = portProperties;
  }

  @Override
  public void start(final ClassLoader cl) throws Exception {
    if (debugEnabled) log.debug("starting remote connector server");
    ClassLoader tmp = Thread.currentThread().getContextClassLoader();
    lock.lock();
    try {
      Thread.currentThread().setContextClassLoader(cl);
      server = ManagementFactory.getPlatformMBeanServer();
      TypedProperties config = JPPFConfiguration.getProperties();
      managementHost = NetworkUtils.getManagementHost();
      managementPort = new ConfigurationHelper(config).getInt(ssl ? 11193 : 11198, 1024, 65535, portProperties);
      if (debugEnabled) log.debug("managementPort={}, portProperties={}", managementPort, Arrays.asList(portProperties));
      Map<String, Object> env = new HashMap<>();
      env.put("jmx.remote.default.class.loader", cl);
      env.put("jmx.remote.protocol.provider.class.loader", cl);
      env.put("jmx.remote.x.server.max.threads", 1);
      env.put("jmx.remote.x.client.connection.check.period", 0);
      // remove the "JMX server connection timeout Thread-*" threads. See bug http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-249
      env.put("jmx.remote.x.server.connection.timeout", Long.MAX_VALUE);
      if (ssl) SSLHelper.configureJMXProperties(env);
      env.put(GenericConnector.OBJECT_WRAPPING, new CustomWrapping());
      boolean found = false;
      JMXServiceURL url = null;
      while (!found) {
        try {
          InetAddress addr = InetAddress.getByName(managementHost);
          String host = (addr instanceof Inet6Address) ? "[" + managementHost + "]" : managementHost;
          url = new JMXServiceURL("service:jmx:jmxmp://" + host + ':' + managementPort);
          connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(url, env, server);
          connectorServer.start();
          found = true;
        } catch(Exception e) {
          String s = e.getMessage();
          if ((e instanceof BindException) || ((s != null) && (s.toLowerCase().contains("bind")))) {
            if (managementPort >= 65530) managementPort = 1024;
            managementPort++;
          }
          else throw e;
        }
      }
      stopped = false;
      if (debugEnabled) log.debug("JMXConnectorServer started at URL " + url);
    } finally {
      lock.unlock();
      Thread.currentThread().setContextClassLoader(tmp);
    }
  }
}
