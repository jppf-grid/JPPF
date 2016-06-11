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

import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.client.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.utils.stats.*;

/**
 * 
 */
class MyCallback extends JobStreamingCallback.Adapter {
  /**
   * 
   */
  private final JPPFClient client;
  /**
   * 
   */
  private final AtomicBoolean done = new AtomicBoolean(false);

  /**
   * 
   * @param client .
   */
  public MyCallback(final JPPFClient client) {
    this.client = client;
  }

  @Override
  public void jobCreated(final JPPFJob job) {
    //job.getSLA().setBroadcastJob(true);
    //job.getSLA().setCancelUponClientDisconnect(false);
  }

  @Override
  public void jobCompleted(final JPPFJob job, final JobStreamImpl jobStream) {
    if (jobStream.getExecutedJobCount() <= 200) return;
    if (done.compareAndSet(false, true)) {
      DeadlockRunner.printf("reached over 200 jobs");
      try {
        JMXDriverConnectionWrapper jmx = DeadlockRunner.getJmxConnection(client);
        JMXDriverConnectionWrapper tmp = new JMXDriverConnectionWrapper(jmx.getHost(), jmx.getPort(), jmx.isSecure());
        tmp.connect();
        jobStream.setStopped(true);
        client.close();
        while (!tmp.isConnected()) Thread.sleep(1L);
        Thread.sleep(500L);
        JPPFStatistics stats = tmp.statistics();
        tmp.close();
        JPPFSnapshot taskCount = stats.getSnapshot(JPPFStatisticsHelper.TASK_QUEUE_COUNT);
        JPPFSnapshot clientCount = stats.getSnapshot(JPPFStatisticsHelper.CLIENTS);
        //printf("statistics after client close:%n%s", stats);
        DeadlockRunner.printf("%s latest: %,d", taskCount.getLabel(), (long) taskCount.getLatest());
        DeadlockRunner.printf("%s latest: %,d", clientCount.getLabel(), (long) clientCount.getLatest());
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        System.exit(0);
      }
    }
  }
}