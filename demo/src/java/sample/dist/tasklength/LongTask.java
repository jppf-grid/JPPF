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
package sample.dist.tasklength;

import org.jppf.server.protocol.JPPFTask;
import org.slf4j.*;

/**
 * Instances of this class are defined as tasks with a predefined execution length, specified at their creation.
 * @author Laurent Cohen
 */
public class LongTask extends JPPFTask
{
  /**
   * Logger.
   */
  private static Logger log = LoggerFactory.getLogger(LongTask.class);
  /**
   * Determines how long this task will run.
   */
  private long taskLength = 0L;
  /**
   * Timestamp marking the time when the task execution starts.
   */
  private long taskStart = 0L;
  /**
   * Determines this task's behavior: false if it should just sleep during its allocated time, or true if it should
   * do some make-do work that uses the cpu.
   */
  private boolean useCPU = false;

  /**
   * Default constructor.
   */
  public LongTask()
  {
  }

  /**
   * Initialize this task with a predefined length of time, in milliseconds, during which it will run.
   * @param taskLength - determines how long this task will run.
   * @param useCPU - determines whether this task should just sleep during its allocated time or do some cpu-intensive work.
   */
  public LongTask(final long taskLength, final boolean useCPU)
  {
    this.taskLength = taskLength;
    this.useCPU = useCPU;
  }

  /**
   * Initialize this task with a predefined length of time, in milliseconds, during which it will run.
   * @param taskLength determines how long this task will run.
   */
  public LongTask(final long taskLength)
  {
    this(taskLength, false);
  }

  /**
   * Perform the execution of this task.
   * @see sample.BaseDemoTask#doWork()
   */
  @Override
  public void run()
  {
    //System.out.println("Starting task " + getId());
    taskStart = System.currentTimeMillis();
    long elapsed = 0L;
    try
    {
      if (useCPU)
      {
        for (; elapsed < taskLength; elapsed = System.currentTimeMillis() - taskStart)
        {
          String s = "";
          for (int i=0; i<10; i++) s += "A10";
        }
      }
      else
      {
        Thread.sleep(taskLength);
        elapsed = System.currentTimeMillis() - taskStart;
      }
      setResult("task has run for " + elapsed + " ms");
      System.out.println("Task " + getId() + " complete");
    }
    catch(InterruptedException e)
    {
      //setException(e);
      setResult(e.getClass().getName() + " : " + e.getMessage());
    }
  }

  /**
   * Called when this task is cancelled.
   * @see org.jppf.server.protocol.JPPFTask#onCancel()
   */
  @Override
  public void onCancel()
  {
    String s = "task " + getId() + " has been cancelled";
    setResult(s);
    System.out.println(s);
  }
}
