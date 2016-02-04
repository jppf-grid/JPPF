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

package org.jppf.node.connection;

import org.jppf.comm.discovery.JPPFConnectionInformation;

/**
 * A default implementation for the {@link DriverConnectionInfo} interface.
 * @author Laurent Cohen
 * @since 4.1
 */
public class JPPFDriverConnectionInfo implements DriverConnectionInfo {
  /**
   * Whether SSL/TLS should be used.
   */
  protected boolean secure;
  /**
   * The driver host name or IP address.
   */
  protected String host;
  /**
   * The driver port to connect to.
   */
  protected int port;
  /**
   * The driver recovery port to connect to.
   */
  protected int recoveryPort;

  /**
   * Default constructor which initializes the parameters to default values.
   */
  public JPPFDriverConnectionInfo() {
    secure = false;
    host = "localhost";
    port = 11111;
    recoveryPort = -1;
  }

  /**
   * Initialize the parameters with the specified values.
   * @param secure whether SSL/TLS should be used.
   * @param host the driver host name or IP address.
   * @param port the driver port to connect to.
   * @param recoveryPort the driver recovery port to connect to.
   */
  public JPPFDriverConnectionInfo(final boolean secure, final String host, final int port, final int recoveryPort) {
    this.secure = secure;
    this.host = host;
    this.port = port;
    this.recoveryPort = recoveryPort;
  }

  @Override
  public boolean isSecure() {
    return secure;
  }

  /**
   * Specify whether SSL/TLS should be used.
   * @param secure {@code true} for a secure connection, {@code false} otherwise.
   */
  public void setSecure(final boolean secure)
  {
    this.secure = secure;
  }

  @Override
  public String getHost() {
    return host;
  }

  /**
   * Set the driver host name or IP address.
   * @param host the host as a string.
   */
  public void setHost(final String host) {
    this.host = host;
  }

  @Override
  public int getPort() {
    return port;
  }

  /**
   * Set the driver port to connect to.
   * @param port the port as an int value.
   */
  public void setPort(final int port) {
    this.port = port;
  }

  @Override
  public int getRecoveryPort() {
    return recoveryPort;
  }

  /**
   * Set the driver recovery port to connect to.
   * @param recoveryPort the recovery port as an int value.
   */
  public void setRecoveryPort(final int recoveryPort) {
    this.recoveryPort = recoveryPort;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((host == null) ? 0 : host.hashCode());
    result = prime * result + port;
    result = prime * result + recoveryPort;
    result = prime * result + (secure ? 1231 : 1237);
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    JPPFDriverConnectionInfo other = (JPPFDriverConnectionInfo) obj;
    if (host == null) {
      if (other.host != null) return false;
    }
    else if (!host.equals(other.host)) return false;
    if (port != other.port) return false;
    if (recoveryPort != other.recoveryPort) return false;
    if (secure != other.secure) return false;
    return true;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("secure=").append(secure);
    sb.append(", host=").append(host);
    sb.append(", port=").append(port);
    sb.append(", recoveryPort=").append(recoveryPort);
    sb.append(']');
    return sb.toString();
  }

  /**
   * Convert the specified {@link JPPFConnectionInformation} object into a {@link DriverConnectionInfo}.
   * @param ci the {@link JPPFConnectionInformation} object to convert from.
   * @param ssl whether ssl is enabled or not.
   * @param recovery whether discovery is enabled or not.
   * @return a {@link DriverConnectionInfo} instance.
   */
  public static DriverConnectionInfo fromJPPFConnectionInformation(final JPPFConnectionInformation ci, final boolean ssl, final boolean recovery) {
    int port = ssl ? ci.sslServerPorts[0] : ci.serverPorts[0];
    boolean recoveryEnabled = recovery && (ci.recoveryPort >= 0);
    return new JPPFDriverConnectionInfo(ssl, ci.host, port, recoveryEnabled ? ci.recoveryPort: -1);
  }
}
