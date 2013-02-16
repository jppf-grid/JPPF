/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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
package org.jppf.server.node.remote;

import org.jppf.comm.recovery.*;
import org.jppf.server.node.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class encapsulate execution nodes.
 * @author Laurent Cohen
 */
public class JPPFRemoteNode extends JPPFNode implements ClientConnectionListener
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFRemoteNode.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Connection to the recovery server.
   */
  private ClientConnection recoveryConnection = null;
  /**
   * Determines whether SSL is enabled.
   */
  private boolean sslEnabled = false;

  /**
   * Default constructor.
   */
  public JPPFRemoteNode()
  {
    super();
    classLoaderManager = new RemoteClassLoaderManager(this);
  }

  /**
   * Initialize this node's resources.
   * @throws Exception if an error is raised during initialization.
   * @see org.jppf.server.node.JPPFNode#initDataChannel()
   */
  @Override
  protected void initDataChannel() throws Exception
  {
    TypedProperties config = JPPFConfiguration.getProperties();
    (nodeConnection = new RemoteNodeConnection(serializer)).init();
    nodeIO = new RemoteNodeIO(this);
    if (config.getBoolean("jppf.recovery.enabled", false))
    {
      if (recoveryConnection == null)
      {
        if (debugEnabled) log.debug("Initializing recovery");
        recoveryConnection = new ClientConnection(uuid);
        recoveryConnection.addClientConnectionListener(this);
        new Thread(recoveryConnection, "reaper client connection").start();
      }
    }
  }

  /**
   * Initialize this node's data channel.
   * @throws Exception if an error is raised during initialization.
   * @see org.jppf.server.node.JPPFNode#closeDataChannel()
   */
  @Override
  protected void closeDataChannel() throws Exception
  {
    if (debugEnabled) log.debug("closing data channel: nodeConnection=" + nodeConnection + ", recoveryConnection=" + recoveryConnection);
    nodeConnection.close();
    if (recoveryConnection != null)
    {
      //clientConnection.removeClientConnectionListener(this);
      ClientConnection tmp = recoveryConnection;
      recoveryConnection = null;
      tmp.close();
    }
  }

  @Override
  public void clientConnectionFailed(final ClientConnectionEvent event)
  {
    try
    {
      if (debugEnabled) log.debug("recovery connection failed, attempting to reconnect this node");
      closeDataChannel();
    }
    catch(Exception e)
    {
      log.error(e.getMessage(), e);
    }
  }

  @Override
  protected NodeConnectionChecker createConnectionChecker()
  {
    return new RemoteNodeConnectionChecker(this);
  }

  @Override
  public boolean isLocal()
  {
    return false;
  }
}
