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
package test.classversion;

import org.jppf.server.protocol.JPPFTask;


/**
 * This task is for testing the network transfer of task with various data sizes.
 * @author Laurent Cohen
 */
public class TaskVersion extends JPPFTask
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Default construction.
   */
  public TaskVersion()
  {
  }

  /**
   * Initialize this task with a byte array of the specified size.
   * The array is created at construction time and passed on to the node, or task execution time and passed back to the client,
   * depending on the inNodeOnly flag.
   * @param datasize the size in byte of the byte array this task owns.
   * @param inNodeOnly if true, the array is created at execution time, otherwise at construction time.
   */
  public TaskVersion(final int datasize, final boolean inNodeOnly)
  {
  }

  /**
   * Perform the multiplication of a matrix row by another matrix.
   * @see sample.BaseDemoTask#doWork()
   */
  @Override
  public void run()
  {
    try
    {
      String msg = "execution successful for task version 2";
      setResult(msg);
      System.out.println(msg);
    }
    catch(Exception e)
    {
      setException(e);
    }
  }
}

