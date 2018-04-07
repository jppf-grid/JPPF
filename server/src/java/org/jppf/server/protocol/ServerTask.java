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

package org.jppf.server.protocol;

import java.io.*;

import org.jppf.io.*;
import org.jppf.node.protocol.TaskState;
import org.slf4j.*;

/**
 *
 * @author Martin JANDA
 * @exclude
 */
public class ServerTask implements Serializable {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ServerTask.class);
  /**
   * Determines whether trace-level logging is enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * Client bundle that owns this task.
   */
  private transient ServerTaskBundleClient bundle;
  /**
   * The position of this task within the job submitted by the client.
   */
  private int jobPosition;
  /**
   * The initial serialized task.
   */
  private DataLocation initialTask;
  /**
   * The serialized execution result.
   */
  private transient DataLocation result;
  /**
   * The exception thrown during execution.
   */
  private Throwable throwable;
  /**
   * The state of this task.
   */
  private TaskState state = TaskState.PENDING;
  /**
   * Number of times a dispatch of this task has expired.
   */
  private int expirationCount;
  /**
   * Maximum number of times a task can be resubmitted.
   */
  private int maxResubmits;
  /**
   * Number of times a task resubmitted itself.
   */
  private int resubmitCount;

  /**
   *
   * @param bundle client bundle that owns this task.
   * @param initialTask the initial serialized task.
   * @param jobPosition the maximum number of times a task can be resubmitted.
   * @param maxResubmits the position of this task within the job submitted by the client.
   */
  public ServerTask(final ServerTaskBundleClient bundle, final DataLocation initialTask, final int jobPosition, final int maxResubmits) {
    if (bundle == null) throw new IllegalArgumentException("bundle is null");
    this.bundle = bundle;
    this.initialTask = initialTask == null ? new MultipleBuffersLocation(0) : initialTask;
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
   * Get the client bundle that owns this task.
   * @param bundle a {@link ServerTaskBundleClient} instance.
   */
  public void setBundle(final ServerTaskBundleClient bundle) {
    this.bundle = bundle;
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
   * @return the result as a {@link DataLocation}.
   */
  public DataLocation getResult() {
    return (result == null) ? initialTask : result;
  }

  /**
   * Set the result of the task execution.
   * @param result the result as a {@link DataLocation}.
   */
  public void setResult(final DataLocation result) {
    this.result = result;
  }

  /**
   * Get the eventual throwable raised during the processing of this task in the driver.
   * @return a {@code Throwable} instance, or null if no exception was raised.
   */
  public Throwable getThrowable() {
    return throwable;
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
    this.throwable = null;
    this.state = (result == initialTask) ? TaskState.CANCELLED : TaskState.RESULT;
    if (traceEnabled) log.trace("result received for {}", this);
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
    this.throwable = exception;
    this.state = TaskState.EXCEPTION;
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName()).append('[')
      .append("state=").append(getState())
      .append(", jobPosition=").append(jobPosition)
      .append(", expirationCount=").append(expirationCount)
      .append(", dataLocation=").append(initialTask)
      .append(", result=").append(result)
      .append(", exception=").append(throwable)
      .append(']').toString();
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
   * Get the number of times a dispatch of this task has expired.
   * @return the new number of expirations as an int.
   */
  public int getExpirationCount() {
    return expirationCount;
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

  /**
   * Save the state of the {@code ServerTask} instance to a stream (i.e.,serialize it).
   * @param out the output stream to which to write the task.
   * @throws IOException if any I/O error occurs.
   */
  private void writeObject(final ObjectOutputStream out) throws IOException {
    out.writeInt(jobPosition);
    out.writeInt(expirationCount);
    out.writeInt(maxResubmits);
    out.writeInt(resubmitCount);
    out.writeObject(state);
    out.writeObject(throwable);
    try {
      IOHelper.writeData(initialTask, new StreamOutputDestination(out));
    } catch(final Exception e) {
      throw (e instanceof IOException) ? (IOException) e : new IOException(e);
    }
  }

  /**
   * Reconstitute the {@code ServerTask} instance from a stream (i.e., deserialize it).
   * @param in the input stream from which to read the task.
   * @throws IOException if any I/O error occurs.
   * @throws ClassNotFoundException if the class of an object in the object graph can not be found.
   */
  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    jobPosition = in.readInt();
    expirationCount = in.readInt();
    maxResubmits = in.readInt();
    resubmitCount = in.readInt();
    state = (TaskState) in.readObject();
    throwable = (Throwable) in.readObject();
    try {
      initialTask = IOHelper.readData(new StreamInputSource(in));
    } catch(final IOException | ClassNotFoundException e) {
      throw  e;
    } catch(final Exception e) {
      throw new IOException(e);
    }
  }
}
