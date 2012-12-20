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

package test.org.jppf.io;

import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.jppf.client.JPPFJob;
import org.jppf.management.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.server.scheduler.bundle.LoadBalancingInformation;
import org.jppf.task.storage.*;
import org.jppf.utils.*;
import org.junit.Test;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for {@link org.jppf.io.FileDataLocation}.
 * @author Laurent Cohen
 */
public class TestFileDataLocation extends Setup1D1N1C
{
  /**
   * 
   */
  private LoadBalancingInformation oldLbi = null;

  /**
   * Test the execution of a job with a very large footprint, and multiple dispatches from the load-balancer.
   * @throws Exception if any error occurs
   */
  @Test(timeout=20000)
  public void testSubmitLargeDataProvider() throws Exception
  {
    int size = 200 * 1024 * 1024;
    int nbTasks = 3;
    try
    {
      configureLoadBalancing();
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, LifeCycleTask.class, 0L);
      DataProvider dp = new MemoryMapDataProvider();
      dp.setValue("bytes", new byte[size]);
      job.setDataProvider(dp);
      List<JPPFTask> results = client.submit(job);
    }
    finally
    {
      resetLoadBalancing();
    }
  }

  /**
   * Configure the driver's load balancing settings.
   * @throws Exception if any error occurs
   */
  private void configureLoadBalancing() throws Exception
  {
    JMXDriverConnectionWrapper driver = BaseSetup.getDriverManagementProxy(client);
    assertNotNull(driver);
    JPPFSystemInformation info = driver.systemInformation();
    System.out.println("runtime driver info: " + info.getRuntime());
    oldLbi = driver.loadBalancerInformation();
    TypedProperties newConfig = new TypedProperties();
    newConfig.setProperty("size", "1");
    driver.changeLoadBalancerSettings("manual", newConfig);
  }

  /**
   * Reset the driver's load balancing settings.
   * @throws Exception if any error occurs
   */
  private void resetLoadBalancing() throws Exception
  {
    LoadBalancingInformation tmpLbi = oldLbi;
    oldLbi = null;
    JMXDriverConnectionWrapper driver = BaseSetup.getDriverManagementProxy(client);
    if (tmpLbi != null) driver.changeLoadBalancerSettings(tmpLbi.getAlgorithm(), tmpLbi.getParameters());
  }
}
