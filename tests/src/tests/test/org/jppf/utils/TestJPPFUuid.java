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

package test.org.jppf.utils;

import static junit.framework.Assert.*;

import java.util.*;
import java.util.concurrent.*;

import org.jppf.utils.JPPFUuid;
import org.junit.Test;

/**
 * Unit tests for the <code>JPPFUuid</code> class.
 * @author Laurent Cohen
 */
public class TestJPPFUuid {
  /**
   * Test that JPPFuuid does not generate uuid collisions in multithreaded execution.<br/>
   * See bug <a href="http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-207">JPPF-207 JPPFUuid generates uuid collisions in multithreaded mode</a>
   * @throws Exception if any error occurs
   */
  @Test(timeout=5000)
  public void testUuidCollisions() throws Exception {
    int nbThreads = Runtime.getRuntime().availableProcessors();
    if (nbThreads < 2) nbThreads = 2;
    int nbTasks = 1000;
    ThreadPoolExecutor executor = null;
    try {
      executor = new ThreadPoolExecutor(nbThreads, nbThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
      executor.prestartAllCoreThreads();
      for (int i=0; i<10; i++) performExecution(nbTasks, executor);
    } finally {
      if (executor != null) executor.shutdownNow();
    }
  }

  /**
   * Test that JPPFuuid does not generate uuid collisions in multithreaded execution.
   * @param nbTasks number of uuid generations to submit.
   * @param executor the multithreaded executor to submit the tasks to.
   * @throws Exception if any error occurs
   */
  public void performExecution(final int nbTasks, final ExecutorService executor) throws Exception {
    List<Future<String>> futures = new ArrayList<>(nbTasks);
    Map<String, Boolean> map = new ConcurrentHashMap<>(nbTasks);
    for (int i=0; i<nbTasks; i++) futures.add(executor.submit(new UuidTask()));
    int count = 0;
    for (Future<String> future: futures) {
      String uuid = future.get();
      Boolean prevValue = map.put(uuid, Boolean.TRUE);
      if (prevValue != null) {
        System.out.println("uuid collision for " + uuid);
        count++;
      }
    }
    assertEquals("found " + count  + " collisions", 0, count);
  }

  /** */
  private static class UuidTask implements Callable<String> {
    @Override
    public String call() throws Exception {
      return JPPFUuid.normalUUID();
    }
  }
}
