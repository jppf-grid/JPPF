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

import org.junit.*;


/**
 * Basic setup for 1 driver, 1 node and no client.
 * @author Laurent Cohen
 */
public class Setup1D1N extends BaseTest {
  /**
   * Launches a driver and node.
   * @throws Exception if a process could not be started.
   */
  @BeforeClass
  public static void setup() throws Exception {
    BaseSetup.setup(1, 1, false, BaseSetup.DEFAULT_CONFIG);
  }

  /**
   * Stops the driver and node.
   * @throws Exception if a process could not be stopped.
   */
  @AfterClass
  public static void cleanup() throws Exception {
    try {
      BaseSetup.cleanup();
    } catch(Exception e) {
      e.printStackTrace();
      throw e;
    }
  }
}
