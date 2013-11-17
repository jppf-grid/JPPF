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
package test.p2p;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.client.*;
import org.jppf.node.protocol.Task;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;
import org.jppf.utils.collections.*;

/**
 * This is a template JPPF application runner.
 * It is fully commented and is designed to be used as a starting point
 * to write an application using JPPF.
 * @author Laurent Cohen
 */
public class P2PRunner {
  /**
   * The JPPF client, handles all communications with the server.
   * It is recommended to only use one JPPF client per JVM, so it
   * should generally be created and used as a singleton.
   */
  private static JPPFClient jppfClient =  null;
  /**
   * 
   */
  private static AtomicInteger jobSeq = new AtomicInteger(0);

  /**
   * The entry point for this application runner to be run from a Java command line.
   * @param args by default, we do not use the command line arguments,
   * however nothing prevents us from using them if need be.
   * @throws Exception if any error occurs
   */
  public static void main(final String[] args) throws Exception {
    try {
      jppfClient = new JPPFClient();
      while (!jppfClient.hasAvailableConnection()) Thread.sleep(1L);
      long time = System.nanoTime();
      JPPFJob job = createJob(10, true, 100L);
      List<JPPFTask> results = jppfClient.submit(job);
      CollectionMap<String, Task<?>> map = new ArrayListHashMap<String, Task<?>>();
      for (Task<?> t: results) {
        P2PTask task = (P2PTask) t;
        map.putValue(task.getNodeUuid(), task);
        Exception e = t.getException();
        if (e != null) throw e;
      }
      assert map.keySet().size() == 2;
      for (Map.Entry<String, Collection<Task<?>>> entry: map.entrySet()) {
        assert entry.getValue().size() == 5;
      }
      System.out.println("execution time = " + StringUtils.toStringDuration((System.nanoTime() - time)/1000000L));
    } finally {
      if ((jppfClient != null) && !jppfClient.isClosed()) jppfClient.close();
    }
  }

  /**
   * Create a job.
   * @param nbTasks number of tasks in the job.
   * @param blocking true if job is blocking, false otherwise.
   * @param duration the duration of each task in the job.
   * @return a JPPFJob instance.
   * @throws Exception if any error occurs.
   */
  private static JPPFJob createJob(final int nbTasks, final boolean blocking, final long duration) throws Exception
  {
    JPPFJob job = new JPPFJob();
    job.setName(P2PRunner.class.getSimpleName() + '-' + jobSeq.incrementAndGet());
    job.setBlocking(blocking);
    for (int i=1; i<=nbTasks; i++) job.addTask(new P2PTask(duration));
    JPPFResultCollector collector = new JPPFResultCollector(job);
    job.setResultListener(collector);
    return job;
  }
}
