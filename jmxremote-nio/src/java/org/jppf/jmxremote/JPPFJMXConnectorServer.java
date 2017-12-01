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

package org.jppf.jmxremote;

import java.io.*;
import java.util.*;

import javax.management.MBeanServer;
import javax.management.remote.*;

import org.jppf.jmxremote.nio.JMXNioServer;
import org.jppf.nio.NioHelper;
import org.jppf.utils.ExceptionUtils;
import org.slf4j.*;

/**
 * Concrete subclass of the {@link JMXConnectorServer} class for the JPPF JMX remote connector server.
 * @author Laurent Cohen
 */
public class JPPFJMXConnectorServer extends JMXConnectorServer implements JMXConnectionStatusListener {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFJMXConnectorServer.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The environment key for the MBean server.
   */
  public static final String MBEAN_SERVER_KEY = "jppf.jmxremote.internal.mbeanserver";
  /**
   * The environment for this connector.
   */
  private final Map<String, Object> environment = new HashMap<>();
  /**
   * The address of this connector.
   */
  private final JMXServiceURL address;
  /**
   * Whether this connector server is started.
   */
  private boolean started = false;

  /**
   *
   * @param serviceURL the address of this connector.
   * @param environment the environment for this connector.
   * @param mbeanServer .
   */
  public JPPFJMXConnectorServer(final JMXServiceURL serviceURL, final Map<String, ?> environment, final MBeanServer mbeanServer) {
    super(mbeanServer);
    if (environment != null) this.environment.putAll(environment);
    this.environment.put(MBEAN_SERVER_KEY, mbeanServer);
    this.address = serviceURL;
  }

  @Override
  public void start() throws IOException {
    if (debugEnabled) log.debug("starting server @{}, env={}", address, environment);
    try {
      JMXNioServer server = JMXNioServer.getInstance();
      if (debugEnabled) log.debug("Got JMXNioServer instance {}", server);
      int port = address.getPort();
      Boolean tls = (Boolean) environment.get("jppf.jmx.remote.tls.enabled");
      boolean secure = (tls == null) ? false : tls;
      NioHelper.getAcceptorServer().addServer(port, secure, environment);
      server.addConnectionStatusListener(this);
      started = true;
      if (debugEnabled) log.debug("successfully started server @{}", address);
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public void stop() throws IOException {
    if (!started) return;
    started = false;
    JMXNioServer server = JMXNioServer.getInstance();
    try {
      server.removeAllConnections(address.getPort());
    } finally {
      server.removeConnectionStatusListener(this);
    }
  }

  @Override
  public boolean isActive() {
    return started;
  }

  @Override
  public JMXServiceURL getAddress() {
    return address;
  }

  @Override
  public Map<String, ?> getAttributes() {
    Map<String, Object> map = new HashMap<>();
    synchronized(environment) {
      for (Map.Entry<String, ?> entry: environment.entrySet())
        if (entry.getValue() instanceof Serializable) map.put(entry.getKey(), entry.getValue());
    }
    return Collections.unmodifiableMap(map);
  }

  @Override
  public void connectionOpened(final JMXConnectionStatusEvent event) {
    if (debugEnabled) log.debug("server @{} connection opened event = {}", address, event);
    connectionOpened(event.getConnectionID(), "connection opened", null);
  }

  @Override
  public void connectionClosed(final JMXConnectionStatusEvent event) {
    if (debugEnabled) log.debug("server @{} connection closed event = {}", address, event);
    connectionClosed(event.getConnectionID(), "connection closed", null);
  }

  @Override
  public void connectionFailed(final JMXConnectionStatusEvent event) {
    if (debugEnabled) log.debug("server @{} connection failed event = {}", address, event);
    connectionFailed(event.getConnectionID(), "connection failed", ExceptionUtils.getStackTrace(event.getThrowable()));
  }
}
