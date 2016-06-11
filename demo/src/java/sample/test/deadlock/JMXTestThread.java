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

import java.util.ConcurrentModificationException;

import org.jppf.client.JPPFClient;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.jppf.utils.ThreadSynchronization;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class JMXTestThread extends ThreadSynchronization implements Runnable {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JMXTestThread.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * 
   */
  private final JPPFClient client;
  /**
   * 
   */
  private final int index;

  /**
   * Initialize with the specified JPPF client.
   * @param client the JPPF client to use.
   * @param index an int index.
   */
  public JMXTestThread(final JPPFClient client, final int index) {
    this.client = client;
    this.index = index;
  }

  @Override
  public void run() {
    try {
      JMXDriverConnectionWrapper jmx = client.awaitWorkingConnectionPool().getJMXConnections().get(index);
      DriverJobManagementMBean jobManager = jmx.getJobManager();
      while (!isStopped()) {
        try {
        String[] uuids = jobManager.getAllJobUuids();
        } catch(ConcurrentModificationException e) {
          log.error(e.getMessage(), e);
        }
        goToSleep(1L);
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }
}
