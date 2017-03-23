/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.job;

import java.io.*;

import org.jppf.io.*;
import org.jppf.node.protocol.Task;

/**
 * Instances of this class provide information about tasks that were dispatched to a node,
 * at the time they are returned from the node.
 * @author Laurent Cohen
 * @since 5.0
 */
public class ServerTaskInformation implements Serializable {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The position of this task within the job submitted by the client.
   */
  private final int jobPosition;
  /**
   * The throwable traised during execution.
   */
  private final Throwable throwable;
  /**
   * Number of times a dispatch of this task has expired.
   */
  private final int expirationCount;
  /**
   * Maximum number of times the task can be resubmitted.
   */
  private final int maxResubmits;
  /**
   * Number of times the task was resubmitted.
   */
  private final int resubmitCount;
  /**
   * Holds the result of the task.
   */
  private transient final DataLocation result;

  /**
   * Initialize this object.
   * @param jobPosition the position of this task within the job submitted by the client.
   * @param throwable the throwble rraised during execution.
   * @param expirationCount number of times a dispatch of this task has expired.
   * @param maxResubmits maximum number of times the task can be resubmitted.
   * @param resubmitCount number of times the task was resubmitted.
   * @exclude
   */
  public ServerTaskInformation(final int jobPosition, final Throwable throwable, final int expirationCount, final int maxResubmits, final int resubmitCount) {
    this(jobPosition, throwable, expirationCount, maxResubmits, resubmitCount, null);
  }

  /**
   * Initialize this object.
   * @param jobPosition the position of this task within the job submitted by the client.
   * @param throwable the throwble rraised during execution.
   * @param expirationCount number of times a dispatch of this task has expired.
   * @param maxResubmits maximum number of times the task can be resubmitted.
   * @param resubmitCount number of times the task was resubmitted.
   * @param result holds the result of the task.
   * @exclude
   */
  public ServerTaskInformation(final int jobPosition, final Throwable throwable, final int expirationCount, final int maxResubmits, final int resubmitCount, final DataLocation result) {
    this.jobPosition = jobPosition;
    this.throwable = throwable;
    this.expirationCount = expirationCount;
    this.maxResubmits = maxResubmits;
    this.resubmitCount = resubmitCount;
    this.result = result;
  }

  /**
   * Get the position of this task within the job submitted by the client.
   * @return the position of the task in the job as an {@code int} index value.
   */
  public int getJobPosition() {
    return jobPosition;
  }

  /**
   * Get the throwable raised during execution of the task.
   * @return a {@code Throwable} instance, or {@code null} if no throwable was raised.
   */
  public Throwable getThrowable() {
    return throwable;
  }

  /**
   * Get the number of times a dispatch of the task has expired.
   * @return the number of dispatches as an {@code int} value.
   */
  public int getExpirationCount() {
    return expirationCount;
  }

  /**
   * Get the maximum number of times the task can be resubmitted.
   * @return the max number of resubmits as an {@code int} value.
   */
  public int getMaxResubmits() {
    return maxResubmits;
  }

  /**
   * Get the number of times the task was resubmitted.
   * @return the number of resubmits as an {@code int} value.
   */
  public int getResubmitCount() {
    return resubmitCount;
  }

  /**
   * Get an input stream of the task's result data, which can be desrialized as a {@link Task}.
   * @return an {@link InputStream}, or {@code null} if no result could be obtained.
   * @throws Exception if any error occurs getting the stream.
   */
  public InputStream getResultAsStream() throws Exception {
    if (result == null) return null;
    return result.getInputStream();
  }

  /**
   * Deserialize the result into a Task object.
   * @return a {@link Task}, or {@code null} if no result could be obtained.
   * @throws Exception if any error occurs deserializing the result.
   */
  public Task<?> getResultAsTask() throws Exception {
    if (result == null) return null;
    return (Task<?>) IOHelper.unwrappedData(result);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("jobPosition=").append(jobPosition);
    sb.append(", throwable=").append(throwable);
    sb.append(", expirationCount=").append(expirationCount);
    sb.append(", maxResubmits=").append(maxResubmits);
    sb.append(", resubmitCount=").append(resubmitCount);
    sb.append(']');
    return sb.toString();
  }
}
