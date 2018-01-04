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

package sample.test.deadlock;

import org.jppf.client.JPPFClient;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.utils.concurrent.ThreadSynchronization;
import org.jppf.utils.stats.*;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class MasterNodeMonitoringThread extends ThreadSynchronization implements Runnable {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(MasterNodeMonitoringThread.class);
  /**
   *
   */
  private final JPPFClient client;
  /**
   *
   */
  private final long waitTime;
  /**
   * 
   */
  private final ProvisioningThread provisioner;
  /**
   *
   */
  private int nbTasks = 0;

  /**
   *
   * @param client the JPPF client.
   * @param waitTime .
   * @param provisioner .
   */
  public MasterNodeMonitoringThread(final JPPFClient client, final long waitTime, final ProvisioningThread provisioner) {
    this.client = client;
    this.waitTime = waitTime;
    this.provisioner = provisioner;
  }

  @Override
  public void run() {
    log.info("starting MasterNodeMonitoringThread, waitTime={}", waitTime);
    JMXDriverConnectionWrapper jmx = null;
    while (!isStopped()) {
      if (jmx == null) {
        try {
          jmx = DeadlockRunner.getJmxConnection(client);
        } catch (final Exception e) {
          e.printStackTrace();
          return;
        }
        log.info("got jmx connection");
      }
      if (isStopped()) break;
      goToSleep(waitTime);
      if (isStopped()) break;
      try {
        final JPPFStatistics stats = jmx.statistics();
        final int n = (int) stats.getSnapshot(JPPFStatisticsHelper.TASK_DISPATCH).getTotal();
        if ((nbTasks == n) && !isStopped()) {
          System.out.println("EPIC FAIL !!!!!!!");
          provisioner.setStopped(true);
          provisioner.wakeUp();
          Thread.sleep(1000L);
          //System.exit(1);
          return;
        }
        nbTasks = n;
      } catch(final Exception e) {
        e.printStackTrace();
        System.exit(1);
        return;
      }
    }
  }
}
