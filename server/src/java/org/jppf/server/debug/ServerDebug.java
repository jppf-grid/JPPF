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

package org.jppf.server.debug;

import java.util.*;

import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.*;
import org.jppf.server.protocol.*;
import org.jppf.server.queue.JPPFPriorityQueue;
import org.jppf.utils.StringUtils;

/**
 * 
 * @author Laurent Cohen
 */
public class ServerDebug implements ServerDebugMBean
{
  /**
   * The class loader channels.
   */
  private final Set<ChannelWrapper<?>> clientClassLoaderSet = new HashSet<>();
  /**
   * The class loader channels.
   */
  private final Set<ChannelWrapper<?>> nodeClassLoaderSet = new HashSet<>();
  /**
   * The node channels.
   */
  private final Set<ChannelWrapper<?>> nodeSet = new HashSet<>();
  /**
   * The client channels.
   */
  private final Set<ChannelWrapper<?>> clientSet = new HashSet<>();
  /**
   * The acceptor channels.
   */
  private final Set<ChannelWrapper<?>> acceptorSet = new HashSet<>();

  @Override
  public String[] clientClassLoaderChannels()
  {
    return classLoaderChannels(clientClassLoaderSet);
  }

  @Override
  public String[] nodeClassLoaderChannels()
  {
    return classLoaderChannels(nodeClassLoaderSet);
  }

  /**
   * Get the class loader channels for a client or node.
   * @param set the set of channels to get a string representation of.
   * @return the channels as as an array of strings.
   */
  private String[] classLoaderChannels(final Set<ChannelWrapper<?>> set)
  {
    String[] result = null;
    synchronized(set)
    {
      result = new String[set.size()];
      int count = 0;
      for (ChannelWrapper<?> channel: set) result[count++] = channel.toString();
    }
    return result;
  }

  @Override
  public String[] nodeDataChannels()
  {
    return viewChannels(nodeSet);
  }

  @Override
  public String[] clientDataChannels()
  {
    return viewChannels(clientSet);
  }

  @Override
  public String all()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("jobs in queue:").append('\n');
    sb.append(dumpQueueDetails()).append('\n');
    sb.append('\n').append("node class loader channels:").append('\n');
    for (String s: nodeClassLoaderChannels()) sb.append(s).append('\n');
    sb.append('\n').append("client class loader channels:").append('\n');
    for (String s: clientClassLoaderChannels()) sb.append(s).append('\n');
    sb.append('\n').append("node job channels:").append('\n');
    for (String s: nodeDataChannels()) sb.append(s).append('\n');
    sb.append('\n').append("client job channels:").append('\n');
    for (String s: clientDataChannels()) sb.append(s).append('\n');
    return sb.toString();
  }

  /**
   * View the list of channels in the specified set.
   * @param set the set to view.
   * @return an array of state strings for each channel.
   */
  private String[] viewChannels(final Set<ChannelWrapper<?>> set)
  {
    String[] result = null;
    synchronized(set)
    {
      result = new String[set.size()];
      int count = 0;
      for (ChannelWrapper<?> channel: set)
      {
        /*
        StringBuilder sb = new StringBuilder();
        sb.append(channel.toString());
        sb.append(", state=").append(channel.getContext().getState());
         */
        result[count++] = channel.toString();
      }
    }
    return result;
  }

  /**
   * Add a channel to those observed.
   * @param channel the channel to add.
   * @param serverName the name of the server that owns the channel.
   */
  public void addChannel(final ChannelWrapper<?> channel, final String serverName)
  {
    Set<ChannelWrapper<?>> set = findSetFromName(serverName);
    synchronized(set)
    {
      set.add(channel);
    }
  }

  /**
   * Remove a channel from those observed.
   * @param channel the channel to add.
   * @param serverName the name of the server that owns the channel.
   */
  public void removeChannel(final ChannelWrapper<?> channel, final String serverName)
  {
    Set<ChannelWrapper<?>> set = findSetFromName(serverName);
    synchronized(set)
    {
      set.remove(channel);
    }
  }

  /**
   * Get the set of channels for the specified server name.
   * @param name the name of the server for which to get the channels.
   * @return a set of <code>ChannelWrapper</code> instances.
   */
  private Set<ChannelWrapper<?>> findSetFromName(final String name)
  {
    if (NioConstants.CLIENT_CLASS_SERVER.equals(name)) return clientClassLoaderSet;
    else if (NioConstants.NODE_CLASS_SERVER.equals(name)) return nodeClassLoaderSet;
    else if (NioConstants.NODE_SERVER.equals(name)) return nodeSet;
    else if (NioConstants.CLIENT_SERVER.equals(name)) return clientSet;
    return acceptorSet;
  }

  @Override
  public String[] allChannels()
  {
    int size = clientClassLoaderSet.size() + nodeClassLoaderSet.size() + nodeSet.size() + clientSet.size();
    List<String> list = new ArrayList<>(size);
    for (ChannelWrapper<?> channel: clientClassLoaderSet) list.add(channel.toString());
    for (ChannelWrapper<?> channel: nodeClassLoaderSet) list.add(channel.toString());
    for (ChannelWrapper<?> channel: nodeSet) list.add(channel.toString());
    for (ChannelWrapper<?> channel: clientSet) list.add(channel.toString());
    return list.toArray(new String[list.size()]);
  }

  @Override
  public String dumpQueue()
  {
    JPPFDriver.getInstance();
    JPPFPriorityQueue queue = (JPPFPriorityQueue) JPPFDriver.getQueue();
    Set<String> set = queue.getAllJobIds();
    StringBuilder sb = new StringBuilder();
    for (String uuid: set) sb.append(queue.getJob(uuid)).append('\n');
    return sb.toString();
  }

  @Override
  public String dumpQueueDetails()
  {
    JPPFDriver.getInstance();
    JPPFPriorityQueue queue = (JPPFPriorityQueue) JPPFDriver.getQueue();
    Set<String> set = queue.getAllJobIds();
    StringBuilder sb = new StringBuilder();
    String hr = StringUtils.padRight("", '-', 80) + '\n';
    for (String uuid: set)
    {
      sb.append(hr);
      ServerJob serverJob = queue.getJob(uuid);
      sb.append(serverJob).append('\n');
      List<ServerTaskBundleClient> bundleList = serverJob.getBundleList();
      if (bundleList.isEmpty()) sb.append("client bundles: empty\n");
      else
      {
      sb.append("client bundles:\n");
      for (ServerTaskBundleClient clientBundle: bundleList) sb.append("- ").append(clientBundle).append("\n");
      }
      List<ServerTaskBundleClient> completionBundles = serverJob.getCompletionBundles();
      if (completionBundles.isEmpty()) sb.append("client completion bundles: empty\n");
      else
      {
        sb.append("client completion bundles:\n");
        for (ServerTaskBundleClient clientBundle: completionBundles) sb.append("- ").append(clientBundle).append("\n");
      }
      Set<ServerTaskBundleNode> dispatchSet = serverJob.getDispatchSet();
      if (dispatchSet.isEmpty()) sb.append("node bundles: empty\n");
      else
      {
        sb.append("node bundles:\n");
        for (ServerTaskBundleNode nodeBundle: dispatchSet) sb.append("- ").append(nodeBundle).append("\n");
      }
    }
    return sb.toString();
  }
}
