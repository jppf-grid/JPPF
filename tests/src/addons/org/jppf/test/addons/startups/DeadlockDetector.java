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

package org.jppf.test.addons.startups;

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
   * @param type the type of JPPF component, either "driver" or "node".
   */
  public static void setup(final String type) {
    synchronized(DeadlockDetector.class) {
      if (alreadyRun) return;
      alreadyRun = true;
    }
    System.out.println("setting up " + type + " deadlock detector");
    try {
      @SuppressWarnings("resource")
      final JMXConnectionWrapper jmx = "driver".equals(type) ? new JMXDriverConnectionWrapper() : new JMXNodeConnectionWrapper();
      jmx.connect();
      final DiagnosticsMBean diag = jmx.getDiagnosticsProxy();
      final TimerTask task = new TimerTask() {
        @Override
        public void run() {
          try {
            if (deadlockDetected) cancel();
            else if (diag.hasDeadlock()) {
              deadlockDetected = true;
              final String title =  "thread dump for " + type + " " + jmx;
              final String text = TextThreadDumpWriter.printToString(diag.threadDump(), title);
              log.error("deadlock detected !!!\n{}", text);
              System.err.println("deadlock detected !!!\n" + text);
              cancel();
            }
          } catch (final Exception e) {
            log.error(e.getMessage(), e);
          }
        }
      };
      new Timer("DeadlockChecker", true).schedule(task, 1000L, 2000L);
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
  }
}
