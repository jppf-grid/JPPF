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

import org.jppf.management.JMXNodeConnectionWrapper;
import org.jppf.management.diagnostics.*;
import org.jppf.utils.*;
import org.junit.Test;

import test.org.jppf.test.setup.*;

/**
 * Test a multi-server topology with 2 servers, 1 node attached to each server and 1 client.
 * @author Laurent Cohen
 */
public class TestMultiServerSetup extends AbstractNonStandardSetup {
  /** */
  private static final int TIMEOUT = 15_000;

  /**
   * Test a simple job.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = TIMEOUT)
  public void testSetup() throws Exception {
    try {
      final TestConfiguration cfg = createConfig("p2p");
      cfg.driverLog4j = "classes/tests/config/p2p/log4j-driver.properties";
      cfg.nodeLog4j = "classes/tests/config/p2p/log4j-node.properties";
      print(false, false, ">>> setting up");
      TestConfigSource.setClientConfig(cfg.clientConfig);
      BaseSetup.setup(2, 2, false, false, cfg);
      checkPeers(TIMEOUT, false);
      print(false, false, ">>> initialization complete");
    } finally {
      for (int i=1; i<=BaseSetup.nbNodes(); i++) {
        JMXNodeConnectionWrapper nodeJmx = null;
        try (JMXNodeConnectionWrapper jmx = new JMXNodeConnectionWrapper("localhost", NODE_MANAGEMENT_PORT_BASE + i, false)) {
          nodeJmx = jmx;
          jmx.connectAndWait(3000L);
          final DiagnosticsMBean proxy = jmx.getDiagnosticsProxy();
          final String text = TextThreadDumpWriter.printToString(proxy.threadDump(), "node thread dump for " + jmx);
          FileUtils.writeTextFile("node_thread_dump_" + jmx.getPort() + ".log", text);
        } catch (final Exception e) {
          print(false, false, ">>> error generating thread dump for %s:%n%s", nodeJmx, ExceptionUtils.getStackTrace(e));
        }
      }
    }
  }
}
