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

package test.org.jppf.test.setup.common;

import org.jppf.server.protocol.JPPFTask;
import org.slf4j.*;

/**
 * A simple JPPF task for unit-testing.
 * @author Laurent Cohen
 */
public class SimpleTask extends JPPFTask
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(SimpleTask.class);
  /**
   * The duration of this task;
   */
  private long duration = 0L;

  /**
   * Initialize this task.
   */
  public SimpleTask()
  {
  }

  /**
   * Initialize this task.
   * @param duration specifies the duration of this task.
   */
  public SimpleTask(final long duration)
  {
    this.duration = duration;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void run()
  {
    try
    {
      if (duration > 0) Thread.sleep(duration);
      setResult(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE);
      log.info("task id =" + getId() + ", duration=" + duration + ", result=" + getResult());
    }
    catch(Exception e)
    {
      setException(e);
    }
  }
}
