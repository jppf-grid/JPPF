/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

import org.jppf.nio.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.nodeserver.AbstractNodeContext;
import org.jppf.server.protocol.*;
import org.jppf.server.queue.JPPFPriorityQueue;
import org.jppf.utils.StringUtils;

/**
 * 
 * @author Laurent Cohen
 */
public class ServerDebug implements ServerDebugMBean {
  /**
   * 
   */
  private final JPPFDriver driver = JPPFDriver.getInstance();

  @Override
  public String clientClassLoaderChannels() {
    return classLoaderChannels(clientClassLoaderSet());
  }

  @Override
  public String nodeClassLoaderChannels() {
    return classLoaderChannels(nodeClassLoaderSet());
  }

  /**
   * Get the class loader channels for a client or node.
   * @param set the set of channels to get a string representation of.
   * @return the channels as as an array of strings.
   */
  private String classLoaderChannels(final Set<ChannelWrapper<?>> set) {
    StringBuilder sb = new StringBuilder();
    synchronized(set) {
      for (ChannelWrapper<?> channel: set) sb.append(channel.toString()).append('\n');
    }
    return sb.toString();
  }

  @Override
  public String nodeDataChannels() {
    return viewChannels(nodeSet());
  }

  @Override
  public String clientDataChannels() {
    return viewChannels(clientSet());
  }

  @Override
  public String nodeMessages() {
    Set<ChannelWrapper<?>> set = new HashSet<>(nodeSet());
    StringBuilder sb = new StringBuilder();
    for (ChannelWrapper<?> ch: set) {
      long id = ch.getId();
      AbstractNodeContext ctx = (AbstractNodeContext) ch.getContext();
      String s = ctx.getMessage() == null ? "null" : ctx.getMessage().toString();
      sb.append("channelId=").append(id).append(", message=").append(s).append('\n');
    }
    return sb.toString();
  }

  @Override
  public String all() {
    StringBuilder sb = new StringBuilder();
    sb.append("jobs in queue:").append('\n');
    sb.append(dumpQueueDetails()).append('\n');
    sb.append('\n').append("node class loader channels:").append('\n');
    sb.append(nodeClassLoaderChannels()).append('\n');
    sb.append('\n').append("client class loader channels:").append('\n');
    sb.append(clientClassLoaderChannels()).append('\n');
    sb.append('\n').append("node job channels:").append('\n');
    sb.append(nodeDataChannels()).append('\n');
    sb.append('\n').append("client job channels:").append('\n');
    sb.append(clientDataChannels()).append('\n');
    return sb.toString();
  }

  /**
   * View the list of channels in the specified set.
   * @param set the set to view.
   * @return an array of state strings for each channel.
   */
  private String viewChannels(final Set<ChannelWrapper<?>> set) {
    StringBuilder sb = new StringBuilder();
    synchronized(set) {
      for (ChannelWrapper<?> channel: set) sb.append(channel.toString()).append('\n');
    }
    return sb.toString();
  }

  @Override
  public String allChannels() {
    StringBuilder sb = new StringBuilder();
    sb.append("node class loader channels:").append('\n');
    sb.append(nodeClassLoaderChannels()).append('\n');
    sb.append('\n').append("client class loader channels:").append('\n');
    sb.append(clientClassLoaderChannels()).append('\n');
    sb.append('\n').append("node job channels:").append('\n');
    sb.append(nodeDataChannels()).append('\n');
    sb.append('\n').append("client job channels:").append('\n');
    sb.append(clientDataChannels()).append('\n');
    return sb.toString();
  }

  @Override
  public String dumpQueue() {
    JPPFDriver.getInstance();
    JPPFPriorityQueue queue = (JPPFPriorityQueue) JPPFDriver.getQueue();
    Set<String> set = queue.getAllJobIds();
    StringBuilder sb = new StringBuilder();
    for (String uuid: set) sb.append(queue.getJob(uuid)).append('\n');
    return sb.toString();
  }

