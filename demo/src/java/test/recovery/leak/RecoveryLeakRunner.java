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

package test.recovery.leak;

import org.jppf.management.JMXDriverConnectionWrapper;


/**
 * 
 * @author Laurent Cohen
 */
public class RecoveryLeakRunner {
  /**
   * Entry point into the test.
   * @param args not used.
   */
  public static void main(final String...args) {
    try {
      for (int i=1; i<=500; i++) {
        if (i > 1) Thread.sleep(4000L);
        System.out.println("iteration " + i);
        JMXDriverConnectionWrapper jmx = new JMXDriverConnectionWrapper("127.0.0.1", 11191, false);
        jmx.connect();
        while (!jmx.isConnected()) Thread.sleep(10L);
        jmx.restartShutdown(10L, 10L);
        try {
          jmx.close();
        } catch (Exception ignore) {
        }
        System.out.println("iteration " + i + " done");
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
}
