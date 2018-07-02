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
  protected final boolean secure;
  /**
   * The driver host name or IP address.
   */
  protected final String host;
  /**
   * The driver port to connect to.
   */
  protected final int port;
  /**
   * Whether recovery is enabled.
   */
  protected final boolean recoveryEnabled;

  /**
   * Initialize the parameters with the specified values.
   * @param secure whether SSL/TLS should be used.
   * @param host the driver host name or IP address.
   * @param port the driver port to connect to.
   * @param recoveryPort the driver recovery port to connect to.
   * @deprecated as of JPPF 6.0, the recovery mechanism uses the same port number as the main server port.
   * This constructor assumes recovery is enabled if {@code recoveryPort > 0}, and disabled otherwise.
   */
  public JPPFDriverConnectionInfo(final boolean secure, final String host, final int port, final int recoveryPort) {
    this(secure, host, port, recoveryPort > 0);
  }

  /**
   * Initialize the parameters with the specified values.
   * @param secure whether SSL/TLS should be used.
   * @param host the driver host name or IP address.
   * @param port the driver port to connect to.
   * @param recoveryEnabled whether recovery is enabled..
   */
  public JPPFDriverConnectionInfo(final boolean secure, final String host, final int port, final boolean recoveryEnabled) {
    this.secure = secure;
    this.host = host;
    this.port = port;
    this.recoveryEnabled = recoveryEnabled;
  }

  @Override
  public boolean isSecure() {
    return secure;
  }

  @Override
  public String getHost() {
    return host;
  }

  @Override
  public int getPort() {
    return port;
  }

  /**
   * @deprecated as of JPPF 6.0, the recovery mechanism uses the same port number as the main server port.
   * This method will return {@code -1} if recovery is disabled, or the value of {@link #getPort()} if it is enabled.
   */
  @Override
  public int getRecoveryPort() {
    return recoveryEnabled ? port : -1;
  }

  @Override
  public boolean isRecoveryEnabled() {
    return recoveryEnabled;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((host == null) ? 0 : host.hashCode());
    result = prime * result + port;
    result = prime * result + (recoveryEnabled ? 1 : 0);
    result = prime * result + (secure ? 1231 : 1237);
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    final JPPFDriverConnectionInfo other = (JPPFDriverConnectionInfo) obj;
    if (host == null) {
      if (other.host != null) return false;
    } else if (!host.equals(other.host)) return false;
    if (port != other.port) return false;
    if (recoveryEnabled != other.recoveryEnabled) return false;
    if (secure != other.secure) return false;
    return true;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("secure=").append(secure);
    sb.append(", host=").append(host);
    sb.append(", port=").append(port);
    sb.append(", recoveryEnabled=").append(recoveryEnabled);
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
    final int port = ssl ? ci.sslServerPorts[0] : ci.serverPorts[0];
    return new JPPFDriverConnectionInfo(ssl, ci.host, port, ci.recoveryEnabled);
  }
}
