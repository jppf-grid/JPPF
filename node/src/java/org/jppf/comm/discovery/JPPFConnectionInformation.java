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

package org.jppf.comm.discovery;

import java.io.*;

import org.jppf.node.connection.*;
import org.jppf.utils.StringUtils;
import org.slf4j.*;

/**
 * This class encapsulates the connection information for a JPPF driver.
 * The information includes the host, class server, application and node server ports.
 * @author Laurent Cohen
 */
public class JPPFConnectionInformation implements Serializable, Comparable<JPPFConnectionInformation>, Cloneable
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFConnectionInformation.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
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
   * Port number used for JMX management and monitoring.
   */
  public int managementPort = -1;
  /**
   * Port number used for JMX management and monitoring over a SSL/TLS connection.
   */
  public int sslManagementPort = -1;
  /**
   * Port number for recovery from hardware failures.
   */
  public int recoveryPort = -1;
  /**
   * Host address used for JMX management and monitoring.
   */
  public transient String managementHost = null;
  /**
   * Identifier for this object.
   */
  public String uuid = null;
  /**
   * The length of the subnet mask for the host address.
   */
  public int subnetMaskLength = 0;

  /**
   * Default constructor.
   */
  public JPPFConnectionInformation()
  {
  }

  /**
   * Compare this connection information with another.
   * @param ci the other object to compare to.
   * @return -1 if this connection information is less than the other, 1 if it is greater, 0 if they are equal.
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(final JPPFConnectionInformation ci)
  {
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
  public int hashCode()
  {
    //return managementPort + (host == null ? 0 : host.hashCode());
    return 31 + (uuid == null ? 0 : uuid.hashCode());
  }

  /**
   * Determine whether this object is equal to another.
   * @param obj the object to compare to.
   * @return true if the 2 objects are equal, false otherwise.
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(final Object obj)
  {
    if (obj == null) return false;
    if (this == obj) return true;
    if (getClass() != obj.getClass()) return false;
    JPPFConnectionInformation other = (JPPFConnectionInformation) obj;
    if (uuid == null) return other.uuid == null;
    return uuid.equals(other.uuid);
  }

  @Override
  public Object clone() throws CloneNotSupportedException
  {
    return super.clone();
  }

  /**
   * Get a string representation of this connection information object.
   * @return a string describing this object.
   */
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('[');
    sb.append("uuid=").append(uuid);
    sb.append(", host=").append(host);
    sb.append(", managementPort=").append(managementPort);
    sb.append(", recoveryPort=").append(recoveryPort);
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
  public static JPPFConnectionInformation fromBytes(final byte[] bytes) throws Exception
  {
    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
    JPPFConnectionInformation info = null;
    try
    {
      info = (JPPFConnectionInformation) ois.readObject();
    }
    finally
    {
      ois.close();
    }
    return info;
  }

  /**
   * Serialize a DriverConnectionInformation object to an array of bytes.
   * @param info the <code>DriverConnectionInformation</code> object to serialize to.
   * @return an array of bytes.
   * @throws Exception if an error is raised while serializing.
   */
  public static byte[] toBytes(final JPPFConnectionInformation info) throws Exception
  {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    try
    {
      oos.writeObject(info);
      oos.close();
    }
    finally
    {
      oos.close();
    }
    return baos.toByteArray();
  }

  /**
   * Convert this object into a {@link DriverConnectionInfo}.
   * @param ssl whether ssl is enabled or not.
   * @param recovery whether discovery is enabled or not.
   * @return a {@link DriverConnectionInfo} instance.
   */
  public DriverConnectionInfo toDriverConnectionInfo(final boolean ssl, final boolean recovery) {
    int port = ssl ? sslServerPorts[0] : serverPorts[0];
    boolean recoveryEnabled = recovery && (recoveryPort >= 0);
    return new JPPFDriverConnectionInfo(ssl, host, port, recoveryEnabled ? recoveryPort: -1);
  }
}
