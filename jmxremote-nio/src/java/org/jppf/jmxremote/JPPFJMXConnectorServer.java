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

/**
 * 
 * @author Laurent Cohen
 */
public class JPPFJMXConnectorServer extends JMXConnectorServer implements JMXConnectionStatusListener {
  /**
   * The environment for this connector.
   */
  private final Map<String, ?> environment;
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
  public JPPFJMXConnectorServer(final JMXServiceURL serviceURL, final Map<String, ?> environment, MBeanServer mbeanServer) {
    super(mbeanServer);
    this.environment = (environment == null) ? new HashMap<String, Object>() : environment;
    this.address = serviceURL;
  }

  @Override
  public void start() throws IOException {
    try {
      JMXNioServer server = JMXNioServer.getInstance();
      int port = address.getPort();
      Boolean tls = (Boolean) environment.get("jppf.jmx.remote.tls.enabled");
      boolean secure = (tls == null) ? false : tls;
      NioHelper.getAcceptorServer().addServer(port, secure, environment);
      server.addConnectionStatusListener(this);
      started = true;
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
    try {
      JMXNioServer.getInstance().removeAllConnections(address.getPort());
    } finally {
      JMXNioServer.getInstance().removeConnectionStatusListener(this);
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
      for (Map.Entry<String, ?> entry: environment.entrySet()) {
        if (entry.getValue() instanceof Serializable) map.put(entry.getKey(), entry.getValue());
      }
    }
    return Collections.unmodifiableMap(map);
  }

  @Override
  public void connectionOpened(JMXConnectionStatusEvent event) {
    connectionOpened(event.getConnectionID(), "connection opened", null);
  }

  @Override
  public void connectionClosed(JMXConnectionStatusEvent event) {
    connectionOpened(event.getConnectionID(), "connection closed", null);
  }

  @Override
  public void connectionFailed(JMXConnectionStatusEvent event) {
    connectionOpened(event.getConnectionID(), "connection failed", ExceptionUtils.getStackTrace(event.getThrowable()));
  }
}