  @Override
  public String dumpQueueDetails() {
    JPPFDriver.getInstance();
    JPPFPriorityQueue queue = (JPPFPriorityQueue) JPPFDriver.getQueue();
    return dumpJobDetails(queue.getAllJobIds());
  }

  @Override
  public String dumpQueueDetailsFromPriorityMap() {
    JPPFDriver.getInstance();
    JPPFPriorityQueue queue = (JPPFPriorityQueue) JPPFDriver.getQueue();
    return dumpJobDetails(queue.getAllJobIdsFromPriorityMap());
  }

  /**
   * 
   * @param set the set of job uuids. 
   * @return .
   */
  private String dumpJobDetails(final Set<String> set) {
    JPPFPriorityQueue queue = (JPPFPriorityQueue) JPPFDriver.getQueue();
    StringBuilder sb = new StringBuilder();
    String hr = StringUtils.padRight("", '-', 80) + '\n';
    for (String uuid: set) {
      sb.append(hr);
      ServerJob serverJob = queue.getJob(uuid);
      sb.append(serverJob).append('\n');
      List<ServerTaskBundleClient> bundleList = serverJob.getClientBundles();
      if (bundleList.isEmpty()) sb.append("client bundles: empty\n");
      else {
        sb.append("client bundles:\n");
        for (ServerTaskBundleClient clientBundle: bundleList) sb.append("- ").append(clientBundle).append("\n");
      }
      List<ServerTaskBundleClient> completionBundles = serverJob.getCompletionBundles();
      if (completionBundles.isEmpty()) sb.append("client completion bundles: empty\n");
      else {
        sb.append("client completion bundles:\n");
        for (ServerTaskBundleClient clientBundle: completionBundles) sb.append("- ").append(clientBundle).append("\n");
      }
      Set<ServerTaskBundleNode> dispatchSet = serverJob.getDispatchSet();
      if (dispatchSet.isEmpty()) sb.append("node bundles: empty\n");
      else {
        sb.append("node bundles:\n");
        for (ServerTaskBundleNode nodeBundle: dispatchSet) sb.append("- ").append(nodeBundle).append("\n");
      }
    }
    return sb.toString();
  }

  /**
   * Get the set of client class loader connections.
   * @return a set of {@link ChannelWrapper} instances.
   */
  private Set<ChannelWrapper<?>> clientClassLoaderSet() {
    return new HashSet<>(driver.getClientClassServer().getAllConnections());
  }

  /**
   * Get the set of client class loader connections.
   * @return a set of {@link ChannelWrapper} instances.
   */
  private Set<ChannelWrapper<?>> nodeClassLoaderSet() {
    return new HashSet<>(driver.getNodeClassServer().getAllConnections());
  }

  /**
   * Get the set of client class loader connections.
   * @return a set of {@link ChannelWrapper} instances.
   */
  private Set<ChannelWrapper<?>> nodeSet() {
    List<AbstractNodeContext> list = driver.getNodeNioServer().getAllChannels();
    Set<ChannelWrapper<?>> set = new HashSet<>();
    for (AbstractNodeContext ctx: list) set.add(ctx.getChannel());
    return set;
  }

  /**
   * Get the set of client class loader connections.
   * @return a set of {@link ChannelWrapper} instances.
   */
  private Set<ChannelWrapper<?>> clientSet() {
    return new HashSet<>(driver.getClientNioServer().getAllConnections());
  }

  @Override
  public String taskQueueCheckerChannels() {
    List<AbstractNodeContext> list = driver.getNodeNioServer().getIdleChannels();
    StringBuilder sb = new StringBuilder();
    for (AbstractNodeContext ctx: list) sb.append(ctx).append('\n');
    return sb.toString();
  }

  @Override
  public String showResultsMap() {
    return DebugHelper.showResults();
  }

  @Override
  public int getJobNotifCount() {
    return driver.getJobManager().getNotifCount();
  }

  @Override
  public int getJobNotifPeak() {
    return driver.getJobManager().getNotifMax();
  }
}
