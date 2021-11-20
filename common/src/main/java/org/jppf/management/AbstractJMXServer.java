/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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
import java.net.BindException;
import java.util.Map;
import java.util.concurrent.locks.*;

import javax.management.MBeanServer;
import javax.management.remote.*;

import org.jppf.utils.*;
import org.jppf.utils.configuration.*;
import org.slf4j.*;

/**
 * This class is a wrapper around a JMX management server.
 * It is used essentially to hide the details of the remote management protocol used.
 * @author Laurent Cohen
 * @exclude
 */
public abstract class AbstractJMXServer implements JMXServer {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractJMXServer.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Used to synchronize lookup for available port.
   */
  protected static Lock lock = new ReentrantLock();
  /**
   * The mbean server.
   */
  protected final MBeanServer mbeanServer;
  /**
   * The JMX connector server.
   */
  protected JMXConnectorServer connectorServer;
  /**
   * Determines whether this JMX server is stopped.
   */
  protected boolean stopped = true;
  /**
   * This server's unique id.
   */
  protected String uuid;
  /**
   * The host interface on which the JMX server is listeneing for connections.
   */
  protected String managementHost;
  /**
   * The port on which the connector is listening for connections from remote clients.
   */
  protected int managementPort = -1;
  /**
   * Determines whether JMX should be used over an SSL/TLS connection.
   */
  protected boolean ssl;
  /**
   * An optional {@link MBeanServerForwarder} associated with the {@code JMXConnectorServer}.
   */
  protected MBeanServerForwarder forwarder;
  /**
   * The configuration to use.
   */
  protected final TypedProperties config;

  /**
   * 
   * @param config the configuration to use.
   * @param mbeanServer the mbean server to use.
   */
  protected AbstractJMXServer(final TypedProperties config, final MBeanServer mbeanServer) {
    this.config = config;
    this.mbeanServer = (mbeanServer == null) ? ManagementFactory.getPlatformMBeanServer() : mbeanServer;
  }

  @Override
  public void stop() throws Exception {
    stopped = true;
    if (connectorServer != null) {
      connectorServer.stop();
      connectorServer = null;
    }
    forwarder = null;
  }

  @Override
  public MBeanServer getMBeanServer() {
    return mbeanServer;
  }

  @Override
  public boolean isStopped() {
    return stopped;
  }

  @Override
  public String getUuid() {
    return uuid;
  }

  @Override
  public int getManagementPort() {
    return managementPort;
  }

  @Override
  public String getManagementHost() {
    return managementHost;
  }

  @Override
  public JMXConnectorServer getConnectorServer() {
    return connectorServer;
  }

  @Override
  public MBeanServerForwarder getMBeanServerForwarder() {
    return  forwarder;
  }

  @Override
  public Map<String, ?> getEnvironment() {
    return connectorServer.getAttributes();
  }

  /**
   * Start the underlying conector server.
   * @param protocol the connector server protocol.
   * @param env the environment parameters passed on to the server.
   * @throws Exception if any eror occyrs.
   */
  protected void startConnectorServer(final String protocol, final Map<String, Object> env) throws Exception {
    boolean found = false;
    int nbTries = 0;
    JMXServiceURL url = null;
    if (debugEnabled) log.debug("starting {} for protocol={}", getClass().getSimpleName(), protocol);
    final JPPFProperty<String[]> prop = JPPFProperties.MANAGEMENT_SERVER_FORWARDER;
    forwarder = ReflectionHelper.invokeDefaultOrStringArrayConstructor(MBeanServerForwarder.class, prop.getName(), config.get(prop));
    if (debugEnabled) log.debug("server forwarder = {}", forwarder);
    while (!found) {
      try {
        url = new JMXServiceURL(protocol,  null, managementPort);
        synchronized(AbstractJMXServer.class) {
          connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(url, env, mbeanServer);
          connectorServer.start();
        }
        found = true;
        managementHost = url.getHost();
        if (forwarder != null) connectorServer.setMBeanServerForwarder(forwarder);
      } catch(final Exception e) {
        nbTries++;
        if (nbTries > 65530 - 1024) throw e;
        if ((e instanceof BindException) || StringUtils.hasOneOf(e.getMessage(), true, "bind", "address already in use")) {
          if (debugEnabled) log.debug("starting {} server on port {} failed at attempt {} because the port is already in use", protocol, managementPort, nbTries);
          if (managementPort >= 65530) managementPort = 1024;
          managementPort++;
        }
        else throw e;
      }
    }
    stopped = false;
    if (debugEnabled) log.debug("{} started at URL {} after {} tries, connector server = {}", getClass().getSimpleName(), url, nbTries, connectorServer);
  }
}
