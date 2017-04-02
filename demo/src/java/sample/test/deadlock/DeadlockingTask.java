/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package sample.test.deadlock;

import org.jppf.node.protocol.AbstractTask;

/**
 * This task deliberately triggers a dealock from its {@code run()} method.
 * @author Laurent Cohen
 */
public class DeadlockingTask extends AbstractTask<String> {
  @Override
  public void run() {
    try {
      final Object[] dlo = { new Object(), new Object() };
      Thread[] threads = new Thread[2];
      for (int i=1; i<=2; i++) {
        final int[] n = (i == 1) ? new int[] { 0, 1 } : new int[] { 1, 0};
        threads[i-1] = new Thread("deadlocked thread " + i) {
          @Override
          public void run() {
            synchronized(dlo[n[0]]) {
              dlo[n[0]].toString();
              try {
                Thread.sleep(100L);
              } catch (Exception e) {
                e.printStackTrace();
              }
              synchronized(dlo[n[1]]) {
                dlo[n[1]].toString();
              }
            }
          }
        };
      }
      for (int i=0; i<threads.length; i++) threads[i].start();
      for (int i=0; i<threads.length; i++) threads[i].join();
    } catch(Exception e) {
      setThrowable(e);
      e.printStackTrace();
    }
  }
}
