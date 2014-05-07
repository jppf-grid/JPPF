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

package org.jppf.client;

/**
 * Status of the connection between a client and a driver.
 * @author Laurent Cohen
 */
public enum JPPFClientConnectionStatus {
  /**
   * The connection was just created.
   */
  CREATED,
  /**
   * The connection was just created.
   */
  NEW,
  /**
   * The connection was disconnected from the driver.
   */
  DISCONNECTED,
  /**
   * The connection is currently attempting to connect to the driver.
   */
  CONNECTING,
  /**
   * The connection is successfully connected to the driver.
   */
  ACTIVE,
  /**
   * The connection is currently executing a job.
   */
  EXECUTING,
  /**
   * The connection was closed or failed to connect to the driver and no further attempt will be made.
   */
  FAILED;

  /**
   * Determine whether this status is one of those specified as input.
   * @param statuses the statuses to check against.
   * @return {@code true} if this status is one of those specified as input, {@code false} otherwise.
   */
  public boolean isOneOf(final JPPFClientConnectionStatus...statuses) {
    if (statuses == null) return false;
    for (JPPFClientConnectionStatus status: statuses) {
      if (this == status) return true;
    }
    return false;
  }

  /**
   * Determine whether this status a working status.
   * @return {@code true} if this status is a working status, {@code false} otherwise.
   */
  public boolean isWorkingStatus() {
    return isOneOf(ACTIVE, EXECUTING);
  }
}
