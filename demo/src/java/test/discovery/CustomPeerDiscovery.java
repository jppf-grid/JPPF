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

package test.discovery;

import org.jppf.discovery.*;
import org.jppf.utils.JPPFConfiguration;
import org.jppf.utils.configuration.JPPFProperties;

/**
 * 
 * @author Laurent Cohen
 */
public class CustomPeerDiscovery extends PeerDriverDiscovery {
  /**
   * 
   */
  public CustomPeerDiscovery() {
    System.out.printf("in %s() contructor%n", getClass().getSimpleName());
  }

  @Override
  public void discover() {
    int port = JPPFConfiguration.get(JPPFProperties.SERVER_PORT);
    DriverConnectionInfo info = new DriverConnectionInfo("custom_discovery", "localhost", (port == 11111 ? 11112 : 11111));
    System.out.printf("%s 'discovering' %s%n", getClass().getSimpleName(), info);
    newConnection(info);
  }
}
