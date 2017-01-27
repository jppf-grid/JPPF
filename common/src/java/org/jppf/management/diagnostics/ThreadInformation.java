/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

package org.jppf.management.diagnostics;

import java.io.Serializable;
import java.lang.management.*;
import java.util.*;

import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * Information about a thread, including the stack trace and associated locks.
 * @author Laurent Cohen
 */
public class ThreadInformation implements Serializable
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ThreadInformation.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The id of this thread.
   */
  private final long id;
  /**
   * The name of this thread.
   */
  private final String name;
  /**
   * The state of this thread.
   */
  private final Thread.State state;
  /**
   * The stack trace of this thread.
   */
  private final List<StackFrameInformation> stackTrace;
  /**
   * The ownable synchronizers held by this thread.
   */
  private final List<LockInformation> ownableSynchronizers;
  /**
   * The count of times this thread has been waiting.
   */
  private final long waitCount;
  /**
   * The total cumulated wait time.
   */
  private final long waitTime;
  /**
   * The count of times this thread has been blocked.
   */
  private final long blockedCount;
  /**
   * The total cumulated block time.
   */
  private final long blockedTime;
  /**
   * Whether this thread is uspended.
   */
  private final boolean suspended;
  /**
   * Whether this thread is in native code.
   */
  private final boolean inNative;
  /**
   * The the lock this thread is waiting for, if any.
   */
  private final LockInformation lockInformation;
  /**
   * The id of the owner of the lock this thread is waiting for, if any.
   */
  private final long lockOwnerId;

  /**
   * Initialize this object from the specified {@link ThreadInfo}.
   * @param ti the <code>ThreadInfo</code> from which to get the information.
   */
  public ThreadInformation(final ThreadInfo ti)
  {
    this.id = ti.getThreadId();
    this.name = ti.getThreadName();
    this.state = ti.getThreadState();
    this.waitCount = ti.getWaitedCount();
    this.waitTime = ti.getWaitedTime();
    this.blockedCount = ti.getBlockedCount();
    this.blockedTime = ti.getBlockedTime();
    this.suspended = ti.isSuspended();
    this.inNative = ti.isInNative();
    this.lockOwnerId = ti.getLockOwnerId();
    LockInfo linfo = ti.getLockInfo();
    this.lockInformation = linfo != null ? new LockInformation(linfo) : null;
    this.stackTrace = fillStackTrace(ti);
    LockInfo[] sync = ti.getLockedSynchronizers();
    if (sync.length > 0)
    {
      ownableSynchronizers = new ArrayList<>();
      for (LockInfo li: sync) ownableSynchronizers.add(new LockInformation(li));
      if (debugEnabled) log.debug("thread '" + name + "' ownable synchronizers: " + ownableSynchronizers);
    }
    else
    {
      ownableSynchronizers = null;
      if (debugEnabled) log.debug("thread '" + name + "' has no ownable synchronizer");
    }
  }

  /**
   * Fill the stack trace information for this thread.
   * @param ti the ThreadInfo from hich to get the information.
   * @return a list of {@link StackFrameInformation} objects.
   */
  private List<StackFrameInformation> fillStackTrace(final ThreadInfo ti)
  {
    StackTraceElement[] ste = ti.getStackTrace();
    if (ste.length <= 0) return null;
    List<StackFrameInformation> result = new ArrayList<>();
    SortedMap<Integer, LockInformation> lockInfoMap = new TreeMap<>();
    for (MonitorInfo mi: ti.getLockedMonitors())
    {
      int idx = mi.getLockedStackDepth();
      if (idx >= 0) lockInfoMap.put(idx, new LockInformation(mi));
    }
    for (int i=0; i<ste.length; i++) result.add(new StackFrameInformation(ste[i], lockInfoMap.get(i)));
    return result;
  }

  /**
   * Get the id of this thread.
   * @return the id as a long.
   */
  public long getId()
  {
    return id;
  }

  /**
   * Get the name of this thread.
   * @return the name as a string.
   */
  public String getName()
  {
    return name;
  }

  /**
   * Get the state of this thread.
   * @return a {@link java.lang.Thread.State Thread.State} enum value.
   */
  public Thread.State getState()
  {
    return state;
  }

  /**
   * Get the stack trace of this thread.
   * @return a list of {@link StackFrameInformation} elements, or <code>null</code> if no stack trace is available.
   */
  public List<StackFrameInformation> getStackTrace()
  {
    return stackTrace;
  }

  /**
   * Get the ownable synchronizers held by this thread.
   * @return a list of {@link LockInformation}, or null if this thread holds no ownable synchrnizer.
   */
  public List<LockInformation> getOwnableSynchronizers()
  {
    return ownableSynchronizers;
  }

  /**
   * Get the count of times this thread has been waiting.
   * @return the wait count as a long.
   */
  public long getWaitCount()
  {
    return waitCount;
  }

  /**
   * Get the total cumulated wait time.
   * @return the wait time as a long.
   */
  public long getWaitTime()
  {
    return waitTime;
  }

  /**
   * Get the count of times this thread has been blocked.
   * @return the blocked count as a long.
   */
  public long getBlockedCount()
  {
    return blockedCount;
  }

  /**
   * Get the total cumulated block time.
   * @return the blocked time as a long.
   */
  public long getBlockedTime()
  {
    return blockedTime;
  }

  /**
   * Get whether this thread is suspended.
   * @return <code>true</code> if this thread is suspended, <code>false</code> otherwise.
   */
  public boolean isSuspended()
  {
    return suspended;
  }

  /**
   * Get whether this thread is in native code.
   * @return <code>true</code> if this thread is in native code, <code>false</code> otherwise.
   */
  public boolean isInNative()
  {
    return inNative;
  }

  /**
   * Get the lock this thread is waiting for, if any.
   * @return a {@link LockInformation} instance, or <code>null</code> if this thread is not waiting on a lock.
   */
  public LockInformation getLockInformation()
  {
    return lockInformation;
  }

  /**
   * Get the id of the owner of the lock this thread is waiting for, if any.
   * @return the owner thread id as positive long value, or -1 if this thread is not waiting on a lock.
   */
  public long getLockOwnerId()
  {
    return lockOwnerId;
  }
}
