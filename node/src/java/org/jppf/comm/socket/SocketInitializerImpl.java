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
package org.jppf.comm.socket;

import java.util.*;
import java.util.concurrent.locks.*;

import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class attempt to connect a {@link org.jppf.comm.socket.SocketWrapper SocketWrapper} to a remote server.
 * The connection attempts are performed until a configurable amount of time has passed, and at a configurable time interval.
 * When no attempt succeeded, a <code>JPPFError</code> is thrown, and the application should normally exit.
 * @author Laurent Cohen
 */
public class SocketInitializerImpl extends AbstractSocketInitializer
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(SocketInitializerImpl.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether the trace level is enabled in the logging configuration, without the cost of a method call.
   */
  private boolean traceEnabled = log.isTraceEnabled();
  /**
   * Date after which this task stop trying to connect the class loader.
   */
  private Date latestAttemptDate = null;
  /**
   * The locking lock used to block all class loaders while initializing the connection.
   */
  private ReentrantLock lock = new ReentrantLock();
  /**
   * The locking condition used to block all class loaders while initializing the connection.
   */
  private Condition condition = lock.newCondition();
  /**
   * The timer that periodically attempts the connection to the server.
   */
  private Timer timer = null;
  /**
   * The task run by the timer.
   */
  private SocketInitializationTask task = null;

  /**
   * Instantiate this SocketInitializer with a specified socket wrapper.
   */
  public SocketInitializerImpl()
  {
  }

  /**
   * Initialize the underlying socket client, by starting a <code>Timer</code> and a corresponding
   * <code>TimerTask</code> until a specified amount of time has passed.
   * @param socketWrapper the socket wrapper to initialize.
   * @see org.jppf.comm.socket.SocketInitializer#initializeSocket(org.jppf.comm.socket.SocketWrapper)
   */
  @Override
  public void initializeSocket(final SocketWrapper socketWrapper)
  {
    String errMsg = "SocketInitializer.initializeSocket(): Could not reconnect to the remote server";
    String fatalErrMsg = "FATAL: could not initialize the Socket Wrapper!";
    this.socketWrapper = socketWrapper;
    lock.lock();
    try
    {
      try
      {
        if (debugEnabled) log.debug(name + "about to close socket wrapper");
        /*if(socketWrapper.isOpened())*/ socketWrapper.close();
      }
      catch(Exception e)
      {
      }
      // random delay between 0 and 1 second , to avoid overloading the server with simultaneous connection requests.
      TypedProperties props = JPPFConfiguration.getProperties();
      long delay = 1000L * props.getLong("reconnect.initial.delay", 0L);
      //if (delay == 0L) delay = rand.nextInt(1000);
      if (delay == 0L) delay = rand.nextInt(10);
      long maxTime = props.getLong("reconnect.max.time", 60L);
      long maxDuration = (maxTime <= 0) ? -1L : 1000L * maxTime;
      long period = 1000L * props.getLong("reconnect.interval", 1L);
      latestAttemptDate = (maxDuration > 0) ? new Date(System.currentTimeMillis() + maxDuration) : null;
      task = new SocketInitializationTask();
      timer = new Timer("Socket initializer (" + instanceNumber + ") timer for " + socketWrapper, true);
      timer.schedule(task, delay, period);
      try
      {
        condition.await();
      }
      catch(InterruptedException e)
      {
        if (debugEnabled) log.debug(name + e.getMessage(), e);
      }
      timer.cancel();
      timer.purge();
      if (!isSuccessful() && !closed)
      {
        if (debugEnabled) log.debug(name + errMsg);
        System.err.println(name + errMsg);
      }
    }
    finally
    {
      lock.unlock();
    }
  }

  /**
   * Close this initializer.
   * @see org.jppf.comm.socket.SocketInitializer#close()
   */
  @Override
  public void close()
  {
    if (!closed)
    {
      if (debugEnabled) log.debug(name + "closing socket initializer");
      closed = true;
      if (task != null) task.cancel();
      if (timer != null)
      {
        if (debugEnabled) log.debug(name + " timer not null");
        timer.cancel();
        timer.purge();
        timer = null;
      }
      signalAll();
    }
  }

  /**
   * Signal all threads waiting on the condition.
   */
  private void signalAll()
  {
    lock.lock();
    try
    {
      condition.signalAll();
    }
    finally
    {
      lock.unlock();
    }
  }

  /**
   * This timer task attempts to (re)connect a socket wrapper to its corresponding remote server.
   * It also checks that the maximum duration for the attempts has not been reached, and cancels itself if it has.
   */
  class SocketInitializationTask extends TimerTask
  {
    /**
     * Attempt to connect to the remote server.
     * @see java.util.TimerTask#run()
     */
    @Override
    public void run()
    {
      attemptCount++;
      try
      {
        if (traceEnabled) log.trace(name + " opening the socket connection");
        socketWrapper.open();
        successfull = true;
        if (traceEnabled) log.trace(name + " socket connection successfully opened");
        reset();
      }
      catch(Exception e)
      {
        if (traceEnabled) log.trace(name + " socket connection open failed: " + e.getClass().getName() + " : " + e.getMessage());
        if (latestAttemptDate != null)
        {
          Date now = new Date();
          if (now.after(latestAttemptDate))
          {
            successfull = false;
            if (traceEnabled) log.trace(name + " socket initialization unsuccessful");
            reset();
          }
        }
      }
    }

    /**
     * Reset the status of this task.
     */
    private void reset()
    {
      cancel();
      signalAll();
    }
  }
}
