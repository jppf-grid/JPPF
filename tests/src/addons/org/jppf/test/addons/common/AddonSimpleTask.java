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

package org.jppf.test.addons.common;

import org.jppf.node.protocol.AbstractTask;
import org.slf4j.*;

/**
 * A simple JPPF task for unit-testing.
 * @author Laurent Cohen
 */
public class AddonSimpleTask extends AbstractTask<String> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AddonSimpleTask.class);
  /**
   * Message used for successful task execution.
   */
  public static final String EXECUTION_SUCCESSFUL_MESSAGE = "execution successful";
  /**
   * The duration of this task;
   */
  private long duration = 0L;

  /**
   * Initialize this task.
   */
  public AddonSimpleTask() {
  }

  /**
   * Initialize this task.
   * @param duration specifies the duration of this task.
   */
  public AddonSimpleTask(final long duration) {
    this.duration = duration;
  }

  @Override
  public void run() {
    try {
      if (duration > 0) Thread.sleep(duration);
      setResult(EXECUTION_SUCCESSFUL_MESSAGE);
      log.info("task id =" + getId() + ", duration=" + duration + ", result=" + getResult());
    } catch (Exception e) {
      setThrowable(e);
    }
  }
}
