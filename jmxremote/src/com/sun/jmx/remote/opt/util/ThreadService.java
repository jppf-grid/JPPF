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
/*
 * @(#)file      ThreadService.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   1.9
 * @(#)lastedit  07/03/08
 * @(#)build     @BUILD_TAG_PLACEHOLDER@
 *
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * The contents of this file are subject to the terms of either the GNU General
 * Public License Version 2 only ("GPL") or the Common Development and
 * Distribution License("CDDL")(collectively, the "License"). You may not use
 * this file except in compliance with the License. You can obtain a copy of the
 * License at http://opendmk.dev.java.net/legal_notices/licenses.txt or in the
 * LEGAL_NOTICES folder that accompanied this code. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file found at
 *     http://opendmk.dev.java.net/legal_notices/licenses.txt
 * or in the LEGAL_NOTICES folder that accompanied this code.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.
 *
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 *
 *       "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding
 *
 *       "[Contributor] elects to include this software in this distribution
 *        under the [CDDL or GPL Version 2] license."
 *
 * If you don't indicate a single choice of license, a recipient has the option
 * to distribute your version of this file under either the CDDL or the GPL
 * Version 2, or to extend the choice of license to its licensees as provided
 * above. However, if you add GPL Version 2 code and therefore, elected the
 * GPL Version 2 license, then the option applies only if the new code is made
 * subject to such option by the copyright holder.
 *
 */

package com.sun.jmx.remote.opt.util;

import java.security.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A generic thread service wrapping an underlying {@link ExecutorService}.
 */
public class ThreadService {
  /**
   * A global, shared thread service.
   */
  private static final ThreadService shared = new ThreadService(0, Integer.MAX_VALUE);
  /**
   * 
   */
  private static final int LEAVING_WAITING_TIME = 1000;
  /**
   * 
   */
  private static final ClassLogger logger = new ClassLogger("com.sun.jmx.remote.opt.util", "ThreadService");
  /**
   * The default priority of the threads.
   */
  private int defaultPriority;
  /**
   * The default thread context class loader.
   */
  private ClassLoader defaultLoader;
  /**
   * The pool of threads used for submitting execution requests.
   */
  protected ExecutorService executor = null;
  /**
   * How long before an idle thread is terminated.
   */
  private long waitingTime = 5000L;

  /**
   * Initialize this thread service.
   * @param min the minimum (core) size of the thread pool.
   * @param max the maximum size of the thread pool.
   */
  public ThreadService(final int min, final int max) {
    if (min < 0) throw new IllegalArgumentException("Negative minimal thread number.");
    if (max < min) throw new IllegalArgumentException("Maximum number less than minimal number.");
    defaultPriority = Thread.currentThread().getPriority();
    defaultLoader = getContextClassLoader();
    BlockingQueue<Runnable> queue = new SynchronousQueue<>();
    executor = new ThreadPoolExecutor(min, max, waitingTime, TimeUnit.MILLISECONDS, queue, new CustomThreadFactory("JobExecutor"));
  }

  /**
   * Submit a job.
   * @param job the job to submit.
   */
  public void handoff(final Runnable job) {
    try {
      isTerminated();
      if (job == null) throw new IllegalArgumentException("Null job.");
      executor.submit(job);
    } catch(RuntimeException | Error e) {
      if (logger.traceOn()) logger.trace("handoff", "got exception: ", e);
      throw e;
    }
  }

  /**
   * Terminate this thread service.
   */
  public void terminate() {
    executor.shutdownNow();
  }

  /**
   * Get the glbal shared thread service.
   * @return an instance of {@link ThreadService}.
   */
  public static ThreadService getShared() {
    return shared;
  }

  /**
   * Check whether this thread service is terminated.
   * @throws IllegalStateException if this thread service is terminated.
   */
  private void isTerminated() throws IllegalStateException {
    if (executor.isTerminated() || executor.isShutdown()) {
      throw new IllegalStateException("The Thread Service has been terminated.");
    }
  }

  /**
   * Get the thread context class loader in a privileged action.
   * @return the thread context class loader set on the thread.
   */
  private ClassLoader getContextClassLoader() {
    return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
      @Override
      public ClassLoader run() {
        return Thread.currentThread().getContextClassLoader();
      }
    });
  }

  /**
   * Set the thread context class loader in a privileged action.
   * @param currentThread the thread to set the class loader on.
   * @param classloader the thread context class loader to set.
   */
  private void setContextClassLoader(final Thread currentThread, final ClassLoader classloader) {
    AccessController.doPrivileged(new PrivilegedAction<Object>() {
      @Override
      public Object run() {
        currentThread.setContextClassLoader(classloader);
        return null;
      }
    });
  }

  /**
   * The thread fctory used by the thread srvice to create nex Threads.
   */
  public class CustomThreadFactory implements ThreadFactory {
    /**
     * The name used as prefix for the constructed threads name.
     */
    private String name = null;
    /**
     * Count of created threads.
     */
    private AtomicInteger count = new AtomicInteger(0);
    /**
     * The thread group that contains the threads of this factory.
     */
    private ThreadGroup threadGroup = null;
    /**
     *
     */
    private final ExceptionHandler defaultExceptionHandler = new ExceptionHandler();
    /**
     * Indicates whether new thread should be created in PrivilegedAction.
     */
    private final boolean doPrivileged;
    /**
     * Indicates whether new thread should be created as daemon threads.
     */
    private final boolean daemon;

    /**
     * Initialize this thread factory with the specified name.
     * @param name the name used as prefix for the constructed threads name.
     */
    public CustomThreadFactory(final String name) {
      this(name, false, true);
    }

    /**
     * Initialize this thread factory with the specified name.
     * @param name the name used as prefix for the constructed threads name.
     * @param doPrivileged indicates whether thread should be created in PrivilegedAction.
     * @param daemon whether created threads are daemon threads.
     */
    public CustomThreadFactory(final String name, final boolean doPrivileged, final boolean daemon) {
      this.name = name == null ? "CustomThreadFactory" : name;
      threadGroup = new ThreadGroup(this.name + " thread group");
      this.doPrivileged = doPrivileged;
      this.daemon = daemon;
    }

    @Override
    public synchronized Thread newThread(final Runnable r) {
      Thread thread;
      final String threadName = name + '_' + String.format("%04d", + count.incrementAndGet());
      if(doPrivileged) {
        thread = AccessController.doPrivileged(new PrivilegedAction<Thread>() {
          @Override
          public Thread run() {
            return new Thread(threadGroup, r, threadName);
          }
        });
      } else thread = new Thread(threadGroup, r, threadName);
      final Thread t = thread;
      AccessController.doPrivileged(new PrivilegedAction<Object>() {
        @Override
        public Object run() {
          t.setContextClassLoader(defaultLoader);
          return null;
        }
      });
      thread.setPriority(defaultPriority);
      thread.setUncaughtExceptionHandler(defaultExceptionHandler);
      thread.setDaemon(daemon);
      return thread;
    }

    /**
     * Default uncaught exception handler set onto the threeads created by the thread factory.
     */
    private class ExceptionHandler implements Thread.UncaughtExceptionHandler {
      @Override
      public void uncaughtException(final Thread t, final Throwable e) {
        System.err.println("exception caught from thread " + t + " :");
        e.printStackTrace();
      }
    }
  }
}
