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

package org.jppf.client.balancer.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class that support job state management.
 * @author Martin JANDA
 */
public abstract class AbstractClientJob
{
  /**
   * Job status is new (just submitted).
   */
  protected static final int NEW = 0;
  /**
   * Job status is executing.
   */
  protected static final int EXECUTING = 1;
  /**
   * Job status is done/complete.
   */
  protected static final int DONE = 2;
  /**
   * Job status is cancelled.
   */
  protected static final int CANCELLED = 3;
  /**
   * The job status.
   */
  private volatile int status = NEW;
  /**
   * List of all runnables called on job completion.
   */
  private final List<Runnable> onDoneList = new ArrayList<Runnable>();

  /**
   * Updates status to new value if old value is equal to expect.
   * @param expect the expected value.
   * @param newStatus the new value.
   * @return <code>true</code> if new status was set.
   */
  protected final boolean updateStatus(final int expect, final int newStatus)
  {
    if(status == expect)
    {
      status = newStatus;
      return true;
    }
    else return false;
  }

  /**
   * @return <code>true</code> when job is cancelled or finished normally.
   */
  public boolean isDone()
  {
    return status >= EXECUTING;
  }

  /**
   * @return <code>true</code> when job was cancelled.
   */
  public boolean isCancelled()
  {
    return status >= CANCELLED;
  }

  /**
   * Cancels this job.
   * @return whether cancellation was successful.
   */
  public boolean cancel()
  {
    if (status > EXECUTING) return false;
    status = CANCELLED;
    return true;
  }

/**
   * Called when task was cancelled or finished.
   */
  protected void done()
  {
    Runnable[] runnables;
    synchronized (onDoneList)
    {
      runnables = onDoneList.toArray(new Runnable[onDoneList.size()]);
    }
    for (Runnable runnable : runnables)
    {
      runnable.run();
    }
  }

  /**
   * Registers instance to be called on job finish.
   * @param runnable {@link Runnable} to be called on job finish.
   */
  public void addOnDone(final Runnable runnable)
  {
    if(runnable == null) throw new IllegalArgumentException("runnable is null");

    synchronized (onDoneList)
    {
      onDoneList.add(runnable);
    }
  }

  /**
   * Deregisters instance to be called on job finish.
   * @param runnable {@link Runnable} to be called on job finish.
   */
  public void removeOnDone(final Runnable runnable)
  {
    if(runnable == null) throw new IllegalArgumentException("runnable is null");

    synchronized (onDoneList)
    {
    onDoneList.remove(runnable);
    }
  }
}
