/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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
package org.jppf.server.peer;

import org.jppf.comm.discovery.JPPFConnectionInformation;
import org.jppf.server.nio.classloader.ClassNioServer;
import org.slf4j.*;


/**
 * Instances of this class are used to initialize the connections to a peer driver
 * in a separate thread.
 * @author Laurent Cohen
 */
public class JPPFPeerInitializer extends Thread
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFPeerInitializer.class);
  /**
   * Name of the peer in the configuration file.
   */
  private final String peerName;
  /**
   * Peer connection information.
   */
  private final JPPFConnectionInformation connectionInfo;
  /**
   * JPPF class server
   */
  private final ClassNioServer classServer;

  /**
   * Initialize this peer initializer from a specified peerName.
   * @param peerName the name of the peer in the configuration file.
   * @param connectionInfo peer connection information.
   * @param classServer JPPF class server
   */
  public JPPFPeerInitializer(final String peerName, final JPPFConnectionInformation connectionInfo, final ClassNioServer classServer)
  {
    if(peerName == null || peerName.isEmpty()) throw new IllegalArgumentException("peerName is blank");
    if(connectionInfo == null) throw new IllegalArgumentException("connectionInfo is null");

    this.peerName       = peerName;
    this.connectionInfo = connectionInfo;
    this.classServer    = classServer;
    setName("Peer Initializer [" + peerName + ']');
  }

  /**
   * Perform the peer initialization.
   * @see java.lang.Thread#run()
   */
  @Override
  public void run()
  {
    log.info("start initialization of peer [" + peerName + ']');
    try
    {
      new PeerResourceProvider(peerName, connectionInfo, classServer).init();
      new PeerNode(peerName, connectionInfo).run();
    }
    catch(Exception e)
    {
      log.error(e.getMessage(), e);
    } finally
    {
      log.info("end initialization of peer [" + peerName + ']');
    }
  }
}
