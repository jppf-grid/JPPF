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

package test.org.jppf.test.setup.common;

import org.jppf.node.NodeRunner;
import org.jppf.node.protocol.AbstractTask;
import org.jppf.test.addons.mbeans.UserObject;
import org.jppf.utils.ExceptionUtils;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class NotifyingTask extends AbstractTask<String> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(NotifyingTask.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Prefix of the string sent as notification at the start of a task.
   */
  public static final String START_PREFIX = "start: ";
  /**
   * Prefix of the string sent as notification at the end of a task.
   */
  public static final String END_PREFIX = "end: ";
  /**
   * Result set onto the task when it executes successfully.
   */
  public static final String SUCCESS = "success";
  /**
   * The duration of this task.
   */
  private final long duration;
  /**
   * Whether to send a notification at the start of this task execution.
   */
  private final boolean notifyStart;
  /**
   * Whether to send a notification at the end of this task execution.
   */
  private final boolean notifyEnd;

  /**
   * Initialize this task with <code>notifyStart = false</code> and <code>notifyEnd = true</code>.
   * @param duration the duration of this task.
   */
  public NotifyingTask(final long duration) {
    this(duration, false, true);
  }

  /**
   * Initialize this task.
   * @param duration the duration of this task.
   * @param notifyStart whether to send a notification at the start of this task execution.
   * @param notifyEnd whether to send a notification at the end of this task execution.
   */
  public NotifyingTask(final long duration, final boolean notifyStart, final boolean notifyEnd) {
    this.duration = duration;
    this.notifyStart = notifyStart;
    this.notifyEnd = notifyEnd;
  }

  @Override
  public void run() {
    try {
      if (notifyStart) TaskNotifier.addNotification(new UserObject(NodeRunner.getUuid(), START_PREFIX + getId()));
      if (debugEnabled) log.debug("sent start notification from {}", this);
      Thread.sleep(duration);
      if (notifyEnd) TaskNotifier.addNotification(new UserObject(NodeRunner.getUuid(), END_PREFIX + getId()));
      if (debugEnabled) log.debug("sent end notification from {}", this);
      setResult(SUCCESS);
      //System.out.println("task " + getId() + " successful");
    } catch (@SuppressWarnings("unused") final Exception e) {
      //System.out.println("Error on task " + getId() + " : " + ExceptionUtils.getMessage(e));
      //e.printStackTrace();
    }
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName())
      .append("[id=").append(getId()) .append(", result=").append(getResult())
      .append(", throwable=").append(ExceptionUtils.getMessage(getThrowable())) .append(", duration=").append(duration)
      .append(", notifyStart=").append(notifyStart) .append(", notifyEnd=").append(notifyEnd)
      .append(']') .toString();
  }
}
