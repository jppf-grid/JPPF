/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

import org.junit.BeforeClass;

import test.org.jppf.test.setup.BaseSetup.Configuration;

/**
 * Tests with a driver and 2 offline nodes.
 * @author Laurent Cohen
 */
public class SetupOfflineNode1D2N1C extends AbstractSetupOfflineNode
{
  /**
   * Launches a driver and 2 nodes and start the client.
   * @throws Exception if a process could not be started.
   */
  @BeforeClass
  public static void setup() throws Exception
  {
    Configuration testConfig = createConfig();
    client = BaseSetup.setup(1, 2, true, testConfig);
  }
}
