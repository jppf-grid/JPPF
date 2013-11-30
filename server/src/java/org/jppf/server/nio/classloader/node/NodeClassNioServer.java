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

package org.jppf.server.nio.classloader.node;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.jppf.comm.recovery.*;
import org.jppf.nio.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.classloader.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class serve class loading requests from the JPPF nodes.
 * @author Laurent Cohen
 */
public class NodeClassNioServer extends ClassNioServer implements ReaperListener
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(NodeClassNioServer.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether TRACE logging level is enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * The thread polling the local channel.
   */
  protected ChannelSelectorThread selectorThread = null;
  /**
   * The local channel, if any.
   */
  protected ChannelWrapper<?> localChannel = null;
  /**
   * Mapping of channels to their uuid.
   */
  //protected final Map<String, ChannelWrapper<?>> nodeConnections = new HashMap<>();
  protected final Map<String, ChannelWrapper<?>> nodeConnections = new ConcurrentHashMap<>();

  /**
   * Initialize this class server.
   * @param driver reference to the driver.
   * @param useSSL determines whether an SSLContext should be created for this server.
   * @throws Exception if the underlying server socket can't be opened.
   */
  public NodeClassNioServer(final JPPFDriver driver, final boolean useSSL) throws Exception
  {
    super(JPPFIdentifiers.NODE_CLASSLOADER_CHANNEL, driver, useSSL);
  }

  /**
   * Initialize the local channel connection.
   * @param localChannel the local channel to use.
   */
  public void initLocalChannel(final ChannelWrapper<?> localChannel)
  {
    if (JPPFConfiguration.getProperties().getBoolean("jppf.local.node.enabled", false))
    {
      this.localChannel = localChannel;
      ChannelSelector channelSelector = new LocalChannelSelector(localChannel);
      localChannel.setSelector(channelSelector);
      selectorThread = new ChannelSelectorThread(channelSelector, this);
      localChannel.setInterestOps(0);
      new Thread(selectorThread, "ClassChannelSelector").start();
      postAccept(localChannel);
    }
  }

  @Override
  protected NioServerFactory<ClassState, ClassTransition> createFactory()
  {
    return new NodeClassServerFactory(this);
  }

  @Override
  public void postAccept(final ChannelWrapper<?> channel)
  {
    try
    {
      synchronized(channel)
      {
        transitionManager.transitionChannel(channel, ClassTransition.TO_WAITING_INITIAL_NODE_REQUEST);
        if (transitionManager.checkSubmitTransition(channel)) transitionManager.submitTransition(channel);
      }
    }
    catch (Exception e)
    {
      if (debugEnabled) log.debug(e.getMessage(), e);
      else log.warn(ExceptionUtils.getMessage(e));
      closeConnection(channel);
    }
  }

  /**
   * Get a channel from its uuid.
   * @param uuid the uuid key to look up in the the map.
   * @return channel the corresponding channel.
   */
  protected ChannelWrapper<?> getNodeConnection(final String uuid)
  {
    return nodeConnections.get(uuid);
  }

  /**
   * Put the specified uuid / channel pair into the uuid map.
   * @param uuid the uuid key to add to the map.
   * @param channel the corresponding channel.
   */
  public void addNodeConnection(final String uuid, final ChannelWrapper<?> channel)
  {
    if (debugEnabled) log.debug("adding node connection: uuid=" + uuid + ", channel=" + channel);
    nodeConnections.put(uuid, channel);
  }

  /**
   * Remove the specified uuid entry from the uuid map.
   * @param uuid the uuid key to remove from the map.
   * @return channel the corresponding channel.
   */
  public ChannelWrapper<?> removeNodeConnection(final String uuid)
  {
    if (debugEnabled) log.debug("removing node connection: uuid=" + uuid);
    return nodeConnections.remove(uuid);
  }

  /**
   * Close the specified connection.
   * @param channel the channel representing the connection.
   */
  public static void closeConnection(final ChannelWrapper<?> channel)
  {
    if (channel == null)
    {
      log.warn("attempt to close null channel - skipping this step");
      return;
    }
    NodeClassNioServer server = JPPFDriver.getInstance().getNodeClassServer();
    ClassContext context = (ClassContext) channel.getContext();
    String uuid = context.getUuid();
    if (uuid != null) server.removeNodeConnection(uuid);
    try
    {
      channel.close();
    }
    catch(Exception e)
    {
      if (debugEnabled) log.debug(e.getMessage(), e);
      else log.warn(e.getMessage());
    }
  }

  @Override
  public void connectionFailed(final ReaperEvent event)
  {
    ServerConnection c = event.getConnection();
    if (!c.isOk())
    {
      String uuid = c.getUuid();
      ChannelWrapper<?> channel = getNodeConnection(uuid);
      if (debugEnabled) log.debug("about to close channel = " + channel + " with uuid = " + uuid);
      closeConnection(channel);
    }
  }

  /**
   * Close and remove all connections accepted by this server.
   * @see org.jppf.nio.NioServer#removeAllConnections()
   */
  @Override
  public synchronized void removeAllConnections()
  {
    if (!isStopped()) return;
    nodeConnections.clear();
    super.removeAllConnections();
  }

  @Override
  public boolean isIdle(final ChannelWrapper<?> channel)
  {
    return ClassState.IDLE_NODE == channel.getContext().getState();
  }

  @Override
  public List<ChannelWrapper<?>> getAllConnections()
  {
    List<ChannelWrapper<?>> list = super.getAllConnections();
    if (localChannel != null) list.add(localChannel);
    return list;
  }
}
