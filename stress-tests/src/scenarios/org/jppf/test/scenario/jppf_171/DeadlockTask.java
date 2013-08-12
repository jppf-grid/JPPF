/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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
package org.jppf.test.scenario.jppf_171;

import org.jppf.server.protocol.JPPFTask;

/**
 * This class is a template for a standard JPPF task.
 * @author Laurent Cohen
 */
public class DeadlockTask extends JPPFTask
{
  /**
   * How long to sleep before deserializing.
   */
  static final long EXEC_SLEEP_TIME = 50L;
  /**
   * 
   */
  private int idx = 0;
  /**
   * 
   */
  private long sleepTime = 1L;

  /**
   * Perform initializations.
   * @param idx the idx.
   * @param sleepTime the time to sleep.
   */
  public DeadlockTask(final int idx, final long sleepTime)
  {
    this.idx = idx;
    setResult("*** task " + idx + " was not executed ***");
  }

  @Override
  public void run()
  {
    try
    {
      Thread.sleep(EXEC_SLEEP_TIME);
      setResult("task " + idx + " executed successfully");
    }
    catch (Exception e)
    {
      setException(e);
    }
  }
}
