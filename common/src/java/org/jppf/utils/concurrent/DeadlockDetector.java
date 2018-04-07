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

import java.io.*;
import java.lang.management.*;
import java.util.*;

import org.jppf.management.*;
import org.jppf.management.diagnostics.*;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class DeadlockDetector {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(DeadlockDetector.class);
  /**
   * Whether this startup has already run once.
   */
  private static boolean alreadyRun = false;
  /**
   * Whether a deadlock has already been detected.
   */
  private static boolean deadlockDetected = false;
  /**
   * 
   */
  private static Timer timer;
  /**
   * 
   */
  private static DiagnosticsMBean diag;

  /**
   * 
   * @param type the type of JPPF component, either "driver" or "node".
   */
  public synchronized static void setup(final String type) {
    if (alreadyRun) return;
    alreadyRun = true;
    System.out.println("setting up " + type + " deadlock detector");
    try {
      final String suffix;
      if ("client".equals(type)) {
        suffix = type;
        diag = new Diagnostics("client");
      } else {
        @SuppressWarnings("resource")
        final JMXConnectionWrapper jmx = "driver".equals(type) ? new JMXDriverConnectionWrapper() : new JMXNodeConnectionWrapper();
        jmx.setReconnectOnError(false);
        jmx.connect();
        diag = jmx.getDiagnosticsProxy();
        suffix = jmx.toString();
      }
      timer = new Timer("DeadlockChecker", true);
      final TimerTask task = new TimerTask() {
        @Override
        public void run() {
          try {
            if (deadlockDetected) cancel();
            else if (diag.hasDeadlock()) {
              deadlockDetected = true;
              final String title =  "client".equals(type) ? "thread dump for local JVM" : "thread dump for " + type + " " + suffix;
              final String text = TextThreadDumpWriter.printToString(diag.threadDump(), title);
              log.error("deadlock detected !!!\n{}", text);
              System.err.println("deadlock detected !!!\n" + text);
              cancel();
            }
          } catch (final Exception e) {
            log.error(e.getMessage(), e);
            cancel();
            reset();
          }
        }
      };
      timer.schedule(task, 1000L, 2000L);
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * 
   */
  private synchronized static void reset() {
    if (timer != null) {
      timer.cancel();
      timer.purge();
      timer = null;
    }
    diag = null;
    alreadyRun = false;
  }

  /**
   * Get information on the thread that owns the monitor of the specified object.
   * @param object the object for which to lookup an owning thread.
   * @return a {@link ThreadInfo} object, or {@code null} if no thread own the object's monitor.
   */
  public static ThreadInfo getMonitorOwner(final Object object) {
    final ThreadInfo[] allThreads = ManagementFactory.getThreadMXBean().dumpAllThreads(true, false);
    final int idHash = System.identityHashCode(object);
    for (final ThreadInfo ti: allThreads) {
      final MonitorInfo[] monitors = ti.getLockedMonitors();
      for (final MonitorInfo monitor: monitors) {
        if (monitor.getIdentityHashCode() == idHash) return ti;
      }
    }
    return null;
  }

  /**
   * 
   * @param info .
   * @return .
   */
  public static String printThreadInfo(final ThreadInfo info) {
    final StringWriter sw = new StringWriter();
    try (ThreadDumpWriter tw = new TextThreadDumpWriter(sw, "PrintThreadInfo")) { 
      tw.printThread(new ThreadInformation(info));
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
    return sw.toString();
  }
}
