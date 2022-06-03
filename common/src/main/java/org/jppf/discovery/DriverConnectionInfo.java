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

package org.jppf.discovery;

import static org.jppf.utils.configuration.JPPFProperties.*;

/**
 * Superclass for connection info in custom driver discovery mechanisms.
 * @author Laurent Cohen
 * @since 5.2.1
 */
public class DriverConnectionInfo {
  /**
   * The name given to this connection, used as umbered prefix for individual connection names.
   */
  String name;
  /**
   * Whether SSL/TLS should be used.
   */
  boolean secure;
  /**
   * The driver host name or IP address.
   */
  String host;
  /**
   * The driver port to connect to.
   */
  int port;
  /**
   * The connection pool size.
   */
  int poolSize;
  /**
   * Whether the heartbeat mechanism is enabled for the connection pool.
   */
  boolean heartbeatEnabled;

  /**
   * Initialize a plain connection with default name("driver"), pool size (1), host ("localhost") and port (11111).
   */
  public DriverConnectionInfo() {
    this("driver", false, SERVER_HOST.getDefaultValue(), SERVER_PORT.getDefaultValue(), 1, false);
  }

  /**
   * Initialize a plain connection with a default pool size of 1.
   * @param name the name given to this connection.
   * @param host the driver host name or IP address.
   * @param port the driver port to connect to.
   */
  public DriverConnectionInfo(final String name, final String host, final int port) {
    this(name, false, host, port, 1, false);
  }

  /**
   * Initialize a connection with a default pool size of 1.
   * @param name the name given to this connection.
   * @param secure whether SSL/TLS should be used.
   * @param host the driver host name or IP address.
   * @param port the driver port to connect to.
   */
  public DriverConnectionInfo(final String name, final boolean secure, final String host, final int port) {
    this(name, secure, host, port, 1, false);
  }

  /**
   * Initialize a connection.
   * @param name the name given to this connection.
   * @param secure whether SSL/TLS should be used.
   * @param host the driver host name or IP address.
   * @param port the driver port to connect to.
   * @param poolSize the number of driver connections in the pool.
   */
  public DriverConnectionInfo(final String name, final boolean secure, final String host, final int port, final int poolSize) {
    this(name, secure, host, port, poolSize, false);
  }

  /**
   * Initialize a connection.
   * @param name the name given to this connection.
   * @param secure whether SSL/TLS should be used.
   * @param host the driver host name or IP address.
   * @param port the driver port to connect to.
   * @param poolSize the number of driver connections in the pool.
   * @param heartbeatEnabled whether the heartbeat mechanism is enabled for the connection pool.
   */
  public DriverConnectionInfo(final String name, final boolean secure, final String host, final int port, final int poolSize, final boolean heartbeatEnabled) {
    this.name = name;
    this.secure = secure;
    this.host = host;
    this.port = port;
    this.poolSize = poolSize >= 1 ? poolSize : 1;
    this.heartbeatEnabled = heartbeatEnabled;
  }

  /**
   * Get the name given to this connection.
   * @return the connection name as a string.
   */
  public String getName() {
    return name;
  }

  /**
   * Set the name of this connection.
   * @param name the connection name as a string.
   */
  public void setName(final String name) {
    this.name = name;
  }

  /**
   * Determine whether secure (with SSL/TLS) connections should be established.
   * @return {@code true} for secure connections, {@code false} otherwise.
   */
  public boolean isSecure() {
    return secure;
  }

  /**
   * Set whether secure (with SSL/TLS) connections should be established.
   * @param secure {@code true} for secure connections, {@code false} otherwise.
   */
  public void setSecure(final boolean secure) {
    this.secure = secure;
  }

  /**
   * Get the driver host name or IP address.
   * @return the host as a string.
   */
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

  /**
   * Get the driver port to connect to.
   * @return the driver port as an int value.
   */
  public int getPort() {
    return port;
  }

  /**
   * Set the driver port to connect to.
   * @param port the driver port as an int value.
   */
  public void setPort(final int port) {
    this.port = port;
  }

  /**
   * Get the connection pool size.
   * @return the connection pool size as an int value.
   */
  public int getPoolSize() {
    return poolSize;
  }

  /**
   * Set the connection pool size.
   * @param poolSize the connection pool size as an int value.
   */
  public void setPoolSize(final int poolSize) {
    this.poolSize = poolSize;
  }

  /**
   * Determine whether the heartbeat mechanism is enabled for the connection pool.
   * @return {@code true} if heartbeat is enabled, {@code false} otherwise.
   */
  public boolean isHeartbeatEnabled() {
    return heartbeatEnabled;
  }

  /**
   * Set whether the heartbeat mechanism is enabled for the connection pool.
   * @param heartbeatEnabled {@code true} if heartbeat is enabled, {@code false} otherwise.
   */
  public void setHeartbeatEnabled(final boolean heartbeatEnabled) {
    this.heartbeatEnabled = heartbeatEnabled;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = prime + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((host == null) ? 0 : host.hashCode());
    result = prime * result + port;
    result = prime * result + (secure ? 1231 : 1237);
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    final DriverConnectionInfo other = (DriverConnectionInfo) obj;
    if (name == null) {
      if (other.name != null) return false;
    }
    else if (!name.equals(other.name)) return false;
    if (host == null) {
      if (other.host != null) return false;
    }
    else if (!host.equals(other.host)) return false;
    if (port != other.port) return false;
    if (secure != other.secure) return false;
    if (poolSize != other.poolSize) return false;
    if (heartbeatEnabled != other.heartbeatEnabled) return false;
    return true;
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName()).append('[')
      .append("name=").append(name)
      .append(", secure=").append(secure)
      .append(", host=").append(host)
      .append(", port=").append(port)
      .append(", poolSize=").append(poolSize)
      .append(", heartbeatEnabled=").append(heartbeatEnabled)
      .append(']').toString();
  }
}
