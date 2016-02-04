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

package org.jppf.client.concurrent;

import org.jppf.client.taskwrapper.JPPFTaskCallback;
import org.jppf.scheduling.JPPFSchedule;

/**
 * This interface defines the properties that can be provided to a task
 * submitted by a {@link JPPFExecutorService} and that is not a {@link org.jppf.node.protocol.Task Task} instance.
 * These properties include:
 * <ul>
 * <li>the task expiration schedule</li>
 * <li>a callback that will replace the task's {@link org.jppf.node.protocol.Task#onTimeout() onTimeout()} method</li>
 * <li>a callback that will replace the task's {@link org.jppf.node.protocol.Task#onCancel() onCancel()} method</li>
 * </ul>
 * @author Laurent Cohen
 */
public interface TaskConfiguration
{
  /**
   * Get the delegate for the <code>onCancel()</code> method.
   * @return a {@link JPPFTaskCallback} instance.
   */
  JPPFTaskCallback getOnCancelCallback();

  /**
   * Set the delegate for the <code>onCancel()</code> method.
   * @param cancelCallback a {@link JPPFTaskCallback} instance.
   */
  void setOnCancelCallback(final JPPFTaskCallback cancelCallback);

  /**
   * Get the delegate for the <code>onTimeout()</code> method.
   * @return a {@link JPPFTaskCallback} instance.
   */
  JPPFTaskCallback getOnTimeoutCallback();

  /**
   * Set the delegate for the <code>onTimeout()</code> method.
   * @param timeoutCallback a {@link JPPFTaskCallback} instance.
   */
  void setOnTimeoutCallback(final JPPFTaskCallback timeoutCallback);

  /**
   * Get the timeout set on the task.
   * @return a {@link JPPFSchedule} instance, or null if no timeout was set.
   */
  JPPFSchedule getTimeoutSchedule();

  /**
   * Set the timeout set on the task.
   * @param timeoutSchedule a {@link JPPFSchedule} instance.
   */
  void setTimeoutSchedule(final JPPFSchedule timeoutSchedule);
}
