/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

import java.nio.channels.*;
import java.util.*;

import org.jppf.JPPFRuntimeException;
import org.jppf.nio.*;
import org.jppf.scripting.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.client.*;
import org.jppf.server.nio.nodeserver.BaseNodeContext;
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
   * Reference to the JPPF driver.
   */
  private final JPPFDriver driver;

  /**
   * 
   * @param driver reference to the JPPF driver.
   */
  public ServerDebug(final JPPFDriver driver) {
    this.driver = driver;
  }

  @Override
  public String clientClassLoaderChannels() {
    return viewContexts(clientClassLoaderSet());
  }

  @Override
  public String nodeClassLoaderChannels() {
    return viewContexts(nodeClassLoaderSet());
  }

  @Override
  public String nodeDataChannels() {
    return viewContexts(nodeSet());
  }

  @Override
  public String clientDataChannels() {
    final Selector selector = driver.getAsyncClientNioServer().getSelector();
    final Set<SelectionKey> keys = new HashSet<>(selector.keys());
    final StringBuilder sb = new StringBuilder();
    for (final SelectionKey key: keys)  sb.append(key.attachment()).append('\n');
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
  private static String viewContexts(final Set<NioContext> set) {
    final StringBuilder sb = new StringBuilder().append(set.size()).append(" channels\n");
    synchronized(set) {
      for (NioContext channel: set) sb.append(channel.toString()).append('\n');
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
    final JPPFPriorityQueue queue = driver.getQueue();
    final Set<String> set = queue.getAllJobIds();
    final StringBuilder sb = new StringBuilder();
    for (final String uuid: set) sb.append(queue.getJob(uuid)).append('\n');
    return sb.toString();
  }

  @Override
  public String dumpQueueDetails() {
    final JPPFPriorityQueue queue = driver.getQueue();
    return dumpJobDetails(queue.getAllJobIds());
  }

  @Override
  public String dumpQueueDetailsFromPriorityMap() {
    final JPPFPriorityQueue queue = driver.getQueue();
    return dumpJobDetails(queue.getAllJobIdsFromPriorityMap());
  }

  /**
   *
   * @param set the set of job uuids.
   * @return .
   */
  private String dumpJobDetails(final Set<String> set) {
    final JPPFPriorityQueue queue = driver.getQueue();
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
   * @return a set of {@link NioContext} instances.
   */
  private Set<NioContext> clientClassLoaderSet() {
    return new HashSet<>(driver.getAsyncClientClassServer().getAllProviderConnections());
  }

  /**
   * Get the set of client class loader connections.
   * @return a set of {@link NioContext} instances.
   */
  private Set<NioContext> nodeClassLoaderSet() {
    return new HashSet<>(driver.getAsyncNodeClassServer().getAllNodeConnections());
  }

  /**
   * Get the set of client class loader connections.
   * @return a set of {@link NioContext} instances.
   */
  private Set<NioContext> nodeSet() {
    final List<BaseNodeContext> list = driver.getAsyncNodeNioServer().getAllChannels();
    final Set<NioContext> set = new HashSet<>(list);
    return set;
  }

  @Override
  public String taskQueueCheckerChannels() {
    final List<BaseNodeContext> list = driver.getAsyncNodeNioServer().getJobScheduler().getIdleChannels();
    final StringBuilder sb = new StringBuilder();
    for (final BaseNodeContext ctx: list) sb.append(ctx).append('\n');
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
    return driver.getAsyncNodeNioServer().getNodeReservationHandler().getReservations();
  }

  @Override
  public String[] getReservedJobs() {
    final Set<String> set = driver.getAsyncNodeNioServer().getNodeReservationHandler().getReservedJobs();
    return set.toArray(new String[set.size()]);
  }

  @Override
  public String[] getReservedNodes() {
    final Set<String> set = driver.getAsyncNodeNioServer().getNodeReservationHandler().getReservedNodes();
    return set.toArray(new String[set.size()]);
  }

  @Override
  public void log(final String... messages) {
    if (messages != null) {
      for (String message: messages) log.info(message);
    }
  }

  @Override
  public void log(final String message) {
    if (message != null) log.info(message);
  }

  @Override
  public Object executeScript(final String language, final String script) throws JPPFScriptingException {
    try {
      final Map<String, Object> bindings = new HashMap<>();
      bindings.put("serverDebug", ServerDebug.this);
      if (log.isDebugEnabled()) log.debug("request to execute {} script with bindings = {} for driver = {}", language, bindings, driver.getUuid());
      else if (log.isTraceEnabled()) {
        final String s = script.endsWith("\n") ? script.substring(0, script.length() - 1) : script;
        log.trace("request to execute {} script with bindings = {} for driver = {}:\n{}", language, bindings, driver.getUuid(), s);
      }
      final Object result = new ScriptDefinition(language, script, bindings).evaluate();
      if (log.isTraceEnabled()) log.trace("script execution result: {}", result);
      return result;
    } catch (final JPPFScriptingException e) {
      throw e;
    } catch (final Throwable e) {
      throw new JPPFRuntimeException(e);
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

  /**
   * @return a reference to the JPPF driver.
   */
  public JPPFDriver getDriver() {
    return driver;
  }

  @Override
  public String getClientJobEntryStats() {
    return AsyncClientContext.getEntrystats().toString();
  }

  @Override
  public List<String> allClientJobEntries() {
    final AsyncClientNioServer server = driver.getAsyncClientNioServer();
    final Map<String, AsyncClientContext> map = server.performContextAction(context -> context.getEntryMap().size() > 0, null);
    final List<String> result = new ArrayList<>(map.size());
    int nbEntries = 0;
    for (final Map.Entry<String, AsyncClientContext> entry: map.entrySet()) {
      final AsyncClientContext context = entry.getValue();
      final Map<String, JobEntry> jobEntries = context.getEntryMap();
      nbEntries += jobEntries.size();
      final StringBuilder sb = new StringBuilder().append(jobEntries.size()).append(" job entries in ").append(context);
      jobEntries.forEach((id, jobEntry) -> sb.append("\n- ").append(jobEntry));
      result.add(sb.toString());
    }
    result.add("total: " + nbEntries + " entries");
    return result;
  }
}
