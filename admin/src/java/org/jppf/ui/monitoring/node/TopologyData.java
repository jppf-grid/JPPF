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

package org.jppf.ui.monitoring.node;

import java.util.concurrent.atomic.*;

import org.jppf.client.JPPFClientConnection;
import org.jppf.management.*;
import org.jppf.management.diagnostics.*;
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
import org.jppf.ui.monitoring.topology.TopologyDataStatus;
import org.slf4j.*;

/**
 * Instances of this class represent the state of a node in the Topology panel tree.
 * @author Laurent Cohen
 */
public class TopologyData {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(TopologyData.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The type of this object: driver or node.
   */
  private TopologyDataType type = null;
  /**
   * The status of the node.
   */
  private TopologyDataStatus status = TopologyDataStatus.UP;
  /**
   * A driver connection.
   */
  private JPPFClientConnection clientConnection = null;
  /**
   * Forwards node management requests via the driver.
   */
  private JPPFNodeForwardingMBean nodeForwarder = null;
  /**
   * Driver diagnostics MBean proxy.
   */
  private DiagnosticsMBean diagnostics = null;
  /**
   * Wrapper holding the connection to the JMX server on a driver or a node.
   */
  private AtomicReference<JMXDriverConnectionWrapper> jmxWrapper = new AtomicReference<>(null);
  /**
   * Information on the JPPF node .
   */
  private JPPFManagementInfo nodeInformation = null;
  /**
   * Object describing the current state of a node.
   */
  private JPPFNodeState nodeState = null;
  /**
   * Object describing the current health of a node or driver.
   */
  private HealthSnapshot healthSnapshot = new HealthSnapshot();
  /**
   * Determines whether the corresponding driver is collapsed in the visualization panel.
   */
  private boolean collapsed = false;
  /**
   * UUID of the driver or node reprsented by this object.
   */
  private String uuid = null;
  /**
   * The parent driver for a node or peer.
   */
  protected TopologyData parent = null;
  /**
   * 
   */
  private AtomicBoolean initializing = new AtomicBoolean(false);
  /**
   * The number of slaves for a master node.
   */
  private int nbSlaveNodes = -1;

  /**
   * Initialize this topology data as a driver related object.
   * @param clientConnection a reference to the driver connection.
   */
  public TopologyData(final JPPFClientConnection clientConnection) {
    this.type = TopologyDataType.DRIVER;
    this.clientConnection = clientConnection;
    this.jmxWrapper.set(clientConnection.getConnectionPool().getJmxConnection());
    this.uuid = clientConnection.getDriverUuid();
    initializeProxies();
  }

  /**
   * Initialize this topology data as holding information about a node.
   * @param nodeInformation information on the JPPF node.
   */
  public TopologyData(final JPPFManagementInfo nodeInformation) {
    this.type = TopologyDataType.NODE;
    this.nodeInformation = nodeInformation;
    this.nodeState = new JPPFNodeState();
    this.uuid = nodeInformation.getUuid();
  }

  /**
   * Initialize this topology data as holding information about a peer node.
   * @param nodeInformation information on the JPPF peer node.
   * @param peerJmx the JMX wrapper associated with the driver information this peer node represents.
   */
  public TopologyData(final JPPFManagementInfo nodeInformation, final JMXDriverConnectionWrapper peerJmx) {
    this.type = TopologyDataType.PEER;
    this.nodeInformation = nodeInformation;
    this.nodeState = new JPPFNodeState();
    this.jmxWrapper.set(peerJmx != null ? peerJmx : new JMXDriverConnectionWrapper(nodeInformation.getHost(), nodeInformation.getPort(), nodeInformation.isSecure()));
    this.uuid = nodeInformation.getUuid();
  }

  /**
   * Get the wrapper holding the connection to the JMX server on a driver or node.
   * @return a <code>JMXDriverConnectionWrapper</code> instance.
   */
  public JMXDriverConnectionWrapper getJmxWrapper() {
    return jmxWrapper.get();
  }

  /**
   * Set the wrapper holding the connection to the JMX server on a driver or node.
   * @param jmxWrapper a <code>JMXDriverConnectionWrapper</code> instance.
   */
  public void setJmxWrapper(final JMXDriverConnectionWrapper jmxWrapper) {
    this.jmxWrapper.set(jmxWrapper);
  }

  /**
   * Get the information on a JPPF node.
   * @return a <code>NodeManagementInfo</code> instance.
   */
  public JPPFManagementInfo getNodeInformation() {
    return nodeInformation;
  }

  /**
   * Get a string representation of this object.
   * @return a string displaying the host and port of the underlying jmx connection.
   */
  @Override
  public String toString() {
    return (type == TopologyDataType.NODE) ? nodeInformation.getHost() + ':' + nodeInformation.getPort() : (jmxWrapper.get() != null ? jmxWrapper.get().getDisplayName(): "?");
  }

  /**
   * Get the object describing the current state of a node.
   * @return a <code>JPPFNodeState</code> instance.
   */
  public JPPFNodeState getNodeState() {
    return nodeState;
  }

  /**
   * Refresh the state of the node represented by this topology data.
   * @param newState the new node state fetched from the grid.
   */
  public void refreshNodeState(final JPPFNodeState newState) {
    if (!TopologyDataType.NODE.equals(type)) return;
    this.nodeState = newState;
    setStatus(this.nodeState == null ? TopologyDataStatus.DOWN : TopologyDataStatus.UP);
  }

  /**
   * Get the object describing the health of a node or driver.
   * @return a <code>HealthSnapshot</code> instance.
   */
  public HealthSnapshot getHealthSnapshot() {
    return healthSnapshot;
  }

  /**
   * Refresh the health snapshot state of the driver or node represented by this topology data.
   * @param newSnapshot the new health snapshot fetched from the grid.
   */
  public void refreshHealthSnapshot(final HealthSnapshot newSnapshot) {
    if (isPeer()) return;
    this.healthSnapshot = newSnapshot;
  }

  /**
   * Get the driver connection.
   * @return a <code>JPPFClientConnection</code> instance.
   */
  public JPPFClientConnection getClientConnection() {
    return clientConnection;
  }

  /**
   * Set the driver connection.
   * @param clientConnection a <code>JPPFClientConnection</code> instance.
   */
  public void setClientConnection(final JPPFClientConnection clientConnection) {
    this.clientConnection = clientConnection;
  }

  /**
   * Get the status of the node.
   * @return the node status.
   */
  public TopologyDataStatus getStatus() {
    return status;
  }

  /**
   * Set the status of the node.
   * @param status the node status.
   */
  public void setStatus(final TopologyDataStatus status) {
    if (status == TopologyDataStatus.DOWN)
    this.status = status;
  }

  /**
   * Get the id of this topology element.
   * @return the id as a string.
   */
  public String getId() {
    return (jmxWrapper.get() == null) ? null : jmxWrapper.get().getId();
  }

  /**
   * Determine whether this object represents a driver.
   * @return <code>true</code> if this object represets a driver, <code>false</code> otherwise.
   */
  public boolean isDriver() {
    return type == TopologyDataType.DRIVER;
  }

  /**
   * Determine whether this object represents a node.
   * @return <code>true</code> if this object represets a node, <code>false</code> otherwise.
   */
  public boolean isNode() {
    return type == TopologyDataType.NODE;
  }

  /**
   * Determine whether this object represents a peer driver.
   * @return <code>true</code> if this object represets a peer driver, <code>false</code> otherwise.
   */
  public boolean isPeer() {
    return type == TopologyDataType.PEER;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    TopologyData other = (TopologyData) obj;
    if (uuid == null) return other.uuid == null;
    return uuid.equals(other.uuid);
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
   * Get the UUID of the driver or node reprsented by this object.
   * @return the uuid as a string.
   */
  public String getUuid() {
    return uuid;
  }

  /**
   * Set the connection uuid.
   * @param uuid the uuid as a string.
   */
  public void setUuid(final String uuid) {
    this.uuid = uuid;
  }

  /**
   * Get the proxy to the driver MBean that forwards node management requests.
   * @return an instance of {@link JPPFNodeForwardingMBean}.
   */
  public JPPFNodeForwardingMBean getNodeForwarder() {
    return nodeForwarder;
  }

  /**
   * Get the driver diagnostics MBean proxy.
   * @return an instance of {@link DiagnosticsMBean}.
   */
  public DiagnosticsMBean getDiagnostics() {
    return diagnostics;
  }

  /**
   * Get the parent driver data.
   * @return a <code>TopologyData</code> instance, or null if no parent was set.
   */
  public TopologyData getParent() {
    return parent;
  }

  /**
   * Set the parent driver data.
   * @param parent a <code>TopologyData</code> instance, or null to remove the parent.
   */
  public void setParent(final TopologyData parent) {
    this.parent = parent;
  }

  /**
   * Reset the forwarder and diagnostics mbeans. This method should be called when an I/O error occurs
   * when invoking a method of the driver jmx connection wwrapper.
   */
  public void initializeProxies() {
    if (initializing.compareAndSet(false, true)) {
      nodeForwarder = null;
      diagnostics = null;
      new Thread(new ProxySettingTask()).start();
    }
  }

  /**
   * Initialize the driver proxies in a separate thread.
   */
  private class ProxySettingTask implements Runnable {
    @Override
    public void run() {
      if (debugEnabled) log.debug("driverData={}, jmxWrapper={}", this, jmxWrapper);
      try {
        boolean hasNullProxy = true;
        while (hasNullProxy) {
          if (jmxWrapper.get() != null) {
            try {
              if (nodeForwarder == null) nodeForwarder = jmxWrapper.get().getNodeForwarder();
            } catch (Exception ignore) {
            }
            try {
              if (diagnostics == null) diagnostics = jmxWrapper.get().getProxy(DiagnosticsMBean.MBEAN_NAME_DRIVER, DiagnosticsMBean.class);
            } catch (Exception ignore) {
            }
            hasNullProxy = (nodeForwarder == null) || (diagnostics == null);
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

  /**
   * Get the number of slaves for a master node.
   * @return the number of slaves as an int.
   */
  public int getNbSlaveNodes() {
    return nbSlaveNodes;
  }

  /**
   * Set the number of slaves for a master node.
   * @param nbSlaveNodes the number of slaves as an int.
   */
  public void setNbSlaveNodes(final int nbSlaveNodes) {
    this.nbSlaveNodes = nbSlaveNodes;
  }
}
