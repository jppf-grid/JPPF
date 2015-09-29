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

package org.jppf.management;

import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.nodeserver.*;
import org.jppf.utils.TypedProperties;

/**
 * 
 * @author Laurent Cohen
 */
public class PeerDriver implements PeerDriverMBean {
  /**
   * The JPPF driver.
   */
  static JPPFDriver driver = JPPFDriver.getInstance();

  @Override
  public TypedProperties getPeerProperties() {
    NodeNioServer server = driver.getNodeNioServer();
    TypedProperties props = new TypedProperties();
    if (server != null) {
      props.setProperty(PeerAttributesHandler.PEER_TOTAL_NODES, Integer.toString(server.getTotalNodes()));
      props.setProperty(PeerAttributesHandler.PEER_TOTAL_THREADS, Integer.toString(server.getTotalThreads()));
    }
    return props;
  }
}
