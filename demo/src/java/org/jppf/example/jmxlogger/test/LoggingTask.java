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
package org.jppf.example.jmxlogger.test;

import org.jppf.server.protocol.JPPFTask;
import org.slf4j.*;

/**
 * Example of a JPPF task that sends status notifications during its execution.
 * @author Laurent Cohen
 */
public class LoggingTask extends JPPFTask
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(LoggingTask.class);

  /**
   * Initialize this task with the specified id.
   * @param id the task id.
   */
  public LoggingTask(final String id)
  {
    setId(id);
  }

  /**
   * This method contains the code that will be executed by a node.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run()
  {
    log.info("task " + getId() + " has started");
    try
    {
      Thread.sleep(1000L);
      setResult("the execution was performed successfully");
    }
    catch(InterruptedException e)
    {
      setException(e);
    }
    log.info("task " + getId() + " has ended");
  }
}
