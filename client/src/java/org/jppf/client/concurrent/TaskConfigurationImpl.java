/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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
 */
class TaskConfigurationImpl implements TaskConfiguration
{
  /**
   * A delegate for the <code>onCancel()</code> method.
   */
  private JPPFTaskCallback cancelCallback = null;
  /**
   * A delegate for the <code>onTimeout()</code> method.
   */
  private JPPFTaskCallback timeoutCallback = null;
  /**
   * The task timeout schedule configuration.
   */
  private JPPFSchedule timeoutSchedule = null;

  /**
   * Default constructor.
   */
  TaskConfigurationImpl()
  {
  }

  @Override
  public JPPFTaskCallback getOnCancelCallback()
  {
    return cancelCallback;
  }

  @Override
  public void setOnCancelCallback(final JPPFTaskCallback cancelCallback)
  {
    this.cancelCallback = cancelCallback;
  }

  @Override
  public JPPFTaskCallback getOnTimeoutCallback()
  {
    return timeoutCallback;
  }

  @Override
  public void setOnTimeoutCallback(final JPPFTaskCallback timeoutCallback)
  {
    this.timeoutCallback = timeoutCallback;
  }

  @Override
  public JPPFSchedule getTimeoutSchedule()
  {
    return timeoutSchedule;
  }

  @Override
  public void setTimeoutSchedule(final JPPFSchedule timeoutSchedule)
  {
    this.timeoutSchedule = timeoutSchedule;
  }
}
