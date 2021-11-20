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

package test.org.jppf.utils;

import static org.junit.Assert.assertEquals;

import java.util.*;
import java.util.concurrent.Callable;

import org.jppf.utils.JPPFUuid;
import org.junit.Test;

import test.org.jppf.test.setup.BaseTest;

/**
 * Unit tests for the <code>JPPFUuid</code> class.
 * @author Laurent Cohen
 */
public class TestJPPFUuid extends BaseTest {
  /**
   * Test that JPPFuuid does not generate uuid collisions in multithreaded execution.<br/>
   * See bug <a href="http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-207">JPPF-207 JPPFUuid generates uuid collisions in multithreaded mode</a>
   * @throws Exception if any error occurs
   */
  @Test(timeout=20_000L)
  public void testUuidCollisions() throws Exception {
    int nbThreads = Runtime.getRuntime().availableProcessors();
    if (nbThreads < 2) nbThreads = 2;
    testCollisions(nbThreads, 100_000, () -> JPPFUuid.normalUUID());
  }

  /**
   * Test that JPPFuuid does not generate uuid collisions in multithreaded execution.
   * @param nbThreads numbe of threads to run.
   * @param nbTasks number of tasks per thread.
   * @param callable the callable that generates a uuid.
   * @throws Exception if any error occurs
   */
  private void testCollisions(final int nbThreads, final int nbTasks, final MyCallable callable) throws Exception {
    print(false, false, "executing %,d tasks per thread on %d threads", nbTasks, nbThreads);
    final MyThread[] threads = new MyThread[nbThreads];
    for (int i=0; i<nbThreads; i++) threads[i] = new MyThread(nbTasks, callable);
    for (int i=0; i<nbThreads; i++) threads[i].start();
    for (int i=0; i<nbThreads; i++) threads[i].join();
    final Map<String, Boolean> allUuids = new HashMap<>(nbThreads * nbTasks);
    int count = 0;
    for (final MyThread thread: threads) {
      for (final String uuid: thread.uuids) {
        if (allUuids.containsKey(uuid)) {
          print(false, false, "uuid collision for %s", uuid);
          count++;
        } else allUuids.put(uuid, Boolean.TRUE);
      }
    }
    assertEquals(String.format("found %,d collisions out of %,d generated uuids", count, nbThreads * nbTasks), 0, count);
  }

  /** */
  class MyThread extends Thread {
    /** */
    final int nbTasks;
    /** */
    final MyCallable callable;
    /** */
    final List<String> uuids;

    /**
     * @param nbTasks number of tasks per thread.
     * @param callable the callable that generates a uuid.
     */
    MyThread(final int nbTasks, final MyCallable callable) {
      this.nbTasks = nbTasks;
      this.callable = callable;
      this.uuids = new ArrayList<>(nbTasks);
    }

    @Override
    public void run() {
      for (int i=0; i<nbTasks; i++) uuids.add(callable.call());
    }
  }

  /** */
  interface MyCallable extends Callable<String> {
    @Override
    String call();
  }
}
