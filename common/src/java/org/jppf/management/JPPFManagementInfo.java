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

package org.jppf.management;

import java.io.Serializable;
import java.util.*;

import org.jppf.utils.*;


/**
 * Instances of this class encapsulate the information required to access
 * the JMX server of a node.
 * @author Laurent Cohen
 */
public class JPPFManagementInfo implements Serializable, Comparable<JPPFManagementInfo> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * DRIVER information type.
   * @exclude
   */
  public static final byte DRIVER = 0;
  /**
   * Node information type.
   * @exclude
   */
  public static final byte NODE = 1;
  /**
   * Peer driver information type.
   * @exclude
   */
  public static final byte PEER = 2;
  /*
   * Extended attributes must have their 0-3 bits set to 0.
   */
  /**
   * Information that the node is a master node for the provisioning feature.
   */
  public static final byte MASTER = 0x10;
  /**
   * Information that the node is a slave node for the provisioning feature.
   */
  public static final byte SLAVE = 0x20;
  /**
   * Information that node is local on DRIVER or CLIENT. Value of this constant can be changed in future!
   */
  public static final byte LOCAL = 0x40;
  /**
   * Mask for elimination extended type attributes.
   */
  protected static final byte TYPE_MASK = 0x0F;
  /**
   * Maps type values to readable strings.
   */
  private static final Map<Byte, String> typeMap = new HashMap<Byte, String>() {{
    put(DRIVER, "driver");
    put(NODE, "node");
    put(PEER, "peer");
  }};
  /**
   * The name of the host on which the node or driver is running.
   */
  private final String host;
  /**
   * The ip address of the host on which the node or driver is running.
   * @since 5.0
   */
  private final String ipAddress;
  /**
   * The port on which the node's JMX server is listening.
   */
  private final int port;
  /**
   * Unique id for the node.
   */
  private final String uuid;
  /**
   * The type of component this info is for, must be one of {@link #NODE} or {@link #DRIVER}.
   */
  private final byte type;
  /**
   * Determines whether communication with the node or driver should be secure, i.e. via SSL/TLS.
   */
  private final boolean secure;
  /**
   * The system information associated with the node at the time of the initial connection.
   */
  private transient JPPFSystemInformation systemInfo = null;
  /**
   * Determines whether the node is active or inactive.
   */
  private boolean active = true;

  /**
   * Initialize this information with the specified parameters.
   * @param host the host on which the node or driver is running.
   * @param port the port on which the node's or driver's JMX server is listening.
   * @param uuid unique id of the node or driver.
   * @param type the type of component this info is for, must be one of {@link #NODE NODE} or {@link #DRIVER DRIVER}.
   * @param secure specifies whether communication with the node or driver should be secure, i.e. via SSL/TLS.
   * @exclude
   */
  public JPPFManagementInfo(final String host, final int port, final String uuid, final int type, final boolean secure) {
    this(NetworkUtils.getHostIP(host), port, uuid, type, secure);
  }

  /**
   * Initialize this information with the specified parameters.
   * @param host the name of the host on which the node or driver is running.
   * @param ip the ip address of the host on which the node is running.
   * @param port the port on which the node's or driver's JMX server is listening.
   * @param uuid unique id of the node or driver.
   * @param type the type of component this info is for, must be one of {@link #NODE NODE} or {@link #DRIVER DRIVER}.
   * @param secure specifies whether communication with the node or driver should be secure, i.e. via SSL/TLS.
   * @since 5.0
   * @exclude
   */
  public JPPFManagementInfo(final String host, final String ip, final int port, final String uuid, final int type, final boolean secure) {
    this(new HostIP(host, ip), port, uuid, type, secure);
  }

  /**
   * Initialize this information with the specified parameters.
   * @param hostIP encapsulates the host name and ip address of host on which the driver or node is running.
   * @param port the port on which the node's or driver's JMX server is listening.
   * @param uuid unique id of the node or driver.
   * @param type the type of component this info is for, must be one of {@link #NODE NODE} or {@link #DRIVER DRIVER}.
   * @param secure specifies whether communication with the node or driver should be secure, i.e. via SSL/TLS.
   * @since 5.0
   * @exclude
   */
  public JPPFManagementInfo(final HostIP hostIP, final int port, final String uuid, final int type, final boolean secure) {
    this.host = hostIP.hostName();
    this.ipAddress = hostIP.ipAddress();
    this.port = port;
    this.uuid = uuid;
    this.type = (byte) type;
    this.secure = secure;
  }

  /**
   * Get the host on which the node is running.
   * @return the host as a string.
   */
  public synchronized String getHost() {
    return host;
  }

  /**
   * Get the port on which the node's JMX server is listening.
   * @return the port as an int.
   */
  public synchronized int getPort() {
    return port;
  }

  @Override
  public int hashCode() {
    return (uuid == null) ? 0 : uuid.hashCode();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    final JPPFManagementInfo other = (JPPFManagementInfo) obj;
    if (other.uuid == null) return uuid == null;
    return (uuid != null) && uuid.equals(other.uuid);
  }

  @Override
  public int compareTo(final JPPFManagementInfo o) {
    if (o == null) return 1;
    if (this.equals(o)) return 0;
    // we want ascending alphabetical order
    int n = -1 * host.compareTo(o.getHost());
    if (n != 0) return n;
    return port - o.getPort();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append(host).append(':').append(port);
    sb.append(", type=").append(typeToString());
    sb.append(", local=").append(isLocal());
    sb.append(", secure=").append(secure);
    sb.append(", uuid=").append(uuid);
    sb.append(']');
    return sb.toString();
  }

  /**
   * Get a string displayed in the console.
   * @return .
   * @exclude
   */
  public String toDisplayString() {
    return host + ':' + port;
  }

  /**
   * Get the system information associated with the node at the time of the initial connection.
   * @return a <code>JPPFSystemInformation</code> instance.
   */
  public synchronized JPPFSystemInformation getSystemInfo() {
    return systemInfo;
  }

  /**
   * Set the system information associated with the node at the time of the initial connection.
   * @param systemInfo a <code>JPPFSystemInformation</code> instance.
   * @exclude
   */
  public synchronized void setSystemInfo(final JPPFSystemInformation systemInfo) {
    this.systemInfo = systemInfo;
  }

  /**
   * Get the unique id for the node's mbean server.
   * @return the id as a string.
   */
  public String getUuid() {
    return uuid;
  }

  /**
   * Determine whether this information represents a connection to peer driver.
   * @return <code>true</code> if this information represents a peer driver, <code>false</code> otherwise.
   */
  public boolean isPeer() {
    return (type & TYPE_MASK) == PEER;
  }

  /**
   * Determine whether this information represents a real node.
   * @return <code>true</code> if this information represents a node, <code>false</code> otherwise.
   */
  public boolean isNode() {
    return (type & TYPE_MASK) == NODE;
  }

  /**
   * Determine whether this information represents a driver, connected as a peer to the
   * driver from which this information is obtained.
   * @return <code>true</code> if this information represents a driver, <code>false</code> otherwise.
   */
  public boolean isDriver() {
    return (type & TYPE_MASK) == DRIVER;
  }

  /**
   * Determine whether communication with the node or driver is be secure, i.e. via SSL/TLS.
   * @return <code>true</code> if the connection is secure, <code>false</code> otherwise.
   */
  public boolean isSecure() {
    return secure;
  }

  /**
   * Determine whether this information represents a master node for provisioning.
   * @return <code>true</code> if the node is a master node.
   */
  public boolean isMasterNode() {
    return (type & MASTER) == MASTER;
  }

  /**
   * Determine whether this information represents a slave node for provisioning.
   * @return <code>true</code> if the node is a master node.
   */
  public boolean isSlaveNode() {
    return (type & SLAVE) == SLAVE;
  }

  /**
   * Determine whether this information represents a local node on client or driver.
   * @return <code>true</code>
   */
  public boolean isLocal() {
    return (type & LOCAL) == LOCAL;
  }

  /**
   * Determine whether the node is active or inactive.
   * @return <code>true</code> if the node is active, <code>false</code> if it is inactve.
   */
  public boolean isActive() {
    return active;
  }

  /**
   * Specify whether the node is active or inactive.
   * @param active <code>true</code> if the node is active, <code>false</code> if it is inactve.
   * @exclude
   */
  public void setActive(final boolean active) {
    this.active = active;
  }

  /**
   * Get a string representation of the type.
   * @return a string representing the type.
   */
  private String typeToString() {
    byte b = (byte) (type & TYPE_MASK);
    StringBuilder sb = new StringBuilder();
    String s = typeMap.get(b);
    sb.append(s == null ? "?" : s);
    if (isMasterNode()) sb.append("|MASTER");
    if (isSlaveNode()) sb.append("|SLAVE");
    if (isLocal()) sb.append("|LOCAL");
    return sb.toString();
  }

  /**
   * Get the ip address of the host on which the node or driver is running.
   * @return the ip address as a string.
   * @since 5.0
   */
  public String getIpAddress() {
    return ipAddress;
  }
}
