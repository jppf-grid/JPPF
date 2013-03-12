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
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * A simple JPPF task for unit-testing the task life cycle.
 * @author Laurent Cohen
 */
public class LifeCycleTask extends JPPFTask
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(LifeCycleTask.class);
  /**
   * One million.
   */
  protected static final int ONE_MILLION = 1000 * 1000;
  /**
   * The duration of this task;
   */
  protected long duration = 0L;
  /**
   * used to store the task's execution start time in nanoseconds.
   */
  protected double start = 0L;
  /**
   * Measures the time elapsed between the task execution start and its completion in nanoseconds.
   */
  protected double elapsed = 0L;
  /**
   * Determines whether this task was cancelled.
   */
  protected boolean cancelled = false;
  /**
   * Determines whether this task timed out.
   */
  protected boolean timedout = false;
  /**
   * Determines whether this task was executed in a node or in the client's local executor.
   */
  protected boolean executedInNode = true;
  /**
   * The uuid of the node this task executes on.
   */
  protected String nodeUuid = null;

  /**
   * Initialize this task.
   */
  public LifeCycleTask()
  {
  }

  /**
   * Initialize this task.
   * @param duration specifies the duration of this task.
   */
  public LifeCycleTask(final long duration)
  {
    this.duration = duration;
  }

  @Override
  public void run()
  {
    // System.nanoTime() has a different origin on different JVM instances
    // so this value can't be used to compute the start time.
    long nanoStart = System.nanoTime();
    start = System.currentTimeMillis();
    //start = (start * ONE_MILLION) + (nanoStart % ONE_MILLION);
    start *= ONE_MILLION;
    
    try
    {
      executedInNode = isInNode();
      TypedProperties config = JPPFConfiguration.getProperties();
      synchronized(config)
      {
        nodeUuid = config.getString("jppf.node.uuid");
        if (nodeUuid == null)
        {
          nodeUuid = new JPPFUuid().toString();
          config.setProperty("jppf.node.uuid", nodeUuid);
        }
      }
      if (duration > 0L) Thread.sleep(duration);
      setResult(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE);
      displayTask("successful");
    }
    catch(Exception e)
    {
      setException(e);
      //e.printStackTrace();
    }
    finally
    {
      elapsed = System.nanoTime() - nanoStart;
    }
  }

  @Override
  public void onCancel()
  {
    cancelled = true;
    displayTask("cancelled");
  }

  @Override
  public void onTimeout()
  {
    timedout = true;
    displayTask("timed out");
  }

  /**
   * Log or display a message showing the execution status and elapsed of this task.
   * @param message a short message describing the life cycle status.
   */
  private void displayTask(final String message)
  {
    log.debug("displaying task " + this + " (" + message + ')');
  }

  /**
   * Determine whether this task was cancelled.
   * @return <code>true</code> if the task was cancelled, <code>false</code> otherwise.
   */
  public boolean isCancelled()
  {
    return cancelled;
  }

  /**
   * Determine whether this task timed out.
   * @return <code>true</code> if the task timed out, <code>false</code> otherwise.
   */
  public boolean isTimedout()
  {
    return timedout;
  }

  /**
   * Determine whether this task was executed in a node or in the client's local executor.
   * @return <code>true</code> if this task was executed in a node, <code>false</code> otherwise.
   */
  public boolean isExecutedInNode()
  {
    return executedInNode;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("id=").append(getId());
    sb.append(", duration=").append(duration);
    sb.append(", timedout=").append(timedout);
    sb.append(", cancelled=").append(cancelled);
    sb.append(", executedInNode=").append(executedInNode);
    sb.append(", elapsed=").append(elapsed);
    sb.append(", result=").append(getResult());
    sb.append(", nodeUuid=").append(nodeUuid);
    sb.append(']');
    return sb.toString();
  }

  /**
   * Get the task's execution start time.
   * @return the start time in nanoseconds.
   */
  public double getStart()
  {
    return start;
  }

  /**
   * Get the time elapsed between the task execution start its completion.
   * @return the elapsed time in nanoseconds.
   */
  public double getElapsed()
  {
    return elapsed;
  }

  /**
   * Get the uuid of the node this task executes on.
   * @return the uuid as a string.
   */
  public String getNodeUuid()
  {
    return nodeUuid;
  }
}
