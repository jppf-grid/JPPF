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

package org.jppf.server;

import java.util.*;

import org.jppf.management.*;
import org.jppf.server.nio.ChannelWrapper;
import org.slf4j.*;

/**
 * Instances of this class manage information on the nodes obtained via JMX.
 * @author Laurent Cohen
 * @exclude
 */
public class NodeInformationHandler
{
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(NodeInformationHandler.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * A list of objects containing the information required to connect to the nodes JMX servers.
   */
  private final Map<ChannelWrapper<?>, JPPFManagementInfo> nodeInfo = new HashMap<ChannelWrapper<?>, JPPFManagementInfo>();
  /**
   * A list of objects containing the information required to connect to the nodes JMX servers.
   */
  private final Map<String, JPPFManagementInfo> uuidMap = new HashMap<String, JPPFManagementInfo>();

  /**
   * Add a node information object to the map of node information.
   * @param channel a <code>SocketChannel</code> instance.
   * @param info a <code>JPPFNodeManagementInformation</code> instance.
   */
  public void addNodeInformation(final ChannelWrapper<?> channel, final JPPFManagementInfo info)
  {
    if (debugEnabled) log.debug("adding node information for " + info + ", channel=" + channel);
    synchronized (nodeInfo)
    {
      nodeInfo.put(channel, info);
    }
    synchronized (uuidMap)
    {
      uuidMap.put(info.getUuid(), info);
    }
  }

  /**
   * Remove a node information object from the map of node information.
   * @param channel a <code>SocketChannel</code> instance.
   */
  public void removeNodeInformation(final ChannelWrapper<?> channel)
  {
    if (debugEnabled) log.debug("removing node information for channel=" + channel);
    JPPFManagementInfo info = null;
    synchronized (nodeInfo)
    {
      info = nodeInfo.remove(channel);
    }
    synchronized (uuidMap)
    {
      if (info != null) uuidMap.remove(info.getUuid());
    }
  }

  /**
   * Add a node information object to the map of node information.
   * @param channel a <code>SocketChannel</code> instance.
   * @param info a <code>JPPFNodeManagementInformation</code> instance.
   */
  public void updateNodeInformation(final ChannelWrapper<?> channel, final JPPFSystemInformation info)
  {
    if (debugEnabled) log.debug("updating node information for " + info + ", channel=" + channel);
    JPPFManagementInfo mgtInfo = null;
    synchronized (nodeInfo)
    {
      mgtInfo = nodeInfo.get(channel);
      if (mgtInfo != null) mgtInfo.setSystemInfo(info);
    }
  }

  /**
   * Get the mapping of channels to corresponding node information.
   * @return channel a map of <code>ChannelWrapper</code> keys to <code>JPPFManagementInfo</code> values.
   */
  public Map<ChannelWrapper<?>, JPPFManagementInfo> getNodeInformationMap()
  {
    synchronized (nodeInfo)
    {
      return Collections.unmodifiableMap(nodeInfo);
    }
  }

  /**
   * Get number of nodes attached to the server.
   * @return the number of nodes as an <code>int</code> value.
   */
  public int getNbNodes()
  {
    synchronized (nodeInfo)
    {
      return nodeInfo.size();
    }
  }

  /**
   * Get the mapping of node uuid to corresponding node information.
   * @return channel a map of <code>String</code> keys to <code>JPPFManagementInfo</code> values.
   */
  public Map<String, JPPFManagementInfo> getUuidMap()
  {
    synchronized (uuidMap)
    {
      return Collections.unmodifiableMap(uuidMap);
    }
  }

  /**
   * Get the system information for the specified node.
   * @param channel the node for which to get the information.
   * @return a <code>JPPFManagementInfo</code> instance, or null if no information is recorded for the node.
   */
  public JPPFManagementInfo getNodeInformation(final ChannelWrapper<?> channel)
  {
    synchronized (nodeInfo)
    {
      return nodeInfo.get(channel);
    }
  }

  /**
   * Get the system information for the specified node uuid.
   * @param uuid the uuid of the node for which to get the information.
   * @return a <code>JPPFManagementInfo</code> instance, or null if no information is recorded for the node.
   */
  public JPPFManagementInfo getNodeInformation(final String uuid)
  {
    synchronized (uuidMap)
    {
      return uuidMap.get(uuid);
    }
  }


  /**
   * The uuids of all the currently connected nodes.
   * @return a set of uuids for all the nodes connected to the driver.
   */
  public Set<String> getNodeUuids()
  {
    synchronized (uuidMap)
    {
      return Collections.unmodifiableSet(uuidMap.keySet());
    }
  }
}
