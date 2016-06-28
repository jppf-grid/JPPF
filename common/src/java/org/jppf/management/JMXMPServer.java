/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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
import java.lang.reflect.*;
import java.net.*;
import java.util.*;

import javax.management.remote.*;

import org.jppf.ssl.SSLHelper;
import org.jppf.utils.*;
import org.jppf.utils.configuration.*;
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
  private final JPPFProperty<Integer> portProperty;
  /**
   * An optional {@link MBeanServerForwarder} associated with the {@code JMXConnectorServer}.
   */
  private MBeanServerForwarder forwarder;

  /**
   * Initialize this JMX server with the specified uuid.
   * @param id the unique id of the driver or node holding this jmx server.
   * @param ssl specifies whether JMX should be used over an SSL/TLS connection.
   * @param portProperty an ordered set of configuration properties to use for looking up the desired management port.
   * @exclude
   */
  public JMXMPServer(final String id, final boolean ssl, final JPPFProperty<Integer> portProperty) {
    this.id = id;
    this.ssl = ssl;
    if (portProperty == null) this.portProperty = ssl ? JPPFProperties.MANAGEMENT_SSL_PORT : JPPFProperties.MANAGEMENT_SSL_PORT;
    else this.portProperty = portProperty;
  }

  /**
   * {@inheritDoc]}
   * @exclude
   */
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
      managementPort = config.get(portProperty);
      if (debugEnabled) log.debug("managementPort={}, portProperties={}", managementPort, Arrays.asList(portProperty));
      Map<String, Object> env = new HashMap<>();
      env.put("jmx.remote.default.class.loader", cl);
      env.put("jmx.remote.protocol.provider.class.loader", cl);
      env.put("jmx.remote.x.server.max.threads", 1);
      env.put("jmx.remote.x.client.connection.check.period", 0);
      // remove the "JMX server connection timeout Thread-*" threads. See bug http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-249
      env.put("jmx.remote.x.server.connection.timeout", Long.MAX_VALUE);
      if (ssl) SSLHelper.configureJMXProperties(env);
      env.put("jmx.remote.object.wrapping", new CustomWrapping());
      boolean found = false;
      JMXServiceURL url = null;
      InetAddress addr = InetAddress.getByName(managementHost);
      String host = String.format((addr instanceof Inet6Address) ? "[%s]" : "%s", addr.getHostAddress());
      while (!found) {
        try {
          url = new JMXServiceURL("service:jmx:jmxmp://" + host + ':' + managementPort);
          connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(url, env, server);
          connectorServer.start();
          found = true;
          forwarder = createMBeanServerForwarder();
          if (forwarder != null) connectorServer.setMBeanServerForwarder(forwarder);
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

  /**
   * Create an MBeanServerForwarder to associate witht he remote connector server,
   * based on the JPPF configuration.
   * @return an {@link MBeanServerForwarder} instance, or {@code null} if none is defined in the configuration or if it couldn't be created.
   */
  private MBeanServerForwarder createMBeanServerForwarder() {
    String[] configDef = JPPFConfiguration.get(JPPFProperties.MANAGEMENT_SERVER_FORWARDER);
    if ((configDef == null) || (configDef.length <= 0)) return null;
    Class<?> clazz = null;
    try {
      clazz = Class.forName(configDef[0]);
    } catch (ClassNotFoundException e) {
      log.error(String.format("The MBeanServerForwarder class %s was not found, no fowrader will be set. Error is: %s", configDef[0], ExceptionUtils.getStackTrace(e)));
      return null;
    }
    if (!MBeanServerForwarder.class.isAssignableFrom(clazz)) {
      log.error(String.format("The configured class '%s' for property '%s' does not implement %s, no forwarder will be set.",
        clazz.getName(), JPPFProperties.MANAGEMENT_SERVER_FORWARDER.getName(), MBeanServerForwarder.class.getName()));
      return null;
    }
    MBeanServerForwarder forwarder = null;
    Object[] params = null;
    if (configDef.length > 1) {
      params = new String[configDef.length - 1];
      for (int i=1; i<configDef.length; i++) {
        params[i-1] = configDef[i];
      }
    }
    Constructor c = null;
    if (params != null) {
      try {
        c = clazz.getConstructor(String[].class);
      } catch (NoSuchMethodException ignore) {
      }
      if (c != null) {
        try {
          return (MBeanServerForwarder) c.newInstance((Object) params);
        } catch (Exception e) {
          log.error(String.format("The constructor '%s.<init>(String[])' raised an exception: %s", clazz.getName(), ExceptionUtils.getStackTrace(e)));
          return null;
        }
      }
    }
    try {
      forwarder = (MBeanServerForwarder) clazz.newInstance();
    } catch (Exception e) {
      log.error(String.format("Could not instantiate '%s' : %s", clazz.getName(), ExceptionUtils.getStackTrace(e)));
      return null;
    }
    if (params != null) {
      Method m = null;
      try {
        m = clazz.getMethod("setParameters", String[].class);
      } catch (Exception ignore) {
      }
      if (m != null) {
        try {
          m.invoke(forwarder, (Object) params);
        } catch (Exception e) {
          log.error(String.format("Invoking %s.setParameters(String[]) failed : %s", clazz.getName(), ExceptionUtils.getStackTrace(e)));
        }
      }
    }
    return forwarder;
  }

  @Override
  public MBeanServerForwarder getMBeanServerForwarder() {
    return  forwarder;
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public void stop() throws Exception {
    super.stop();
    forwarder = null;
  }
}
