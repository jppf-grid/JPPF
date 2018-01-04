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

package test.org.jppf.serialization;

import java.util.*;

import org.jppf.utils.JPPFConfiguration;
import org.jppf.utils.configuration.JPPFProperties;
import org.junit.*;

import test.org.jppf.test.setup.*;

/**
 * Unit tests for the Kryo serialization scheme.
 * @author Laurent Cohen
 */
public class TestKryoWithLZ4 extends AbstractTestSerialization {
  /**
   * Launches a driver and 1 node and start the client,
   * all setup with 1-way SSL authentication.
   * @throws Exception if a process could not be started.
   */
  @BeforeClass
  public static void setup() throws Exception {
    allowsNonSerializable = true;
    final TestConfiguration config = createConfig("serialization/kryo_lz4");
    final List<String> commonCP = new ArrayList<>();
    commonCP.add("../samples-pack/KryoSerializer/classes");
    commonCP.add("../samples-pack/KryoSerializer/lib/kryo-serializers-0.26.jar");
    commonCP.add("../samples-pack/KryoSerializer/lib/kryo-2.22-all.jar");
    config.driverClasspath.addAll(commonCP);
    config.nodeClasspath.addAll(commonCP);
    client = BaseSetup.setup(1, 1, true, config);
    printOut("----- serialization class = %s -----", JPPFConfiguration.get(JPPFProperties.OBJECT_SERIALIZATION_CLASS));
  }
}
