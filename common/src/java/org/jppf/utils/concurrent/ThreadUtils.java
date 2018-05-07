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

package org.jppf.utils.concurrent;

/**
 * Utility methods for creating, starting and manipulating threads.
 * @author Laurent Cohen
 */
public class ThreadUtils {
  /**
   * Start a non-daemon thread with the specified {@code Runnable} and name.
   * @param runnable the runnable executed by the thread.
   * @param name the name given to the thread.
   * @return the new, started thread.
   */
  public static Thread startThread(final Runnable runnable, final String name) {
    return startThread(runnable, name, false);
  }

  /**
   * Start a daemon thread with the specified {@code Runnable} and name.
   * @param runnable the runnable executed by the thread.
   * @param name the name given to the thread.
   * @return the new, started thread.
   */
  public static Thread startDaemonThread(final Runnable runnable, final String name) {
    return startThread(runnable, name, true);
  }

  /**
   * Start a thread with the specified {@code Runnable}, name and daemon flag.
   * @param runnable the runnable executed by the thread.
   * @param name the name given to the thread.
   * @param daemon whether the new thread should be a daemon thread.
   * @return the new, started thread.
   */
  private static Thread startThread(final Runnable runnable, final String name, final boolean daemon) {
    final Thread thread = (name == null) ? new DebuggableThread(runnable) : new DebuggableThread(runnable, name);
    thread.setDaemon(daemon);
    thread.start();
    return thread;
  }
}
