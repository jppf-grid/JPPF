/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

import java.io.Serializable;

/**
 * A simple runnable task implementation.
 */
public class SimpleRunnable implements Runnable, Serializable
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The result of executing this task.
   */
  private TaskResult executionResult = null;

  /**
   * Default constructor.
   */
  public SimpleRunnable()
  {
  }

  /**
   * Initialize this task with the specified result object.
   * @param result the result to use.
   */
  public SimpleRunnable(final TaskResult result)
  {
    this.executionResult = result;
  }

  /**
   * Execute this task.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run()
  {
    executionResult.message = BaseSetup.EXECUTION_SUCCESSFUL_MESSAGE;
  }
}
