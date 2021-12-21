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

package test.jmx.canceljob;

import org.jppf.management.JPPFManagementInfo;
import org.jppf.node.protocol.AbstractTask;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * A simple JPPF task for unit-testing the task life cycle.
 * @author Laurent Cohen
 */
public class LifeCycleTask extends AbstractTask<String> {
  /**
   * Notification to send.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Explicit serialVersionUID.
   */
  static final String MSG = "start";
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
   * Determines whether this task timed out.
   */
  private boolean timedout = false;

  /**
   * Initialize this task.
   */
  public LifeCycleTask() {
  }

  /**
   * Initialize this task.
   * @param duration specifies the duration of this task.
   */
  public LifeCycleTask(final long duration) {
    this.duration = duration;
  }

  @Override
  public void run() {
    start = System.nanoTime();
    try {
      Thread.sleep(500L);
      final JPPFManagementInfo info = getNode().getManagementInfo();
      final int id = getNode().getConfiguration().get(JPPFProperties.PROVISIONING_SLAVE_ID);
      final String type = (id < 0) ? "master" : String.format("slave.id = %2d", id);
      final String s = String.format("%s-%s:%d (%-13s)", MSG, info.getHost(), info.getPort(), type);
      fireNotification(s, true);
      if (duration > 0) Thread.sleep(duration);
      elapsed = (System.nanoTime() - start) / 1_000_000L;
      setResult("execution succesful in " + elapsed + " ms");
      displayElapsed("successful");
    } catch (final Exception e) {
      setThrowable(e);
    }
  }

  @Override
  public void onCancel() {
    elapsed = (System.nanoTime() - start) / 1_000_000L;
    cancelled = true;
    displayElapsed("cancelled");
  }

  @Override
  public void onTimeout() {
    elapsed = (System.nanoTime() - start) / 1_000_000L;
    timedout = true;
    displayElapsed("timed out");
  }

  /**
   * Log or display a message showing the execution status and elapsed of this task.
   * @param message a short message describing the life cycle status.
   */
  private void displayElapsed(final String message) {
    log.info("task id='" + getId() + "' " + message + ", duration=" + duration + ", result=" + getResult() + ", elapsed=" + elapsed);
  }

  /**
   * Determine whether this task was cancelled.
   * @return true if the task was cancelled, false otherwise.
   */
  public boolean isCancelled() {
    return cancelled;
  }

  /**
   * Determine whether this task timed out.
   * @return true if the task timed out, false otherwise.
   */
  public boolean isTimedout() {
    return timedout;
  }
}
