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

package org.jppf.server.nio.client;

import static org.jppf.utils.stats.JPPFStatisticsHelper.*;

import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.io.*;
import org.jppf.nio.StatelessNioContext;
import org.jppf.node.protocol.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.protocol.*;
import org.jppf.utils.*;
import org.jppf.utils.stats.*;
import org.slf4j.*;

import com.sun.xml.internal.ws.api.policy.PolicyResolver.ClientContext;

/**
 * Context or state information associated with a channel that exchanges heartbeat messages between the server and a node or client.
 * @author Laurent Cohen
 */
public class AsyncClientContext extends StatelessNioContext {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AsyncClientContext.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether TRACE logging level is enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * Reference to the driver.
   */
  final JPPFDriver driver;
  /**
   * The server that handles this context.
   */
  final AsyncClientNioServer server;
  /**
   * A map of the client bundles sent over this connection.
   */
  private final Map<String, JobEntry> entryMap = new ConcurrentHashMap<>();
  /**
   * This queue contains all the result bundles to send back to the client.
   */
  private final BlockingQueue<ClientMessage> sendQueue = new LinkedBlockingQueue<>();

  /**
   * @param server the server that handles this context.
   * @param socketChannel the associated socket channel.
   */
  AsyncClientContext(final AsyncClientNioServer server, final SocketChannel socketChannel) {
    this.server = server;
    this.driver = server.getDriver();
    this.socketChannel = socketChannel;
  }

  @Override
  public void handleException(final Exception e) {
    if (getClosed().compareAndSet(false, true)) {
      if (debugEnabled) log.debug("handling exception on {}:{}", this, (e == null) ? " null" : "\n" + ExceptionUtils.getStackTrace(e));
      cancelJobsOnClose();
      server.closeConnection(this);
      onClose();
    }
  }

  /**
   * Serialize specified bundle into a message to send.
   * @param bundle the byndle to process.
   * @return the created message.
   * @throws Exception if any error occurs.
   */
  public ClientMessage serializeBundle(final ServerTaskBundleClient bundle) throws Exception {
    final ClientMessage message = newMessage(bundle);
    final TaskBundle header = bundle.getJob();
    header.setSLA(null);
    header.setMetadata(null);
    final List<ServerTask> tasks = (header.isHandshake()) ? null : bundle.getTaskList();
    if (!header.isHandshake()) {
      final int[] positions = new int[tasks.size()];
      for (int i=0; i<tasks.size(); i++) positions[i] = tasks.get(i).getJobPosition();
      //if (traceEnabled) log.trace("serializing bundle with tasks postions={}", StringUtils.buildString(positions));
      header.setParameter(BundleParameter.TASK_POSITIONS, positions);
      header.removeParameter(BundleParameter.TASK_MAX_RESUBMITS);
    }
    message.addLocation(IOHelper.serializeData(header, driver.getSerializer()));
    if (tasks != null) for (ServerTask task: tasks) message.addLocation(task.getResult());
    message.setBundle(header);
    return message;
  }

  /**
   * Deserialize a task bundle from a message read from a connection.
   * @param message the message to process.
   * @return a {@link ClientContext} instance.
   * @throws Exception if an error occurs during the deserialization.
   */
  public ServerTaskBundleClient deserializeBundle(final ClientMessage message) throws Exception {
    final List<DataLocation> locations = message.getLocations();
    //if (traceEnabled) log.trace("deserializing {}", message);
    final TaskBundle bundle = message.getBundle();
    if (locations.size() <= 2) return new ServerTaskBundleClient(bundle, locations.get(1));
    return new ServerTaskBundleClient(bundle, locations.get(1), locations.subList(2, locations.size()), isPeer());
  }

  /**
   * Create a new message.
   * @param clientBundle the actual client task bundle from which this message is created.
   * @return an {@link ClientMessage} instance.
   */
  public ClientMessage newMessage(final ServerTaskBundleClient clientBundle) {
    return new ClientMessage(this, clientBundle);
  }

  @Override
  public boolean readMessage() throws Exception {
    if (message == null) message = newMessage(null);
    byteCount = message.getChannelReadCount();
    boolean b = false;
    try {
      b = message.read();
    } catch (final Exception e) {
      updateTrafficStats((ClientMessage) message);
      throw e;
    }
    byteCount = message.getChannelReadCount() - byteCount;
    if (b) updateTrafficStats((ClientMessage) message);
    return b;
  }

  @Override
  public boolean writeMessage() throws Exception {
    writeByteCount = writeMessage.getChannelWriteCount();
    boolean b = false;
    try {
      b = writeMessage.write();
    } catch (final Exception e) {
      updateTrafficStats((ClientMessage) writeMessage);
      throw e;
    }
    writeByteCount = writeMessage.getChannelWriteCount() - writeByteCount;
    if (b) updateTrafficStats((ClientMessage) writeMessage);
    return b;
  }

  /**
   * Add a new job to the job map.
   * @param bundle the job to add.
   */
  public void addEntry(final ServerTaskBundleClient bundle) {
    final String id = bundle.getUuid() + bundle.getId();
    if (debugEnabled) log.debug("adding job entry '{}'", id);
    entryMap.put(id, new JobEntry(bundle));
  }

  /**
   * Retrieve the job entry with the specified id.
   * @param id the id of the job entry to retrieve.
   * @return a {@link JobEntry} instance, or {@code null} if there is no entry with the specified id.
   */
  public JobEntry getJobEntry(final String id) {
    return entryMap.get(id);
  }

