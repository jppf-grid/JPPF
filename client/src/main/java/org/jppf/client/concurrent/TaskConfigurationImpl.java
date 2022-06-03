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

package org.jppf.client.concurrent;

import org.jppf.client.taskwrapper.JPPFTaskCallback;
import org.jppf.scheduling.JPPFSchedule;

/**
 * Configuration for tasks submitted by a <code>JPPFExecutorService</code> which do not extend <code>JPPFTask</code>.
 * @author Laurent Cohen
 * @exclude
 */
class TaskConfigurationImpl implements TaskConfiguration {
  /**
   * A delegate for the <code>onCancel()</code> method.
   */
  private JPPFTaskCallback<Object> cancelCallback;
  /**
   * A delegate for the <code>onTimeout()</code> method.
   */
  private JPPFTaskCallback<Object> timeoutCallback;
  /**
   * The task timeout schedule configuration.
   */
  private JPPFSchedule timeoutSchedule;

  /**
   * Default constructor.
   */
  TaskConfigurationImpl() {
  }

  @Override
  public JPPFTaskCallback<Object> getOnCancelCallback() {
    return cancelCallback;
  }

  @Override
  public TaskConfiguration setOnCancelCallback(final JPPFTaskCallback<Object> cancelCallback) {
    this.cancelCallback = cancelCallback;
    return this;
  }

  @Override
  public JPPFTaskCallback<Object> getOnTimeoutCallback() {
    return timeoutCallback;
  }

  @Override
  public TaskConfiguration setOnTimeoutCallback(final JPPFTaskCallback<Object> timeoutCallback) {
    this.timeoutCallback = timeoutCallback;
    return this;
  }

  @Override
  public JPPFSchedule getTimeoutSchedule() {
    return timeoutSchedule;
  }

  @Override
  public TaskConfiguration setTimeoutSchedule(final JPPFSchedule timeoutSchedule) {
    this.timeoutSchedule = timeoutSchedule;
    return this;
  }
}
