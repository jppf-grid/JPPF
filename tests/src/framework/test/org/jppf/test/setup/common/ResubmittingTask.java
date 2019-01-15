/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

/**
 * A task that resubmits itself. Be careful to call job.getSLA().setMaxTaskResubmit(1) or with another appropriate value.
 */
public class ResubmittingTask extends LifeCycleTask {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Initialize this task.
   * @param duration duration of the task in ms.
   */
  public ResubmittingTask(final long duration) {
    super(duration);
  }

  @Override
  public void run() {
    super.run();
    this.setResubmit(true);
  }
}