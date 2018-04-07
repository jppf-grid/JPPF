/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

import org.jppf.nio.ChannelWrapper;
import org.jppf.scripting.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.nodeserver.AbstractNodeContext;
import org.jppf.server.protocol.*;
import org.jppf.server.queue.JPPFPriorityQueue;
import org.jppf.utils.StringUtils;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class ServerDebug implements ServerDebugMBean {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ServerDebug.class);
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
  private static String classLoaderChannels(final Set<ChannelWrapper<?>> set) {
    final StringBuilder sb = new StringBuilder();
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
    final Set<ChannelWrapper<?>> set = new HashSet<>(nodeSet());
    final StringBuilder sb = new StringBuilder();
    for (ChannelWrapper<?> ch: set) {
      final long id = ch.getId();
      final AbstractNodeContext ctx = (AbstractNodeContext) ch.getContext();
      final String s = ctx.getMessage() == null ? "null" : ctx.getMessage().toString();
      sb.append("channelId=").append(id).append(", message=").append(s).append('\n');
    }
    return sb.toString();
  }

  @Override
  public String all() {
    final StringBuilder sb = new StringBuilder();
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
  private static String viewChannels(final Set<ChannelWrapper<?>> set) {
    final StringBuilder sb = new StringBuilder();
    synchronized(set) {
      for (ChannelWrapper<?> channel: set) sb.append(channel.toString()).append('\n');
    }
    return sb.toString();
  }

  @Override
  public String allChannels() {
    final StringBuilder sb = new StringBuilder();
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
    final JPPFPriorityQueue queue = JPPFDriver.getInstance().getQueue();
    final Set<String> set = queue.getAllJobIds();
    final StringBuilder sb = new StringBuilder();
    for (final String uuid: set) sb.append(queue.getJob(uuid)).append('\n');
    return sb.toString();
  }

  @Override
  public String dumpQueueDetails() {
    JPPFDriver.getInstance();
    final JPPFPriorityQueue queue = JPPFDriver.getInstance().getQueue();
    return dumpJobDetails(queue.getAllJobIds());
  }

  @Override
  public String dumpQueueDetailsFromPriorityMap() {
    JPPFDriver.getInstance();
    final JPPFPriorityQueue queue = JPPFDriver.getInstance().getQueue();
    return dumpJobDetails(queue.getAllJobIdsFromPriorityMap());
  }

  /**
   *
   * @param set the set of job uuids.
   * @return .
   */
  private static String dumpJobDetails(final Set<String> set) {
    final JPPFPriorityQueue queue = JPPFDriver.getInstance().getQueue();
    final StringBuilder sb = new StringBuilder();
    final String hr = StringUtils.padRight("", '-', 80) + '\n';
    for (final String uuid: set) {
      final ServerJob serverJob = queue.getJob(uuid);
      if (serverJob != null) {
        sb.append(hr);
        sb.append(serverJob).append('\n');
        final List<ServerTaskBundleClient> bundleList = serverJob.getClientBundles();
        if (bundleList.isEmpty()) sb.append("client bundles: empty\n");
        else {
          sb.append("client bundles:\n");
          for (ServerTaskBundleClient clientBundle: bundleList) sb.append("- ").append(clientBundle).append("\n");
        }
        final List<ServerTaskBundleClient> completionBundles = serverJob.getCompletionBundles();
        if (completionBundles.isEmpty()) sb.append("client completion bundles: empty\n");
        else {
          sb.append("client completion bundles:\n");
          for (ServerTaskBundleClient clientBundle: completionBundles) sb.append("- ").append(clientBundle).append("\n");
        }
        final Set<ServerTaskBundleNode> dispatchSet = serverJob.getDispatchSet();
        if (dispatchSet.isEmpty()) sb.append("node bundles: empty\n");
        else {
          sb.append("node bundles:\n");
          for (ServerTaskBundleNode nodeBundle: dispatchSet) sb.append("- ").append(nodeBundle).append("\n");
        }
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
    final List<AbstractNodeContext> list = driver.getNodeNioServer().getAllChannels();
    final Set<ChannelWrapper<?>> set = new HashSet<>();
    for (final AbstractNodeContext ctx: list) set.add(ctx.getChannel());
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
    final List<AbstractNodeContext> list = driver.getNodeNioServer().getIdleChannels();
    final StringBuilder sb = new StringBuilder();
    for (final AbstractNodeContext ctx: list) sb.append(ctx).append('\n');
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

  @Override
  public Map<String, Set<String>> getAllReservations() {
    return driver.getNodeNioServer().getNodeReservationHandler().getReservations();
  }

  @Override
  public String[] getReservedJobs() {
    final Set<String> set = driver.getNodeNioServer().getNodeReservationHandler().getReservedJobs();
    return set.toArray(new String[set.size()]);
  }

  @Override
  public String[] getReservedNodes() {
    final Set<String> set = driver.getNodeNioServer().getNodeReservationHandler().getReservedNodes();
    return set.toArray(new String[set.size()]);
  }

  @Override
  public void log(final String... messages) {
    if (messages != null) {
      for (String message: messages) log.info(message);
    }
  }

  @Override
  public Object executeScript(final String language, final String script) throws JPPFScriptingException {
    if (log.isTraceEnabled()) log.trace(String.format("request to execute %s script:%n%s", language, script));
    final ScriptRunner runner = ScriptRunnerFactory.getScriptRunner(language);
    if (runner == null) throw new IllegalStateException("Could not instantiate a script runner for language = " + language);
    try {
      return runner.evaluate(script, null);
    } finally {
      ScriptRunnerFactory.releaseScriptRunner(runner);
    }
  }

  @Override
  public void startProfiling() {
    startProf();
  }

  @Override
  public void endProfiling() {
    endProf();
  }

  /**
   * Trigger method for the profiler to start profiling.
   */
  public static void startProf() {
  }

  /**
   * Trigger method for the profiler to stop profiling.
   */
  public static void endProf() {
  }
}
