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

package org.jppf.management;

import javax.management.NotificationBroadcasterSupport;

import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.nodeserver.*;
import org.jppf.utils.TypedProperties;

/**
 * 
 * @author Laurent Cohen
 * @exclude
 */
public final class PeerDriver extends NotificationBroadcasterSupport implements PeerDriverMBean {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The JPPF driver.
   */
  private final JPPFDriver driver;

  /**
   * Direct instantiation not permitted.
   * @param driver reference to the JPPF driver.
   */
  public PeerDriver(final JPPFDriver driver) {
    this.driver = driver;
    driver.setPeerDriver(this);
  }

  @Override
  public TypedProperties getPeerProperties() {
    final TypedProperties props = new TypedProperties();
    final PeerAttributesHandler peerHandler = driver.getAsyncNodeNioServer().getPeerHandler();
    props.setInt(PeerAttributesHandler.PEER_TOTAL_NODES, peerHandler.getTotalNodes());
    props.setInt(PeerAttributesHandler.PEER_TOTAL_THREADS, peerHandler.getTotalThreads());
    return props;
  }
}
