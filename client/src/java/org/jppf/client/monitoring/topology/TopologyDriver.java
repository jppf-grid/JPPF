/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.client.monitoring.topology;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.client.JPPFClientConnection;
import org.jppf.management.*;
import org.jppf.management.diagnostics.DiagnosticsMBean;
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
import org.slf4j.*;

/**
 * Implementation of {@link TopologyDriver} for JPPF drivers.
 * @author Laurent Cohen
 * @since 5.0
 */
public class TopologyDriver extends AbstractTopologyComponent {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(TopologyDriver.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether the corresponding driver is collapsed in the visualization panel.
   */
  private boolean collapsed = false;
  /**
   * A driver connection.
   */
  private JPPFClientConnection connection = null;
  /**
   * Forwards node management requests via the driver.
   */
  private JPPFNodeForwardingMBean forwarder = null;
  /**
   * Determines whether this driver copmonent is currently initializing its JMX connection and mbean proxies.
   */
  private AtomicBoolean initializing = new AtomicBoolean(false);
  /**
   * Driver diagnostics MBean proxy.
   */
  protected DiagnosticsMBean diagnostics;

  /**
   * Initialize this topology data as a driver related object.
   * @param clientConnection a reference to the driver connection.
   */
  public TopologyDriver(final JPPFClientConnection clientConnection) {
    this.connection = clientConnection;
    this.uuid = clientConnection.getDriverUuid();
    initializeProxies();
  }

  @Override
  public boolean isDriver() {
    return true;
  }

  /**
   * Get the wrapper holding the connection to the JMX server on a driver or node.
   * @return a <code>JMXDriverConnectionWrapper</code> instance.
   */
  public JMXDriverConnectionWrapper getJmx() {
    return (connection == null) ? null : connection.getConnectionPool().getJmxConnection();
  }

  /**
   * Get the driver connection.
   * @return a {@link JPPFClientConnection} instance.
   */
  public JPPFClientConnection getConnection() {
    return connection;
  }

  /**
   * Set the driver connection.
   * @param connection a {@link JPPFClientConnection} instance.
   */
  public void setConnection(final JPPFClientConnection connection) {
    this.connection = connection;
  }

  /**
   * Determine whether the corresponding driver is collapsed in the visualization panel.
   * @return <code>true</code> if the driver is collapsed, <code>false</code> otherwise.
   */
  public boolean isCollapsed() {
    return collapsed;
  }

  /**
   * Specify whether the corresponding driver is collapsed in the visualization panel.
   * @param collapsed <code>true</code> if the driver is collapsed, <code>false</code> otherwise.
   */
  public void setCollapsed(final boolean collapsed) {
    this.collapsed = collapsed;
  }

  /**
   * Get the proxy to the driver MBean that forwards node management requests.
   * @return an instance of {@link JPPFNodeForwardingMBean}.
   */
  public JPPFNodeForwardingMBean getForwarder() {
    return forwarder;
  }

  /**
   * Gert the diagnostics mbean for this driver.
   * @return a {@link DiagnosticsMBean} instance.
   */
  public DiagnosticsMBean getDiagnostics() {
    JMXDriverConnectionWrapper jmx = getJmx();
    if ((jmx != null) && jmx.isConnected()) {
      try {
        return jmx.getProxy(DiagnosticsMBean.MBEAN_NAME_DRIVER, DiagnosticsMBean.class);
      } catch (Exception ignore) {
      }
    }
    return null;
  }


  @Override
  public String toString() {
    JMXDriverConnectionWrapper jmx = getJmx();
    return (jmx == null) ? "?" : jmx.getDisplayName();
  }

  /**
   * Reset the forwarder and diagnostics mbeans. This method should be called when an I/O error occurs
   * when invoking a method of the driver jmx connection wwrapper.
   */
  public void initializeProxies() {
    if (initializing.compareAndSet(false, true)) {
      forwarder = null;
      diagnostics = null;
      new Thread(new ProxySettingTask(), "@" + id + ":proxies").start();
    }
  }

  /**
   * Initialize the driver proxies in a separate thread.
   */
  private class ProxySettingTask implements Runnable {
    @Override
    public void run() {
      if (debugEnabled) log.debug("driverData={}, jmx={}", this, getJmx());
      try {
        boolean hasNullProxy = true;
        while (hasNullProxy) {
          JMXDriverConnectionWrapper jmx = getJmx();
          if (jmx != null) {
            if (getManagementInfo() == null) setManagementInfo(new JPPFManagementInfo(jmx.getHost(), jmx.getPort(), jmx.getId(), JPPFManagementInfo.DRIVER, jmx.isSecure()));
            try {
              if (forwarder == null) forwarder = jmx.getNodeForwarder();
            } catch (Exception ignore) {
            }
            try {
              if (diagnostics == null) diagnostics = jmx.getProxy(DiagnosticsMBean.MBEAN_NAME_DRIVER, DiagnosticsMBean.class);
            } catch (Exception ignore) {
            }
            hasNullProxy = (forwarder == null) || (diagnostics == null);
          }
          try {
            if (hasNullProxy) Thread.sleep(500L);
          } catch (InterruptedException ignore) {
          }
        }
      } finally {
        initializing.set(false);
      }
    }
  }
}
