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

package test.org.jppf.client.event;

import static org.junit.Assert.*;

import java.util.List;

import org.jppf.client.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for <code>JobListener</code> using multiple connections to the same server
 * (connection pool size > 1).
 * @author Laurent Cohen
 */
public class TestJPPFResultCollector extends Setup1D1N {
  /**
   * The JPPF client.
   */
  private JPPFClient client = null;

  /**
   * submit the job with the specified listener and number of tasks.
   * @param name the name of the job to run.
   * @param listener the listener to use for the test.
   * @param nbTasks the number of tasks
   * @return the execution results.
   * @throws Exception if any error occurs
   */
  public List<Task<?>> runJob(final String name, final CountingJobListener listener, final int nbTasks) throws Exception {
    client = BaseSetup.createClient(null, false);
    JPPFJob job = BaseTestHelper.createJob(name, true, false, nbTasks, LifeCycleTask.class, 0L);
    if (listener != null) job.addJobListener(listener);
    List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(nbTasks, results.size());
    //Thread.sleep(250L);
    return results;
  }

  /**
   * Configure the client for a connection pool.
   * @param remoteEnabled specifies whether remote execution is enabled.
   * @param localEnabled specifies whether local execution is enabled.
   * @param poolSize the size of the connection pool.
   */
  private void configure(final boolean remoteEnabled, final boolean localEnabled, final int poolSize) {
    TypedProperties config = JPPFConfiguration.getProperties();
    config.setBoolean("jppf.remote.execution.enabled", remoteEnabled);
    config.setBoolean("jppf.local.execution.enabled", localEnabled);
    config.setInt("jppf.local.execution.threads", 4);
    config.setProperty("jppf.load.balancing.algorithm", "manual");
    config.setProperty("jppf.load.balancing.profile", "manual");
    config.setInt("jppf.load.balancing.profile.manual.size", 5);
    config.setInt("jppf.pool.size", poolSize);
  }

  /**
   * Reset the confiugration.
   */
  private void reset() {
    if (client != null) {
      client.close();
      client = null;
    }
    JPPFConfiguration.reset();
  }
}
