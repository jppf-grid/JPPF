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

package test.org.jppf.ssl;

import static org.junit.Assert.*;

import java.util.List;

import org.jppf.client.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.junit.*;

import test.org.jppf.test.setup.*;

/**
 * SSL Unit Tests with 1-way authentication.
 * @author Laurent Cohen
 */
public class TestSSL extends AbstractNonStandardSetup {
  /**
   * Launches a driver and 1 node and start the client,
   * all setup with 1-way SSL authentication.
   * @throws Exception if a process could not be started.
   */
  @BeforeClass
  public static void setup() throws Exception {
    print(true, false, "trace 1");
    client = BaseSetup.setup(1, 1, true, false, createConfig("ssl"));
    try {
      print(true, false, "trace 2");
      final List<JPPFConnectionPool> pools = client.awaitConnectionPools(5000L, JPPFClientConnectionStatus.workingStatuses());
      print(true, false, "trace 3");
      assertNotNull(pools);
      assertFalse(pools.isEmpty());
      print(true, false, "trace 4");
      final List<JMXDriverConnectionWrapper> drivers = pools.get(0).awaitJMXConnections(Operator.AT_LEAST, 1, 5000L, true);
      print(true, false, "trace 5");
      assertNotNull(drivers);
      assertFalse(drivers.isEmpty());
      final JMXDriverConnectionWrapper driver = drivers.get(0);
      final long start = System.nanoTime();
      boolean ok = false;
      print(true, false, "trace 6");
      while (((System.nanoTime() - start) / 1_000_000L < 5000L) && !ok) {
        if (driver.nbNodes() == 1) ok = true;
        else Thread.sleep(10L);
      }
      print(true, false, "trace 7, ok = %b", ok);
      assertTrue(ok);
    } catch(final AssertionError e) {
      e.printStackTrace();
      BaseSetup.cleanup();
      throw e;
    }
  }

  /**
   * Test a simple job.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testSimpleJob() throws Exception {
    super.testSimpleJob(null);
  }

  @Override
  @Test(timeout = 15000)
  public void testMultipleJobs() throws Exception {
    super.testMultipleJobs();
  }

  @Override
  @Test(timeout = 10000)
  public void testCancelJob() throws Exception {
    super.testCancelJob();
  }

  @Override
  @Test(timeout = 5000)
  public void testNotSerializableExceptionFromNode() throws Exception {
    super.testNotSerializableExceptionFromNode();
  }

  @Override
  @Test(timeout = 8000)
  public void testForwardingMBean() throws Exception {
    super.testForwardingMBean();
  }
}
