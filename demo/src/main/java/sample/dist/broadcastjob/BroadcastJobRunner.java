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
package sample.dist.broadcastjob;

import java.util.List;

import org.jppf.client.*;
import org.jppf.node.protocol.*;

/**
 * Runner class for the &quot;Nroadcast Job&quot; demo.
 * @author Laurent Cohen
 */
public class BroadcastJobRunner {
  /**
   * Entry point for this class, submits the tasks with a set duration to the server.
   * @param args not used.
   */
  public static void main(final String...args) {
    JPPFClient jppfClient = null;
    try {
      jppfClient = new JPPFClient();
      //while (!jppfClient.hasAvailableConnection()) Thread.sleep(10L);
      final int nbTasks = 1;
      System.out.println("Running Broadcast Job with " + nbTasks + " tasks");
      final long start = System.nanoTime();
      final JPPFJob job = new JPPFJob("broadcast test");
      for (int i=1; i<=nbTasks; i++) job.add(new BroadcastTask()).setId("task " + i);
      job.getSLA().setBroadcastJob(true);
      final List<Task<?>> results = jppfClient.submit(job);
      for (final Task<?> task: results) {
        final Throwable e = task.getThrowable();
        if (e != null) throw e;
      }
      final long elapsed = (System.nanoTime() - start) / 1_000_000L;
      System.out.println("Total time: " + elapsed + " ms");
    } catch(final Throwable e) {
      e.printStackTrace();
    } finally {
      if (jppfClient != null) jppfClient.close();
    }
  }

  /**
   * 
   */
  private static class BroadcastTask extends AbstractTask<String> {
    @Override
    public void run() {
      System.out.println("broadcast task " + getId());
    }
  }
}
