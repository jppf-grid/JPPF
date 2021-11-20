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

package test.org.jppf.server.protocol;

import org.jppf.node.protocol.AbstractTask;

import test.org.jppf.test.setup.common.BaseTestHelper;

/**
 * 
 * @author Laurent Cohen
 */
public class TasksForTestJPPFJobSLA3 {
  /**
   * A simple task.
   */
  public static class Task1 extends AbstractTask<String> {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    @Override
    public void run() {
      setResult(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE);
    }
  }

  /**
   * Another simple task.
   */
  public static class Task2 extends AbstractTask<String> {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    @Override
    public void run() {
      setResult(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE);
    }
  }
}
