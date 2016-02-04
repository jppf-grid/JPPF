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

package org.jppf.server.nio.acceptor;

import javax.net.ssl.SSLEngine;

import org.jppf.nio.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class serve task execution requests to the JPPF nodes.
 * @author Laurent Cohen
 */
public class AcceptorNioServer extends NioServer<AcceptorState, AcceptorTransition>
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AcceptorNioServer.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * Initialize this server with the specified port numbers.
   * @param ports the ports this socket server is listening to.
   * @param sslPorts the SSL ports this socket server is listening to.
   * @throws Exception if the underlying server socket can't be opened.
   */
  public AcceptorNioServer(final int[] ports, final int[] sslPorts) throws Exception
  {
    super(ports, sslPorts, JPPFIdentifiers.ACCEPTOR_CHANNEL);
    this.selectTimeout = NioConstants.DEFAULT_SELECT_TIMEOUT;
  }

  @Override
  protected void createSSLContext() throws Exception
  {
  }

  @Override
  protected void configureSSLEngine(final SSLEngine engine) throws Exception
  {
  }

  @Override
  protected NioServerFactory<AcceptorState, AcceptorTransition> createFactory()
  {
    return new AcceptorServerFactory(this);
  }

  @Override
  public void postAccept(final ChannelWrapper<?> channel)
  {
    try
    {
      transitionManager.transitionChannel(channel, AcceptorTransition.TO_IDENTIFYING_PEER);
    }
    catch (Exception e)
    {
      if (debugEnabled) log.debug(e.getMessage(), e);
      else log.warn(ExceptionUtils.getMessage(e));
      closeChannel(channel);
    }
  }

  @Override
  public NioContext createNioContext()
  {
    AcceptorContext context = new AcceptorContext();
    return context;
  }

  /**
   * Close a connection to a node.
   * @param channel a <code>SocketChannel</code> that encapsulates the connection.
   */
  public void closeChannel(final ChannelWrapper<?> channel)
  {
    try
    {
      channel.close();
    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
    }
  }

  @Override
  public boolean isIdle(final ChannelWrapper<?> channel)
  {
    return false;
  }
}
