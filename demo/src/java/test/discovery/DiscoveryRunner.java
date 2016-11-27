/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package test.discovery;

import static org.jppf.utils.configuration.JPPFProperties.*;

import org.jppf.client.*;
import org.jppf.utils.JPPFConfiguration;

import sample.dist.tasklength.LongTask;

/**
 * @author Laurent Cohen
 */
public class DiscoveryRunner {
  /**
   * @param args not used.
   */
  public static void main(final String[] args) {
    // disable built-in discovery
    JPPFConfiguration.set(REMOTE_EXECUTION_ENABLED, false).set(LOCAL_EXECUTION_ENABLED, false);
    try (JPPFClient client = new JPPFClient()) {
      CustomDiscovery discovery = new CustomDiscovery();
      client.addDriverDiscovery(discovery);
      for (int i=1; i<=20; i++) {
        JPPFJob job = new JPPFJob();
        job.setName("discovery " + i);
        for (int j=1; j<=10; j++) job.add(new LongTask(100L)).setId(job.getName() + " - task " + j);
        // add tasks
        client.submitJob(job);
        System.out.println("got results for job " + job.getName());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
