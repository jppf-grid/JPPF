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

package test.org.jppf.serialization;

import org.jppf.utils.JPPFConfiguration;
import org.jppf.utils.configuration.JPPFProperties;
import org.junit.BeforeClass;

import test.org.jppf.test.setup.BaseSetup;

/**
 * Unit tests for the JPPF serialization scheme.
 * @author Laurent Cohen
 */
public class TestJava extends AbstractTestSerialization {
  /**
   * Launches a driver and 1 node and start the client,
   * all setup with 1-way SSL authentication.
   * @throws Exception if a process could not be started.
   */
  @BeforeClass
  public static void setup() throws Exception {
    allowsNonSerializable = false;
    resetSerialization();
    System.out.println("main class loader = " + TestJava.class.getClassLoader());
    client = BaseSetup.setup(1, 1, true, true, createConfig("serialization/java"));
    print(false, false, "----- serialization class = %s -----", JPPFConfiguration.get(JPPFProperties.OBJECT_SERIALIZATION_CLASS));
  }
}
