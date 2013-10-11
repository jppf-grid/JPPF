/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

package org.jppf.server.protocol;

import org.jppf.io.DataLocation;
import org.slf4j.*;

/**
 *
 * @author Martin JANDA
 * @exclude
 */
public class ServerTask {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ServerTask.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Client bundle that owns this task.
   */
  private final ServerTaskBundleClient bundle;
  /**
   * The position of this task within received bundle.
   */
  private final int position;
  /**
   * The position of this task within the job submitted by the client.
   */
  private final int jobPosition;
  /**
   * The shared data provider for this task.
   */
  private final DataLocation   dataLocation;
  /**
   * The execution result.
   */
  private DataLocation         result = null;
  /**
   * The exception thrown during execution.
   */
  private Throwable            exception;
  /**
   * The state of this task.
   */
  private TaskState state = TaskState.PENDING;
  /**
   * Number of times a dispatch of this task has expired.
   */
  private int expirationCount = 0;

  /**
   *
   * @param bundle client bundle that own this task.
   * @param position identification of this task within bundle.
   * @param dataLocation shared data provider for this task.
   * @param jobPosition the position of this task within the job submitted by the client.
   */
  public ServerTask(final ServerTaskBundleClient bundle, final int position, final DataLocation dataLocation, final int jobPosition) {
    if (bundle == null) throw new IllegalArgumentException("bundle is null");
    if (dataLocation == null) throw new IllegalArgumentException("dataLocation is null");

    this.position = position;
    this.bundle = bundle;
    this.dataLocation = dataLocation;
    this.jobPosition = jobPosition;
  }

  /**
   * Returns the position of this task in the bundle in which it was received.
   * @return the position of this task as an <code>int</code>.
   */
  public int getPosition() {
    return position;
  }

  /**
   * Get the client bundle that owns this task.
   * @return <code>ServerTaskBundleClient</code> instance.
   */
  public ServerTaskBundleClient getBundle() {
    return bundle;
  }

  /**
   * Get the provider of shared data for this task.
   * @return a <code>DataProvider</code> instance.
   */
  public DataLocation getDataLocation() {
    return dataLocation;
  }

  /**
   * Get the state of this task.
   * @return a {@link TaskState} enumerated value.
   */
  public TaskState getState() {
    return state;
  }

  /**
   * Set the state of this task.
   * @param state a {@link TaskState} enumerated value.
   */
  public void setState(final TaskState state) {
    this.state = state;
  }

  /**
   * Get the result of the task execution.
   * @return the result as <code>DataLocation</code>.
   */
  public DataLocation getResult() {
    return (result == null) ? dataLocation : result;
  }

  /**
   * Get the exception raised during this task execution.
   * @return a <code>Throwable</code> instance, or null if no exception was raised.
   */
  public Throwable getException() {
    return exception;
  }

  /**
   * Mark this task as cancelled.
   */
  public void cancel() {
    if (debugEnabled) log.debug("cancelling {}", this);
    result = dataLocation;
    state = TaskState.CANCELLED;
  }

  /**
   * Mark this task as cancelled after dispatch expiration.
   */
  public void expirationCancel() {
    if (debugEnabled) log.debug("cancelling {}", this);
    result = dataLocation;
    state = TaskState.TIMEOUT_CANCELLED;
  }

  /**
   * Mark this task as to be resubmitted followxing expiration of a node dispatch.
   */
  public void expirationResubmit() {
    if (debugEnabled) log.debug("expiring {}", this);
    result = null;
    state = TaskState.TIMEOUT_RESUBMIT;
  }

  /**
   * Called to notify that the task received result.
   * @param result the result.
   */
  public void resultReceived(final DataLocation result) {
    if (result == null) throw new IllegalArgumentException("result is null");
    this.result = result;
    this.exception = null;
    this.state = (result == dataLocation) ? TaskState.CANCELLED : TaskState.RESULT;
  }

  /**
   * Called to notify that the task received exception during execution.
   * @param exception the exception.
   */
  public void resultReceived(final Throwable exception) {
    if (exception == null) throw new IllegalArgumentException("exception is null");
    this.result = null;
    this.exception = exception;
    this.state = TaskState.EXCEPTION;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("ServerTask[");
    sb.append("state=").append(getState());
    sb.append(", position=").append(position);
    sb.append(", jobPosition=").append(jobPosition);
    sb.append(", expirationCount=").append(expirationCount);
    sb.append(", dataLocation=").append(dataLocation);
    sb.append(", result=").append(result);
    sb.append(", exception=").append(exception);
    sb.append('}');
    return sb.toString();
  }

  /**
   * Get the position of this task within the job submitted by the client.
   * @return the position as an int.
   */
  public int getJobPosition()
  {
    return jobPosition;
  }

  /**
   * Get the number of times a dispatch of this task has expired.
   * @return the number of expirations as an int.
   */
  public int getExpirationCount()
  {
    return expirationCount;
  }

  /**
   * Increment and get the number of times a dispatch of this task has expired.
   * @return the new number of expirations as an int.
   */
  public int incExpirationCount()
  {
    return ++expirationCount;
  }
}
