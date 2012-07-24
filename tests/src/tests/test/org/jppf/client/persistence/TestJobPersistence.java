/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

package test.org.jppf.client.persistence;

import static org.junit.Assert.*;

import java.util.*;

import org.jppf.client.*;
import org.jppf.client.persistence.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;
import org.junit.Test;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.SimpleTask;

/**
 * Test for the <code>JobPersistence</code> interface.
 * @author Laurent Cohen
 */
public class TestJobPersistence extends Setup1D1N
{
  /**
   * Test the recovery of a job by closing the JPPF client before it completes.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=15000)
  public void testJobRecovery() throws Exception
  {
    String key = null;
    JobPersistence<String> pm = null;
    TypedProperties config = JPPFConfiguration.getProperties();
    long duration = 750L;
    JPPFClient client = null;
    try
    {
      // send tasks 1 at a time
      config.setProperty("jppf.load.balancing.algorithm", "manual");
      config.setProperty("jppf.load.balancing.strategy", "test");
      config.setProperty("strategy.test.size", "1");
      client = BaseSetup.createClient(null, false);
      int nbTasks = 3;
      JPPFJob job = BaseSetup.createJob("TestSubmit", false, false, nbTasks, SimpleTask.class, duration);
      int i=0;
      for (JPPFTask task: job.getTasks()) task.setId("" + ++i);
      pm = new DefaultFilePersistenceManager("root", "job_", ".ser");
      key = pm.computeKey(job);
      assertEquals(key, job.getUuid());
      job.setPersistenceManager(pm);
      JPPFResultCollector collector = new JPPFResultCollector(job);
      job.setResultListener(collector);
      client.submit(job);
      Thread.sleep(2*duration);
      client.close();
      int n = job.getResults().size();
      assertTrue(n < nbTasks);

      client = BaseSetup.createClient(null);
      JPPFJob job2 = pm.loadJob(key);
      assertEquals(job2.getUuid(), job.getUuid());
      int n2 = job2.getResults().size();
      assertEquals(n, n2);
      JPPFResultCollector collector2 = new JPPFResultCollector(job2);
      job2.setResultListener(collector2);
      client.submit(job2);
      List<JPPFTask> results = collector2.waitForResults();
      assertEquals(nbTasks, results.size());
      assertEquals(nbTasks, job2.getResults().size());
    }
    finally
    {
      config.remove("jppf.load.balancing.algorithm");
      config.remove("jppf.load.balancing.strategy");
      config.remove("strategy.test.size");
      if ((pm != null) && (key != null)) pm.deleteJob(key);
      if (client != null) client.close();
    }
  }
}
