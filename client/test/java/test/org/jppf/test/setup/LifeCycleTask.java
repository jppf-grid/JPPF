/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package test.org.jppf.test.setup;

import org.jppf.server.protocol.JPPFTask;
import org.slf4j.*;

/**
 * A simple JPPF task for unit-testing the task life cycle.
 * @author Laurent Cohen
 */
public class LifeCycleTask extends JPPFTask
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(LifeCycleTask.class);
  /**
   * The duration of this task;
   */
  private long duration = 0L;
  /**
   * used to store the task's execution start time.
   */
  private transient long start = 0L;
  /**
   * Measures the time elapsed between the task execution start and either completion
   * or a call to one of the life cycle methods.
   */
  private transient long elapsed = 0L;
  /**
   * Determines whether this task was cancelled.
   */
  private boolean cancelled = false;
  /**
   * Determines whether this task was restarted.
   */
  private boolean restarted = false;
  /**
   * Determines whether this task timed out.
   */
  private boolean timedout = false;

  /**
   * Initialize this task.
   */
  public LifeCycleTask()
  {
  }

  /**
   * Initialize this task.
   * @param duration specifies the duration of this task.
   */
  public LifeCycleTask(final long duration)
  {
    this.duration = duration;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void run()
  {
    start = System.currentTimeMillis();
    try
    {
      if (duration > 0) Thread.sleep(duration);
      setResult(BaseSetup.EXECUTION_SUCCESSFUL_MESSAGE);
      elapsed = System.currentTimeMillis() - start;
      displayElapsed("successful");
    }
    catch(Exception e)
    {
      setException(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onCancel()
  {
    elapsed = System.currentTimeMillis() - start;
    cancelled = true;
    displayElapsed("cancelled");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onRestart()
  {
    elapsed = System.currentTimeMillis() - start;
    restarted = true;
    displayElapsed("restarted");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onTimeout()
  {
    elapsed = System.currentTimeMillis() - start;
    timedout = true;
    displayElapsed("timed out");
  }

  /**
   * Log or display a message showing the execution status and elapsed of this task.
   * @param message a short message describing the life cycle status.
   */
  private void displayElapsed(final String message)
  {
    log.info("task id='" + getId() + "' " + message + ", duration=" + duration + ", result=" + getResult() + ", elapsed=" + elapsed);
  }

  /**
   * Determine whether this task was cancelled.
   * @return true if the task was cancelled, false otherwise.
   */
  public boolean isCancelled()
  {
    return cancelled;
  }

  /**
   * Determines whether this task was restarted.
   * @return true if the task was restarted, false otherwise.
   */
  public boolean isRestarted()
  {
    return restarted;
  }

  /**
   * Determine whether this task timed out.
   * @return true if the task timed out, false otherwise.
   */
  public boolean isTimedout()
  {
    return timedout;
  }
}
