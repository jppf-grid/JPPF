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

package org.jppf.utils;

import org.slf4j.*;

/**
 * A thread that prints out the call stack when {@link #interrupt()} is called.
 */
public class DebuggableThread extends Thread {
  /**
    * Logger for this class.
    */
  private static Logger log = LoggerFactory.getLogger(DebuggableThread.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Construct this thread.
   * @param target the associate {@code Runnable}.
   * @param name the name of this thread.
   */
  public DebuggableThread(final Runnable target, final String name) {
    this(Thread.currentThread().getThreadGroup(), target, name);
  }

  /**
   * Construct this thread.
   * @param group the thread group owning this thread.
   * @param target the associate {@code Runnable}.
   * @param name the name of this thread.
   */
  public DebuggableThread(final ThreadGroup group, final Runnable target, final String name) {
    super(group, target, name);
  }

  @Override
  public void interrupt() {
    if (debugEnabled) log.debug("interrupt() called on {}, call stack:\n{}", this, ExceptionUtils.getCallStack());
    super.interrupt();
  }
}