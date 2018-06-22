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

package test.org.jppf.test.setup;

import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.ConcurrentUtils;
import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.*;

import test.org.jppf.test.setup.common.BaseTestHelper;


/**
 * Basic setup for 1 driver, 1 node and no client.
 * @author Laurent Cohen
 */
public class Setup1D1N extends BaseTest {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(Setup1D1N.class);
  /**
   * 
   */
  private static JMXDriverConnectionWrapper jmx;
  /** */
  @Rule
  public TestWatcher setup1D1NInstanceWatcher = new TestWatcher() {
    @Override
    protected void starting(final Description description) {
      if ((jmx != null) && jmx.isConnected()) BaseTestHelper.printToAll(jmx, false, false, true, true, true, "starting method %s()", description.getMethodName());
    }
  };

  /**
   * Launches a driver and node.
   * @throws Exception if a process could not be started.
   */
  @SuppressWarnings("resource")
  @BeforeClass
  public static void setup() throws Exception {
    BaseSetup.resetClientConfig();
    BaseSetup.setup(1, 1, false, BaseSetup.DEFAULT_CONFIG);
    // make sure the driver is initialized
    jmx = new JMXDriverConnectionWrapper("localhost", DRIVER_MANAGEMENT_PORT_BASE + 1);
    log.info("initializing {}", jmx);
    Assert.assertTrue(jmx.connectAndWait(5000L));
    ConcurrentUtils.awaitCondition(new ConcurrentUtils.ConditionFalseOnException() {
      @Override
      public boolean evaluateWithException() throws Exception {
        return jmx.nbIdleNodes() >= 1;
      }
    }, 5000L, 250L, false);
    BaseTestHelper.printToAll(jmx, false, false, true, true, true, "starting test of class %s", ReflectionUtils.getCurrentClassName());
    log.info("checked JMX connection");
  }

  /**
   * Stops the driver and node.
   * @throws Exception if a process could not be stopped.
   */
  @AfterClass
  public static void cleanup() throws Exception {
    try {
      if (jmx.isConnected()) {
        BaseSetup.generateDriverThreadDump(jmx);
        BaseTestHelper.printToAll(jmx, false, false, true, true, true, "ending test of class %s", ReflectionUtils.getCurrentClassName());
        jmx.close();
      }
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
    try {
      BaseSetup.cleanup();
    } catch(final Exception e) {
      e.printStackTrace();
      throw e;
    }
  }
}
