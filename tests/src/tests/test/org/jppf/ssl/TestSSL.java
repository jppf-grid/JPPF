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

package test.org.jppf.ssl;

import static org.junit.Assert.*;

import java.util.List;

import org.apache.log4j.Level;
import org.jppf.client.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.utils.Operator;
import org.jppf.utils.concurrent.ConcurrentUtils;
import org.jppf.utils.concurrent.ConcurrentUtils.ConditionFalseOnException;
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
    ConfigurationHelper.setLoggerLevel(Level.DEBUG, "org.jppf.ssl");
    print("setting up");
    client = BaseSetup.setup(1, 1, true, false, createConfig("ssl"));
    try {
      print("awaiting at least 1 working connection pool");
      final List<JPPFConnectionPool> pools = client.awaitConnectionPools(5000L, JPPFClientConnectionStatus.workingStatuses());
      assertNotNull(pools);
      assertFalse(pools.isEmpty());
      print("awaiting at least 1 working jmx connection");
      final List<JMXDriverConnectionWrapper> drivers = pools.get(0).awaitJMXConnections(Operator.AT_LEAST, 1, 5000L, true);
      assertNotNull(drivers);
      assertFalse(drivers.isEmpty());
      final JMXDriverConnectionWrapper driver = drivers.get(0);
      print("awaiting 1 connected node");
      assertTrue(ConcurrentUtils.awaitCondition(5000L, 100L, true, (ConditionFalseOnException) () -> driver.nbNodes() == 1));
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
