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

package org.jppf.test.addons.discovery;

import static org.jppf.utils.configuration.JPPFProperties.*;

import org.jppf.discovery.*;
import org.jppf.utils.JPPFConfiguration;

/**
 * 
 * @author Laurent Cohen
 */
public class TestPeerDriverDiscovery extends PeerDriverDiscovery {
  /** */
  public TestPeerDriverDiscovery() {
    System.out.printf("driver@%s:%d in %s()%n", JPPFConfiguration.get(SERVER_HOST), JPPFConfiguration.get(SERVER_PORT), getClass().getSimpleName());
  }

  @Override
  public void discover() throws InterruptedException {
    final int port = JPPFConfiguration.get(SERVER_PORT);
    final String host = JPPFConfiguration.get(SERVER_HOST);
    final DriverConnectionInfo info = new DriverConnectionInfo("custom_discovery", host, (port == 11101 ? 11102 : 11101));
    System.out.printf("%s 'discovering' %s%n", getClass().getSimpleName(), info);
    newConnection(info);
  }
}
