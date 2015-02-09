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
   * Determines whether trace-level logging is enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * Client bundle that owns this task.
   */
  private final ServerTaskBundleClient bundle;
  /**
   * The position of this task within the job submitted by the client.
   */
  private final int jobPosition;
  /**
   * The initial serialized task.
   */
  private final DataLocation initialTask;
  /**
   * The serialized execution result.
   */
  private DataLocation result = null;
  /**
   * The exception thrown during execution.
   */
  private Throwable exception;
  /**
   * The state of this task.
   */
  private TaskState state = TaskState.PENDING;
  /**
   * Number of times a dispatch of this task has expired.
   */
  private int expirationCount = 0;
  /**
   * Maximum number of times a task can be resubmitted.
   */
  private final int maxResubmits;
  /**
   * Number of times a task resubmitted itself.
   */
  private int resubmitCount = 0;

  /**
   *
   * @param bundle client bundle that owns this task.
   * @param initialTask the initial serialized task.
   * @param jobPosition the maximum number of times a task can be resubmitted.
   * @param maxResubmits the position of this task within the job submitted by the client.
   */
  public ServerTask(final ServerTaskBundleClient bundle, final DataLocation initialTask, final int jobPosition, final int maxResubmits) {
    if (bundle == null) throw new IllegalArgumentException("bundle is null");
    if (initialTask == null) throw new IllegalArgumentException("dataLocation is null");
    this.bundle = bundle;
    this.initialTask = initialTask;
    this.jobPosition = jobPosition;
    this.maxResubmits = maxResubmits;
  }

  /**
   * Get the client bundle that owns this task.
   * @return <code>ServerTaskBundleClient</code> instance.
   */
  public ServerTaskBundleClient getBundle() {
    return bundle;
  }

  /**
   * Get the initial serialized task.
   * @return a <code>DataLocation</code> instance.
   */
  public DataLocation getInitialTask() {
    return initialTask;
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
    return (result == null) ? initialTask : result;
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
    if (traceEnabled) log.trace("cancelling {}", this);
    result = initialTask;
    state = TaskState.CANCELLED;
  }

  /**
   * Mark this task as to be resubmitted followxing expiration of a node dispatch.
   */
  public void resubmit() {
    if (traceEnabled) log.trace("resubmitting {}", this);
    result = null;
    state = TaskState.RESUBMIT;
  }

  /**
   * Called to notify that the task received result.
   * @param result the result.
   */
  public void resultReceived(final DataLocation result) {
    if (result == null) throw new IllegalArgumentException("result is null");
    this.result = result;
    this.exception = null;
    this.state = (result == initialTask) ? TaskState.CANCELLED : TaskState.RESULT;
  }

  /**
   * Called to notify that the task broadcast was completed.
   */
  public void broadcastResultReceived() {
    this.result = this.initialTask;
    this.state = TaskState.RESULT;
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
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ServerTask[");
    sb.append("state=").append(getState());
    sb.append(", jobPosition=").append(jobPosition);
    sb.append(", expirationCount=").append(expirationCount);
    sb.append(", resubmitCount=").append(resubmitCount);
    sb.append(", maxResubmits=").append(maxResubmits);
    //sb.append(", dataLocation=").append(initialTask);
    //sb.append(", result=").append(result);
    sb.append(", exception=").append(exception);
    sb.append('}');
    return sb.toString();
  }

  /**
   * Get the position of this task within the job submitted by the client.
   * @return the position as an int.
   */
  public int getJobPosition() {
    return jobPosition;
  }

  /**
   * Increment and get the number of times a dispatch of this task has expired.
   * @return the new number of expirations as an int.
   */
  public int incExpirationCount() {
    return ++expirationCount;
  }

  /**
   * Determine whether this task has completed.
   * This encompasses the cases where it received a result, was cancelled or terminated with an exception.
   * @return <code>true</code> if this task is done, <code>false</code> otherwise.
   */
  public boolean isDone() {
    return state.ordinal() >= TaskState.EXCEPTION.ordinal();
  }

  /**
   * Get the number of times a task resubmitted itself.
   * @return the number of resubmits as an int.
   */
  public int getTaskResubmitCount() {
    return resubmitCount;
  }

  /**
   * Increment and get the number of times a task resubmitted itself.
   * @return the new number of resubmits as an int.
   */
  public int incResubmitCount() {
    return ++resubmitCount;
  }

  /**
   * Get the maximum number of times a task can be resubmitted.
   * @return the maximum number of reesubmits as an int.
   */
  public int getMaxResubmits() {
    return maxResubmits;
  }
}
