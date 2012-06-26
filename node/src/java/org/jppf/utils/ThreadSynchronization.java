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

package org.jppf.utils;

/**
 * This class implements a goToSleep and a wakeUp method as wrappers to {@link java.lang.Object#wait() Object.wait()} and
 * {@link java.lang.Object#notifyAll() Object.notifyAll()} wrappers.
 * @author Laurent Cohen
 * @exclude
 */
public abstract class ThreadSynchronization
{
  /**
   * Determines whether the thread's run() method should terminate.
   */
  protected boolean stopped = false;

  /**
   * Cause the current thread to wait until notified.
   */
  public synchronized void goToSleep()
  {
    try
    {
      wait();
    }
    catch(InterruptedException ignored)
    {
    }
  }

  /**
   * Cause the current thread to wait until notified or the specified time has passed, whichever comes first.
   * @param time the maximum time to wait in milliseconds.
   */
  public synchronized void goToSleep(final long time)
  {
    try
    {
      wait(time);
    }
    catch(InterruptedException ignored)
    {
    }
  }

  /**
   * Cause the current thread to wait until notified or the specified time has passed, whichever comes first.
   * @param millis the maximum time to wait in milliseconds.
   * @param nanos the additional time to wait in nanoseconds.
   */
  public synchronized void goToSleep(final long millis, final int nanos)
  {
    try
    {
      wait(millis, nanos);
    }
    catch(InterruptedException ignored)
    {
    }
  }

  /**
   * Notify the threads currently waiting on this object that they can resume.
   */
  public synchronized void wakeUp()
  {
    notifyAll();
  }

  /**
   * Determine whether the thread's <code>run()</code> method is terminated.
   * @return  true if the thread is stopped, false otherwise.
   */
  public synchronized boolean isStopped()
  {
    return stopped;
  }

  /**
   * Specify whether the thread's <code>run()</code> should terminate.
   * @param stopped true if the thread is to be stopped, false otherwise.
   */
  public synchronized void setStopped(final boolean stopped)
  {
    this.stopped = stopped;
  }
}
