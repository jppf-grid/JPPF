/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

/**
 *
 * @author Martin JANDA
 * @exclude
 */
public class ServerTask {
  /**
   * State for task indicating whether result or exception was received.
   */
  public static enum State {
    /**
     * Task was just received - no result or exception.
     */
    PENDING,
    /**
     * Exception was received for task.
     */
    EXCEPTION,
    /**
     * Result was received for task.
     */
    RESULT,
    /**
     * Task is cancelled.
     */
    CANCELLED,
    /**
     * The taks was sent back to the client.
     */
    SENT
  }

  /**
   * Client bundle that owns this task.
   */
  private final ServerTaskBundleClient bundle;
  /**
   * The position of this task within received bundle.
   */
  private final int position;
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
  private State state = State.PENDING;

  /**
   *
   * @param bundle client bundle that own this task. 
   * @param position identification of this task within bundle.
   * @param dataLocation shared data provider for this task.
   */
  public ServerTask(final ServerTaskBundleClient bundle, final int position, final DataLocation dataLocation) {
    if (bundle == null) throw new IllegalArgumentException("bundle is null");
    if (dataLocation == null) throw new IllegalArgumentException("dataLocation is null");

    this.position = position;
    this.bundle = bundle;
    this.dataLocation = dataLocation;
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
   * @return a {@link State} enumerated value.
   */
  public State getState() {
    /*
    if (exception != null) return State.EXCEPTION;
    if (result == null) return State.PENDING;
    else if (result == dataLocation) return State.CANCELLED;
    else return State.RESULT;
    */
    return state;
  }

  /**
   * Get the result of the task execution.
   * @return the result as <code>DataLocation</code>.
   */
  public DataLocation getResult() {
    return  (result == null) ? dataLocation : result;
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
    result = dataLocation;
    state = State.CANCELLED;
  }
  
  /**
   * Called to notify that the task received result.
   * @param result the result.
   */
  public void resultReceived(final DataLocation result) {
    if (result == null) throw new IllegalArgumentException("result is null");
    this.result = result;
    this.exception = null;
    this.state = (result == dataLocation) ? State.CANCELLED : State.RESULT;
  }

  /**
   * Called to notify that the task received exception during execution.
   * @param exception the exception.
   */
  public void resultReceived(final Throwable exception) {
    if (exception == null) throw new IllegalArgumentException("exception is null");
    this.result = null;
    this.exception = exception;
    this.state = State.EXCEPTION;
  }

  /**
   * Set this task as sent back to the client.
   */
  public void taskSent() {
    state = State.SENT;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("ServerTask");
    sb.append("{position=").append(position);
    sb.append(", state=").append(getState());
    sb.append(", dataLocation=").append(dataLocation);
    sb.append(", result=").append(result);
    sb.append(", exception=").append(exception);
    sb.append('}');
    return sb.toString();
  }
}
