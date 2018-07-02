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

package org.jppf.comm.discovery;

import java.io.*;

import org.jppf.utils.*;

/**
 * This class encapsulates the connection information for a JPPF driver.
 * The information includes the host, class server, application and node server ports.
 * @author Laurent Cohen
 */
public class JPPFConnectionInformation implements Serializable, Comparable<JPPFConnectionInformation>, Cloneable {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The host name.
   */
  public String host = null;
  /**
   * The ports on which the server is listening.
   */
  public int[] serverPorts = null;
  /**
   * The SSL ports on which the server is listening.
   */
  public int[] sslServerPorts = null;
  /**
   * Identifier for this object.
   */
  public String uuid = null;
  /**
   * Whether the recovery (aka heartbeat) mechanism is enabled.
   */
  public boolean recoveryEnabled;

  /**
   * Default constructor.
   */
  public JPPFConnectionInformation() {
  }

  /**
   * Determine whether this connection information contains a valid port of the specified type,
   * that is, at least one port of the specified type greater than zero.
   * @param secure {@code true} to specifiy that secure (SSL/TLS) ports must be checked, {@code false} to look for plain ports.
   * @return {@code true} if this information has a valid port, {@code false} otherwise.
   * @since 5.0
   */
  public boolean hasValidPort(final boolean secure) {
    final int[] ports = secure ? sslServerPorts: serverPorts;
    if (ports != null) {
      for (final int port: ports) {
        if (port > 0) return true;
      }
    }
    return false;
  }

  /**
   * Get a valid port of the specified type, that is, the first port of the specified type greater than zero.
   * @param secure {@code true} to specifiy that secure (SSL/TLS) ports must be checked, {@code false} to look for plain ports.
   * @return the value of the first valmid port found if this information has a valid port, {@code -1} otherwise.
   * @since 5.0
   */
  public int getValidPort(final boolean secure) {
    final int[] ports = secure ? sslServerPorts: serverPorts;
    if (ports != null) {
      for (final int port: ports) {
        if (port > 0) return port;
      }
    }
    return -1;
  }

  /**
   * Compare this connection information with another.
   * @param ci the other object to compare to.
   * @return -1 if this connection information is less than the other, 1 if it is greater, 0 if they are equal.
   */
  @Override
  public int compareTo(final JPPFConnectionInformation ci) {
    if ((ci == null) || (ci.uuid == null)) return 1;
    if (uuid == null) return -1;
    return uuid.compareTo(ci.uuid);
  }

  /**
   * Compute the hashcode of this object.
   * @return the hashcode as an int.
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return 31 + (uuid == null ? 0 : uuid.hashCode());
  }

  /**
   * Determine whether this object is equal to another.
   * @param obj the object to compare to.
   * @return true if the 2 objects are equal, false otherwise.
   */
  @Override
  public boolean equals(final Object obj) {
    if (obj == null) return false;
    if (this == obj) return true;
    if (getClass() != obj.getClass()) return false;
    final JPPFConnectionInformation other = (JPPFConnectionInformation) obj;
    if (uuid == null) return other.uuid == null;
    return uuid.equals(other.uuid);
  }

  /**
   * Determine whether this object is equal to another.
   * @param other the object to compare to.
   * @return true if the 2 objects are equal, false otherwise.
   */
  public boolean isSame(final JPPFConnectionInformation other) {
    return isSame(other, true);
  }

  /**
   * Determine whether this object is equal to another.
   * @param other the object to compare to.
   * @param compareUuid whether to compare the uuids.
   * @return true if the 2 objects are equal, false otherwise.
   */
  public boolean isSame(final JPPFConnectionInformation other, final boolean compareUuid) {
    if (other == null) return false;
    if (this == other) return true;
    if (!ComparisonUtils.equalStrings(host, other.host)) return false;
    if (!ComparisonUtils.equalIntArrays(serverPorts, other.serverPorts)) return false;
    if (!ComparisonUtils.equalIntArrays(sslServerPorts, other.sslServerPorts)) return false;
    if (recoveryEnabled != other.recoveryEnabled) return false;
    if (compareUuid) {
      if (uuid == null) return other.uuid == null;
      return uuid.equals(other.uuid);
    }
    return true;
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    final JPPFConnectionInformation ci = new JPPFConnectionInformation();
    ci.uuid = uuid;
    ci.host = host;
    ci.recoveryEnabled = recoveryEnabled;
    if (serverPorts != null) {
      ci.serverPorts = new int[serverPorts.length];
      System.arraycopy(serverPorts, 0, ci.serverPorts, 0, serverPorts.length);
    }
    if (sslServerPorts != null) {
      ci.sslServerPorts = new int[sslServerPorts.length];
      System.arraycopy(sslServerPorts, 0, ci.sslServerPorts, 0, sslServerPorts.length);
    }
    return ci;
  }

  /**
   * Get a string representation of this connection information object.
   * @return a string describing this object.
   */
  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('[');
    sb.append("uuid=").append(uuid);
    sb.append(", host=").append(host);
    sb.append(", recoveryEnabled=").append(recoveryEnabled);
    sb.append(", serverPorts=").append(StringUtils.buildString(serverPorts));
    sb.append(", sslServerPorts=").append(StringUtils.buildString(sslServerPorts));
    sb.append(']');
    return sb.toString();
  }

  /**
   * Deserialize a DriverConnectionInformation object from an array of bytes.
   * @param bytes the array of bytes to deserialize from.
   * @return a <code>DriverConnectionInformation</code> instance.
   * @throws Exception if an error is raised while deserializing.
   */
  public static JPPFConnectionInformation fromBytes(final byte[] bytes) throws Exception {
    JPPFConnectionInformation info = null;
    try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
      info = (JPPFConnectionInformation) ois.readObject();
    }
    return info;
  }

  /**
   * Serialize a DriverConnectionInformation object to an array of bytes.
   * @param info the <code>DriverConnectionInformation</code> object to serialize to.
   * @return an array of bytes.
   * @throws Exception if an error is raised while serializing.
   */
  public static byte[] toBytes(final JPPFConnectionInformation info) throws Exception {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(info);
    }
    return baos.toByteArray();
  }
}
