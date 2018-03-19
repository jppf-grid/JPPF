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

package test.org.jppf.server.peer;

import org.jppf.node.policy.Equal;
import org.junit.*;

import test.org.jppf.test.setup.*;

/**
 * Test a multi-server topology with 2 servers, 1 node attached to each server and 1 client,
 * all setup with SSL 2-way authentication.
 * @author Laurent Cohen
 */
//@Ignore
public class TestMultiServerWithSSL extends AbstractNonStandardSetup {
  /**
   * Launches a 2 drivers with 1 node attached to each and start the client,
   * all setup with 2-way SSL authentication.
   * @throws Exception if a process could not be started.
   */
  @BeforeClass
  public static void setup() throws Exception {
    client = BaseSetup.setup(2, 2, true, false, createConfig("ssl2_p2p"));
  }

  /**
   * Wait until each driver has 1 idle node.
   * @throws Exception if any error occurs.
   */
  @Before
  public void instanceSetup() throws Exception {
    awaitPeersInitialized(15_000L);
  }

  /**
   * Test a simple job.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testSimpleJob() throws Exception {
    super.testSimpleJob(new Equal("jppf.ssl.server.port", 12101));
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
}
