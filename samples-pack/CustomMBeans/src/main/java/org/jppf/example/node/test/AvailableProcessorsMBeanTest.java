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

package org.jppf.example.node.test;

import org.jppf.management.JMXNodeConnectionWrapper;

/**
 * A simple class to test a custom node MBean
 * @author Laurent Cohen
 */
public class AvailableProcessorsMBeanTest {
  /**
   * Entry point.
   * @param args not used.
   * @throws Exception if any error occurs.
   */
  public static void main(final String... args) throws Exception {
    // we assume the node is on localhost and uses the management port 12001
    try (JMXNodeConnectionWrapper wrapper = new JMXNodeConnectionWrapper("localhost", 12001)) {
      wrapper.connectAndWait(5000L);
      // query the node for the available processors
      final int n = (Integer) wrapper.invoke("org.jppf.example.mbean:name=AvailableProcessors,type=node", "queryAvailableProcessors", (Object[]) null, (String[]) null);
      System.out.println("The node has " + n + " available processors");
    }
  }
}
