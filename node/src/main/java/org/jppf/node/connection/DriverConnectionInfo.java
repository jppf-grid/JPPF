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

package org.jppf.node.connection;

/**
 * This interface provides the required information for connecting to a remote JPPF driver.
 * @author Laurent Cohen
 * @since 4.1
 */
public interface DriverConnectionInfo {
  /**
   * Determine whether secure (with SSL/TLS) connections should be established.
   * @return {@code true} for secure connections, {@code false} otherwise.
   */
  boolean isSecure();

  /**
   * Get the driver host name or IP address.
   * @return the host as a string.
   */
  String getHost();

  /**
   * Get the driver port to connect to.
   * @return the driver port as an int value.
   */
  int getPort();

  /**
   * Get the recovery port for the heartbeat mechanism.
   * @return the recovery port a an int; a negative value indicates that recovery is disabled for the node.
   * @deprecated as of JPPF 6.0, the recovery mechanism uses the same port number as the main server port.
   * This method will return {@code -1} if recovery is disabled, or the value of {@link #getPort()} if it is enabled.
   */
  int getRecoveryPort();

  /**
   * Determine whether the recovery mechanism (exchange of heartbeat messages) is enabled.
   * @return {@code true} if recovery is enabled, {@code false} otherwise.
   * @deprecated use {@link #isHeartbeatEnabled()} instead.
   */
  boolean isRecoveryEnabled();

  /**
   * Determine whether the recovery mechanism (exchange of heartbeat messages) is enabled.
   * @return {@code true} if the heartbeat mechanism is enabled, {@code false} otherwise.
   */
  default boolean isHeartbeatEnabled() {
    return isRecoveryEnabled();
  }
}
