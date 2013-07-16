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

package org.jppf.client;

import static org.jppf.client.JPPFClientConnectionStatus.*;

import java.net.*;

import org.jppf.JPPFError;
import org.jppf.client.event.*;
import org.jppf.comm.discovery.JPPFConnectionInformation;
import org.jppf.comm.socket.*;
import org.slf4j.*;

/**
 * This class provides an API to submit execution requests and administration
 * commands, and request server information data.<br>
 * It has its own unique identifier, used by the nodes, to determine whether
 * classes from the submitting application should be dynamically reloaded or not
 * depending on whether the uuid has changed or not.
 * @author Laurent Cohen
 */
public class JPPFClientConnectionImpl extends AbstractJPPFClientConnection
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFClientConnectionImpl.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this client with a specified application UUID.
   * @param client the JPPF client that owns this connection.
   * @param uuid the unique identifier of the remote driver.
   * @param name configuration name for this local client.
   * @param info the connection properties for this connection.
   * @param ssl determines whether this is an SSL connection.
   */
  public JPPFClientConnectionImpl(final JPPFClient client, final String uuid, final String name, final JPPFConnectionInformation info, final boolean ssl)
  {
    this.client = client;
    this.sslEnabled = ssl;
    this.connectionUuid = client.getUuid() + '_' + connectionCount.incrementAndGet();
    configure(uuid, name, info.host, ssl ? info.sslServerPorts[0] : info.serverPorts[0], 0, ssl);
    jmxPort = ssl ? info.sslManagementPort : info.managementPort;
    initializeJmxConnection();
  }

  @Override
  public void init()
  {
    try
    {
      if (isClosed()) throw new IllegalStateException("this client connection is closed");
      try
      {
        host = InetAddress.getByName(host).getHostName();
        displayName = name + '[' + host + ':' + port + ']';
        getJmxConnection().setHost(host);
      }
      catch (UnknownHostException e)
      {
        displayName = name;
      }
      delegate = new ClassServerDelegateImpl(this, client.getUuid(), host, port);
      delegate.addClientConnectionStatusListener(new ClientConnectionStatusListener()
      {
        @Override
        public void statusChanged(final ClientConnectionStatusEvent event)
        {
          delegateStatusChanged(event);
        }
      });
      taskServerConnection.addClientConnectionStatusListener(new ClientConnectionStatusListener()
      {
        @Override
        public void statusChanged(final ClientConnectionStatusEvent event)
        {
          taskServerConnectionStatusChanged(event);
        }
      });
      connect();
      JPPFClientConnectionStatus status = getStatus();
      if (debugEnabled) log.debug("connection [" + name + "] status=" + status);
      if (client.isClosed()) close();
      else if ((status == ACTIVE) || (status == EXECUTING))
      {
        client.addClientConnection(this);
        if (debugEnabled) log.debug("connection [" + name + "] added to the client pool");
      }
    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
      setStatus(FAILED);
    }
    catch (JPPFError e)
    {
      setStatus(FAILED);
      throw e;
    }
  }

  /**
   * Connect to the driver.
   * @throws Exception if connection failed.
   */
  protected void connect() throws Exception
  {
    delegate.init();
    if (!delegate.isClosed())
    {
      new Thread(delegate, delegate.getName()).start();
      taskServerConnection.init();
    }
  }

  @Override
  protected SocketInitializer createSocketInitializer()
  {
    return new SocketInitializerImpl();
  }
}
