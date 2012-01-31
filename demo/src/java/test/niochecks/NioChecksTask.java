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
package test.niochecks;

import org.jppf.JPPFException;
import org.jppf.server.protocol.JPPFTask;

/**
 * This class is a template for a standard JPPF task.
 * @author Laurent Cohen
 */
public class NioChecksTask extends JPPFTask
{
  /**
   * The duration of this task.
   */
  private final long duration;

  /**
   * Default constructor,
   * @param duration the duration of this task.
   */
  public NioChecksTask(final long duration)
  {
    this.duration = duration < 1L ? 1L : duration;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void run()
  {
    System.out.println("Starting the task");
    try
    {
      Thread.sleep(duration);
      setResult("the execution was performed successfully");
    }
    catch(Exception e)
    {
      setException(e);
      e.printStackTrace();
    }
    catch(Error e)
    {
      e.printStackTrace();
      setException(new JPPFException(e));
    }
  }
}
