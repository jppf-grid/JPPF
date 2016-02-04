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

package org.jppf.server.nio.client;

import java.util.*;

import org.jppf.nio.*;
import org.jppf.server.JPPFDriver;
import org.jppf.utils.*;
import org.jppf.utils.stats.JPPFStatisticsHelper;
import org.slf4j.*;

/**
 * Instances of this class serve task execution requests to the JPPF nodes.
 * @author Laurent Cohen
 */
public class ClientNioServer extends NioServer<ClientState, ClientTransition>
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ClientNioServer.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Reference to the driver.
   */
  private static JPPFDriver driver;
  /**
   * 
   */
  private List<ChannelWrapper<?>> channels = new Vector<>();

  /**
   * Initialize this class loader server.
   * @param driver reference to the driver.
   * @param useSSL determines whether an SSLContext should be created for this server.
   * @throws Exception if the underlying server socket can't be opened.
   */
  public ClientNioServer(final JPPFDriver driver, final boolean useSSL) throws Exception
  {
    super(JPPFIdentifiers.CLIENT_JOB_DATA_CHANNEL, useSSL);
    if (driver == null) throw new IllegalArgumentException("driver is null");

    this.driver = driver;
    this.selectTimeout = NioConstants.DEFAULT_SELECT_TIMEOUT;
  }

  @Override
  protected NioServerFactory<ClientState, ClientTransition> createFactory()
  {
    return new ClientServerFactory(this);
  }

  @Override
  public void postAccept(final ChannelWrapper<?> channel)
  {
    try
    {
      channels.add(channel);
      transitionManager.transitionChannel(channel, ClientTransition.TO_WAITING_HANDSHAKE);
    }
    catch (Exception e)
    {
      if (debugEnabled) log.debug(e.getMessage(), e);
      else log.warn(ExceptionUtils.getMessage(e));
      closeClient(channel);
    }
    //driver.getStatsManager().newClientConnection();
    driver.getStatistics().addValue(JPPFStatisticsHelper.CLIENTS, 1);
  }

  @Override
  public NioContext createNioContext() {
    return new ClientContext();
  }

  /**
   * Remove the specified channel.
   * @param channel the channel to remove.
   */
  public void removeChannel(final ChannelWrapper<?> channel) {
    channels.remove(channel);
  }

  /**
   * Attempts to close the connection witht he specified uuid.
   * @param connectionUuid the connection uuid to correlate.
   */
  public void closeClientConnection(final String connectionUuid) {
    ChannelWrapper<?> channel = null;
    if (debugEnabled) log.debug("closing client channel with connectionUuid=" + connectionUuid);
    synchronized(channels) {
      for (ChannelWrapper<?> ch: channels) {
        ClientContext context = (ClientContext) ch.getContext();
        if (context.getConnectionUuid().equals(connectionUuid)) {
          channel = ch;
          break;
        }
      }
      if (channel != null) closeClient(channel);
    }
  }

  /**
   * Close a connection to a node.
   * @param channel a <code>SocketChannel</code> that encapsulates the connection.
   */
  public static void closeClient(final ChannelWrapper<?> channel) {
    if (debugEnabled) log.debug("closing client channel " + channel);
    try {
      driver.getClientNioServer().removeChannel(channel);
      channel.close();
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    try {
      driver.getStatistics().addValue(JPPFStatisticsHelper.CLIENTS, -1);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  @Override
  public boolean isIdle(final ChannelWrapper<?> channel) {
    return ClientState.IDLE == channel.getContext().getState();
  }

  @Override
  public List<ChannelWrapper<?>> getAllConnections() {
    return channels;
  }

  @Override
  public synchronized void removeAllConnections() {
    if (!isStopped()) return;
    List<ChannelWrapper<?>> list  = new ArrayList<>(channels);
    channels.clear();
    for (ChannelWrapper<?> channel: list) {
      try {
        closeClient(channel);
      } catch (Exception e) {
        log.error("error closing channel {} : {}", channel, ExceptionUtils.getStackTrace(e));
      }
    }
    super.removeAllConnections();
  }
}
