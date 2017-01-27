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

package org.jppf.discovery;

import static org.jppf.utils.configuration.JPPFProperties.*;

/**
 * Instances of this class represent the connection information and configuration of a JPPF connection pool on the client side.
 * @author Laurent Cohen
 * @since 5.2.1
 */
public class ClientConnectionPoolInfo extends DriverConnectionInfo {
  /**
   * The connection priority.
   */
  private final int priority;
  /**
   * The connection pool size.
   */
  private final int poolSize;
  /**
   * The associated JMX connection pool size.
   */
  private final int jmxPoolSize;

  /**
   * Initialize a pool of plain connections with default name("driver"), host ("localhost"), port (11111), priority (0) and pool sizes (1).
   */
  public ClientConnectionPoolInfo() {
    this("driver", false, SERVER_HOST.getDefaultValue(), SERVER_PORT.getDefaultValue());
  }

  /**
   * Initialize a pool of plain connections with default priority (0) and pool sizes (1).
   * @param name the name given to this connection pool, used as numbered prefix for individual connection names.
   * @param host the driver host name or IP address.
   * @param port the driver port to connect to.
   */
  public ClientConnectionPoolInfo(final String name, final String host, final int port) {
    this(name, false, host, port);
  }

  /**
   * Initialize a pool of connections with default priority (0) and pool sizes (1).
   * @param name the name given to this connection pool, used as numbered prefix for individual connection names.
   * @param secure whether SSL/TLS should be used.
   * @param host the driver host name or IP address.
   * @param port the driver port to connect to.
   */
  public ClientConnectionPoolInfo(final String name, final boolean secure, final String host, final int port) {
    this(name, secure, host, port, DISCOVERY_PRIORITY.getDefaultValue(), POOL_SIZE.getDefaultValue(), JMX_POOL_SIZE.getDefaultValue());
  }

  /**
   * Initialize a pool of connections with the specified parameters.
   * @param name the name given to this connection pool, used as numbered prefix for individual connection names.
   * @param secure whether SSL/TLS should be used.
   * @param host the driver host name or IP address.
   * @param port the driver port to connect to.
   * @param priority the connection priority.
   * @param poolSize the connection pool size.
   * @param jmxPoolSize the associated JMX connection pool size.
   */
  public ClientConnectionPoolInfo(final String name, final boolean secure, final String host, final int port, final int priority, final int poolSize, final int jmxPoolSize) {
    super(name, secure, host, port);
    this.priority = priority;
    this.poolSize = poolSize;
    this.jmxPoolSize = jmxPoolSize;
  }

  /**
   * Get the connection priority.
   * @return the connection priority as an int value.
   */
  public int getPriority() {
    return priority;
  }

  /**
   * Get the connection pool size.
   * @return the connection pool size as an int value.
   */
  public int getPoolSize() {
    return poolSize;
  }

  /**
   * Get the associated JMX connection pool size.
   * @return the JMX pool size as an int value.
   */
  public int getJmxPoolSize() {
    return jmxPoolSize;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = prime + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((host == null) ? 0 : host.hashCode());
    result = prime * result + port;
    result = prime * result + (secure ? 1231 : 1237);
    result = prime * result + priority;
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    ClientConnectionPoolInfo other = (ClientConnectionPoolInfo) obj;
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
    if (priority != other.priority) return false;
    return true;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("name=").append(name);
    sb.append(", secure=").append(secure);
    sb.append(", host=").append(host);
    sb.append(", port=").append(port);
    sb.append(", priority=").append(priority);
    sb.append(", poolSize=").append(poolSize);
    sb.append(", jmxPoolSize=").append(jmxPoolSize);
    sb.append(']');
    return sb.toString();
  }
}
