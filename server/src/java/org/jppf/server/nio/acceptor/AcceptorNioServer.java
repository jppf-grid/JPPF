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

package org.jppf.server.nio.acceptor;

import java.nio.channels.SelectionKey;

import javax.net.ssl.*;

import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.*;
import org.jppf.utils.StringUtils;
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
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether TRACE logging level is enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * Reference to the driver.
   */
  private static JPPFDriver driver = JPPFDriver.getInstance();

  /**
   * Initialize this server with the specified port numbers.
   * @param ports the ports this socket server is listening to.
   * @param sslPorts the SSL ports this socket server is listening to.
   * @throws Exception if the underlying server socket can't be opened.
   */
  public AcceptorNioServer(final int[] ports, final int[] sslPorts) throws Exception
  {
    super(ports, sslPorts, NioConstants.ACCEPTOR, false);
    this.selectTimeout = NioConstants.DEFAULT_SELECT_TIMEOUT;
  }

  @Override
  protected void createSSLContext() throws Exception
  {
    // see http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#SSLContext for SSLContext algorithm names
    //sslContext = SSLContext.getInstance("SSL");
    sslContext = SSLContext.getDefault();
  }

  @Override
  protected void configureSSLEngine(final SSLEngine engine) throws Exception
  {
    SSLParameters params = sslContext.getDefaultSSLParameters();
    if (debugEnabled) log.debug("SSL parameters : cipher suites=" + StringUtils.arrayToString(params.getCipherSuites()) +
      ", protocols=" + StringUtils.arrayToString(params.getProtocols()) + ", neddCLientAuth=" + params.getNeedClientAuth() + ", wantClientAuth=" + params.getWantClientAuth());
    engine.setUseClientMode(false);
    engine.setSSLParameters(params);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected NioServerFactory<AcceptorState, AcceptorTransition> createFactory()
  {
    return new AcceptorServerFactory(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void postAccept(final ChannelWrapper<?> channel)
  {
    try
    {
      transitionManager.transitionChannel(channel, AcceptorTransition.TO_IDENTIFYING_PEER);
    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
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
   * Get the IO operations a connection is initially interested in.
   * @return a bit-wise combination of the interests, taken from
   * {@link java.nio.channels.SelectionKey SelectionKey} constants definitions.
   * @see org.jppf.server.nio.NioServer#getInitialInterest()
   */
  @Override
  public int getInitialInterest()
  {
    return SelectionKey.OP_READ;
  }

  /**
   * Close a connection to a node.
   * @param channel a <code>SocketChannel</code> that encapsulates the connection.
   */
  public static void closeChannel(final ChannelWrapper<?> channel)
  {
    if (JPPFDriver.JPPF_DEBUG) driver.getInitializer().getServerDebug().removeChannel(channel, NioConstants.ACCEPTOR);
    try
    {
      channel.close();
    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
    }
  }
}