  /**
   * Remove the job entry with the specified id.
   * @param jobUuid the uuid of the job whose entry to retrieve.
   * @param bundleId the id of the client job bundle for the entry.
   * @return the removed {@link JobEntry} instance, or {@code null} if there is no entry with the specified id.
   */
  public JobEntry removeJobEntry(final String jobUuid, final long bundleId) {
    if (traceEnabled) log.trace("removing job entry with jobUuid={}, bundleId={}, call stack:\n{}", jobUuid, bundleId, ExceptionUtils.getCallStack());
    else if (debugEnabled) log.debug("removing job entry with jobUuid={}, bundleId={}", jobUuid, bundleId);
    return entryMap.remove(jobUuid + bundleId);
  }

  /**
   * Cancel the jobs upon client disconnection.
   */
  void cancelJobsOnClose() {
    final List<String> entriesToRemove = new ArrayList<>(entryMap.size());
    entryMap.forEach((id, entry) -> {
      cancelJobOnClose(entry);
      if (entry != null) {
        final ServerTaskBundleClient bundle = entry.getBundle();
        if ((bundle != null) && (bundle.getSLA().isCancelUponClientDisconnect())) {
          entriesToRemove.add(id);
        }
      }
    });
    entriesToRemove.forEach(id -> entryMap.remove(id));
  }

  /**
   * Cancel the job upon client disconnection.
   * @param jobEntry the job to process.
   */
  void cancelJobOnClose(final JobEntry jobEntry) {
    final String jobUuid = jobEntry.jobUuid;
    final int tasksToSend = jobEntry.nbTasksToSend;
    final ServerTaskBundleClient clientBundle = jobEntry.getBundle();
    if (clientBundle != null) {
      final TaskBundle header = clientBundle.getJob();
      if (debugEnabled) log.debug("cancelUponClientDisconnect={} for {}", header.getSLA().isCancelUponClientDisconnect(), header);
      if (header.getSLA().isCancelUponClientDisconnect()) {
        try {
          final ServerJob job = driver.getQueue().getJob(clientBundle.getUuid());
          int taskCount = 0;
          if (job != null) {
            taskCount = job.getTaskCount();
            if (debugEnabled) log.debug("cancelling job {}", job);
            job.cancel(driver, true);
          }
          final int tasksToSend2 = jobEntry.nbTasksToSend;
          final int n = tasksToSend - tasksToSend2 - taskCount;
          if (debugEnabled) log.debug("tasksToSend={}, tasksToSend2={}, n={}, taskCount={}, serverJob={}", new Object[] {tasksToSend, tasksToSend2, n, taskCount, job});
          final JPPFStatistics stats = driver.getStatistics();
          stats.addValue(JPPFStatisticsHelper.TASK_QUEUE_COUNT, -taskCount);
          if (job != null) driver.getQueue().removeBundle(job);
        } catch(final Exception e) {
          log.error(e.getMessage(), e);
        }
      }
    } else if (jobUuid != null) {
      ServerJob job = driver.getQueue().getJob(jobUuid);
      if (job == null) job = driver.getQueue().getJobFromPriorityMap(jobUuid);
      if (debugEnabled) log.debug("case 2: removing {}, jobUuid={}", job, jobUuid);
      if ((job != null) && job.getSLA().isCancelUponClientDisconnect()) driver.getQueue().removeBundle(job);
    }
  }

  /**
   * Update the inbound and outbound traffic statistics.
   * @param message the message for which to update the statistics.
   */
  private void updateTrafficStats(final ClientMessage message) {
    if (message != null) {
      if (inSnapshot == null) inSnapshot = driver.getStatistics().getSnapshot(peer ? PEER_IN_TRAFFIC : CLIENT_IN_TRAFFIC);
      if (outSnapshot == null) outSnapshot = driver.getStatistics().getSnapshot(peer ? PEER_OUT_TRAFFIC : CLIENT_OUT_TRAFFIC);
      double value = message.getChannelReadCount();
      if (value > 0d) inSnapshot.addValues(value, 1L);
      value = message.getChannelWriteCount();
      if (value > 0d) outSnapshot.addValues(value, 1L);
    }
  }

  /**
   * Get the closed flag for this client context.
   * @return an {@link AtomicBoolean}.
   */
  AtomicBoolean getClosed() {
    return closed;
  }

  /**
   * Add the specified message to the send queue.
   * @param bundle the client bundle to which the message is associated.
   * @param message the message to add to the queue.
   * @throws Exception if any error occurs.
   */
  void offerMessageToSend(final ServerTaskBundleClient bundle, final ClientMessage message) throws Exception {
    //final JobEntry entry = entryMap.get(bundle.getUuid() + bundle.getOriginalBundleId());
    //if (entry != null) entry.completedBundles.offer(bundle);
    sendQueue.offer(message);
    server.updateInterestOps(getSelectionKey(), SelectionKey.OP_WRITE, true);
  }

  @Override
  protected ClientMessage nextMessageToSend() {
    return sendQueue.poll();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("uuid=").append(uuid);
    sb.append(", connectionUuid=").append(connectionUuid);
    sb.append(", peer=").append(peer);
    sb.append(", ssl=").append(ssl);
    sb.append(", jobEntries=").append(entryMap.size());
    sb.append(", sendQueue size=").append(sendQueue.size());
    sb.append(", interestOps=").append(getInterestOps());
    sb.append(']');
    return sb.toString();
  }
}
