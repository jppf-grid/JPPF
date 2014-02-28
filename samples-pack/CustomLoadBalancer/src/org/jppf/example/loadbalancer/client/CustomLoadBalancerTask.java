/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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
package org.jppf.example.loadbalancer.client;

import org.jppf.server.protocol.JPPFTask;

/**
 * This task is used to test our custom load-balancer.
 * Its execution depends on 2 parameters:<br>
 * - a data size, used to instantiate an array of bytes of the specified size, and corresponding
 * approximately to the task memory footprint specified in the job metadata<br>
 * - a task duration, used to set the task duration to the specified value, and corresponding
 * to the maximum task length specified in the job metadata.
 * @author Laurent Cohen
 */
public class CustomLoadBalancerTask extends JPPFTask
{
  /**
   * The task data, corresponding approximately to its memory footprint.
   */
  private byte[] data = null;
  /**
   * The task duration, corresponding the maximum task length specified in the job metadata.
   */
  private long duration = 0L;

  /**
   * Initialize this task with the specified data size and task duration.
   * @param size the data size in bytes.
   * @param duration the task duration in milliseconds.
   */
  public CustomLoadBalancerTask(final int size, final long duration)
  {
    data = new byte[size];
    this.duration = duration;
  }

  /**
   * The execution of this task consists in performing a <code>Thread.sleep(duration)</code>,
   * and printing a message that will allow us to check that the load-balancer behaved as expected.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run()
  {
    // this task's id is specified on the client side and contains
    // the name of the job it is a part of
    System.out.println("Starting execution of task " + this.getId());
    try
    {
      Thread.sleep(duration);
    }
    catch (Exception e)
    {
      setThrowable(e);
    }
    // set the execution results
    setResult("the execution was performed successfully");
  }
}
