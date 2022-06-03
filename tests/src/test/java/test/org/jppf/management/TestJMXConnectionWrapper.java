/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package test.org.jppf.management;

import org.jppf.management.JMXNodeConnectionWrapper;
import org.junit.Test;

import test.org.jppf.test.setup.BaseTest;

/**
 * Unit tests for the class <code>JMXConnectionWrapper</code>.
 * @author Laurent Cohen
 */
public class TestJMXConnectionWrapper extends BaseTest {
  /**
   * Testing that connectAndWait() isn't blocking more than the specified timeout.
   * See bug <a href="http://sourceforge.net/tracker/?func=detail&aid=3539051&group_id=135654&atid=733518">3539051 - JMX: performConnection is blocking connectAndWait</a>
   * @throws Exception if any error occurs
   */
  @Test(timeout = 15_000)
  public void testConnectAnWaitNonReachableServer() throws Exception {
    //make sure the host is on an unreachable network
    final JMXNodeConnectionWrapper jmx = new JMXNodeConnectionWrapper("10.1.1.2", 12345, false);
    final long duration = 1400L;
    final int nbThreads = 16;
    final Thread[] threads = new Thread[nbThreads];
    for (int i=0; i<nbThreads; i++) {
      threads[i] = new Thread("TestJmxConnect-" + i) {
        @Override
        public void run() {
          final long start = System.nanoTime();
          jmx.connectAndWait(duration);
          final long elapsed = (System.nanoTime() - start) / 1_000_000L;
          print(false, false, "[%s] connectAndWait() actually waited %,d ms", getName(), elapsed);
        }
      };
    }
    print(false, false, "starting all threads");
    for (int i=0; i<nbThreads; i++) threads[i].start();
    print(false, false, "wiating for all threads to join");
    for (int i=0; i<nbThreads; i++) threads[i].join();
    print(false, false, "closing connection");
    jmx.close();
    print(false, false, "connection closed");
  }
}
