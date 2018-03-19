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

import org.junit.Test;

import test.org.jppf.test.setup.*;

/**
 * Test a multi-server topology with 2 servers, 1 node attached to each server and 1 client,
 * all setup with SSL 2-way authentication.
 * @author Laurent Cohen
 */
//@Ignore
public class TestMultiServerWithSSLSetup extends AbstractNonStandardSetup {
  /**
   * Test a simple job.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 15000)
  public void testSetup() throws Exception {
    final TestConfiguration cfg = createConfig("ssl2_p2p");
    cfg.driverLog4j = "classes/tests/config/ssl2_p2p/log4j-driver.template.properties";
    cfg.nodeLog4j = "classes/tests/config/ssl2_p2p/log4j-node.template.properties";
    print(false, false, ">>> setting up");
    TestConfigSource.setClientConfig(cfg.clientConfig);
    BaseSetup.setup(2, 2, false, false, cfg);
    //SSLHelper.resetConfig();
    //JPPFConfiguration.reset();
    //print(false, false, ">>> setup complete, awaiting peers initialized; configuration:%n%s", JPPFConfiguration.getProperties().asString());
    //awaitPeersInitialized(15_000L);
    checkPeers(15_000L, true);
    print(false, false, ">>> initialization complete");
  }
}
