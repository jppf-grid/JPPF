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

package org.jppf.client;

import java.util.*;

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
   * The connection failed to (re)connect to the driver and no further attempt will be made.
   */
  FAILED,
  /**
   * The connection was closed by the application.
   */
  CLOSED;

  /**
   * The list of working statuses.
   */
  private static final List<JPPFClientConnectionStatus> WORKING_STATUSES = Collections.unmodifiableList(Arrays.asList(ACTIVE, EXECUTING));
  /**
   * The list of working statuses.
   */
  private static final List<JPPFClientConnectionStatus> TERMINATED_STATUSES = Collections.unmodifiableList(Arrays.asList(CLOSED, FAILED));

  /**
   * Determine whether this status is one of those specified as input.
   * @param statuses the statuses to check against.
   * @return {@code true} if this status is one of those specified as input, {@code false} otherwise.
   */
  public boolean isOneOf(final List<JPPFClientConnectionStatus> statuses) {
    if (statuses == null) return false;
    for (JPPFClientConnectionStatus status: statuses) {
      if (this == status) return true;
    }
    return false;
  }

  /**
   * Determine whether this status a working status, that is if it is either {@link #ACTIVE} or {@link #EXECUTING}.
   * @return {@code true} if this status is a working status, {@code false} otherwise.
   */
  public boolean isWorkingStatus() {
    return isOneOf(WORKING_STATUSES);
  }

  /**
   * Determine whether this status a working status, that is if it is either {@link #FAILED} or {@link #CLOSED}.
   * @return {@code true} if this status is a working status, {@code false} otherwise.
   */
  public boolean isTerminatedStatus() {
    return isOneOf(TERMINATED_STATUSES);
  }

  /**
   * Get the statuses indicating that a connection is in a working state.
   * @return an array of {@link JPPFClientConnectionStatus} enum values.
   */
  public static JPPFClientConnectionStatus[] workingStatuses() {
    return WORKING_STATUSES.toArray(new JPPFClientConnectionStatus[WORKING_STATUSES.size()]);
  }

  /**
   * Get the statuses indicating that a connection is in a terminated state.
   * @return an array of {@link JPPFClientConnectionStatus} enum values.
   */
  public static JPPFClientConnectionStatus[] terminatedStatuses() {
    return TERMINATED_STATUSES.toArray(new JPPFClientConnectionStatus[TERMINATED_STATUSES.size()]);
  }
}
