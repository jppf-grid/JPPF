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

package org.jppf.test.addons.jobtaskslistener;

import org.jppf.node.protocol.AbstractTask;
import org.jppf.utils.ExceptionUtils;

/**
 * @author Laurent Cohen
 */
public class MyJobTasksListenerTask extends AbstractTask<String> {
  /** */
  public static final String RESULT_SUCCESS = "success";
  /** */
  public static final String RESULT_FAILURE = "failure";
  /**
   * How long this task will sleep.
   */
  private final long duration;

  /**
   * @param duration how long this task will sleep.
   */
  public MyJobTasksListenerTask(final long duration) {
    this.duration = duration;
  }

  /**
   * @param id id of this task.
   * @param duration how long this task will sleep.
   */
  public MyJobTasksListenerTask(final String id, final long duration) {
    setId(id);
    this.duration = duration;
  }

  @Override
  public void run() {
    System.out.println("starting task " + getId());
    try {
      if (duration > 0L) Thread.sleep(duration);
      setResult(RESULT_SUCCESS);
      System.out.printf("task %s success%n", getId());
    } catch (Exception e) {
      setResult(RESULT_FAILURE);
      setThrowable(e);
      System.out.printf("task %s failure: %s%n", getId(), ExceptionUtils.getMessage(e));
    }
  }
}
