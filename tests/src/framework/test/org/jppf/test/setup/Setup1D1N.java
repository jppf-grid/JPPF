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

package test.org.jppf.test.setup;

import org.jppf.JPPFException;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.utils.StringUtils;
import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;


/**
 * Basic setup for 1 driver, 1 node and no client.
 * @author Laurent Cohen
 */
public class Setup1D1N extends BaseTest {
  /**
   * 
   */
  private static JMXDriverConnectionWrapper driverJmx;
  /** */
  @Rule
  public TestWatcher testDriverDiscoveryInstanceWatcher = new TestWatcher() {
    @Override
    protected void starting(final Description description) {
      try {
        String msg = String.format( "***** start of method %s() *****", description.getMethodName());
        String banner = StringUtils.padLeft("", '*', msg.length(), false);
        logInServer(driverJmx, banner, msg, banner);
      } catch(Exception e) {
        e.printStackTrace();
      }
    }
  };

  /**
   * Launches a driver and node.
   * @throws Exception if a process could not be started.
   */
  @BeforeClass
  public static void setup() throws Exception {
    BaseSetup.setup(1, 1, false, BaseSetup.DEFAULT_CONFIG);
    driverJmx = new JMXDriverConnectionWrapper("localhost", 11201);
    driverJmx.connectAndWait(5000L);
    Assert.assertTrue(driverJmx.isConnected());
    long elapsed;
    long start = System.nanoTime();
    while (((elapsed = (System.nanoTime() - start) / 1_000_000L) < 5000L) && (driverJmx.nbNodes() < 1)) Thread.sleep(10L);
    if (elapsed >= 5000L) throw new JPPFException("node not connected");
  }

  /**
   * Stops the driver and node.
   * @throws Exception if a process could not be stopped.
   */
  @AfterClass
  public static void cleanup() throws Exception {
    try (final JMXDriverConnectionWrapper jmx = driverJmx) {
      jmx.connectAndWait(5000L);
      if (jmx.isConnected()) BaseSetup.generateDriverThreadDump(jmx);
    }
    BaseSetup.cleanup();
  }
}
