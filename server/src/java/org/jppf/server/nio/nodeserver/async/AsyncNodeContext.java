/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

package org.jppf.server.nio.nodeserver.async;

import static org.jppf.utils.stats.JPPFStatisticsHelper.*;

import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.execute.ExecutorStatus;
import org.jppf.io.*;
import org.jppf.job.JobReturnReason;
import org.jppf.load.balancer.*;
import org.jppf.management.JMXConnectionWrapper;
import org.jppf.nio.*;
import org.jppf.node.protocol.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.AbstractTaskBundleMessage;
import org.jppf.server.nio.nodeserver.*;
import org.jppf.server.protocol.*;
import org.jppf.utils.*;
import org.jppf.utils.collections.*;
import org.jppf.utils.concurrent.*;
import org.jppf.utils.stats.*;
import org.slf4j.*;

/**
 * Context or state information associated with a channel that exchanges heartbeat messages between the server and a node or client.
 * @author Laurent Cohen
 */
public class AsyncNodeContext extends StatelessNioContext implements AbstractBaseNodeContext<EmptyEnum> {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AsyncNodeContext.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Reference to the driver.
   */
  final JPPFDriver driver;
  /**
   * The server that handles this context.
   */
  private final AsyncNodeNioServer server;
  /**
   * Mappings of job uuids to a collection of ids of bundles that are distributed to the node.
   */
  private final CollectionMap<String, Long> jobToBundlesIds = new ArrayListHashMap<>();
  /**
   * A map of the client bundles sent over this connection.
   */
  private final Map<String, NodeJobEntry> entryMap = new ConcurrentHashMap<>();
  /**
   * This queue contains all the result bundles to send back to the client.
   */
  private final BlockingQueue<AbstractTaskBundleMessage> sendQueue = new LinkedBlockingQueue<>();
  /**
   * Common node attributes and operations.
   */
  final NodeContextAttributes attributes;
  /**
   * Lock to synchronize I/O on a local node.
   */
  private final ThreadSynchronization localNodeLock;

  /**
   * @param server the server that handles this context.
   * @param socketChannel the associated socket channel.
   * @param local whether this channel context is local.
   */
  public AsyncNodeContext(final AsyncNodeNioServer server, final SocketChannel socketChannel, final boolean local) {
    this.server = server;
    this.driver = server.getDriver();
    this.socketChannel = socketChannel;
    this.local = (socketChannel == null);
    this.localNodeLock = local ? new ThreadSynchronization() : null;
    this.attributes = new NodeContextAttributes(this, server.getBundlerHandler(), server);
    this.attributes.setDriver(server.getDriver());
  }

  @Override
  public void handleException(final Exception exception) {
    if (!isClosed()) {
      final Map<String, NodeJobEntry> allEntries;
      synchronized(jobToBundlesIds) {
        allEntries = new HashMap<>(entryMap);
      }
      if (debugEnabled) log.debug("handling exception on {}\n{}", this, ((exception == null) ? ExceptionUtils.getCallStack() : ExceptionUtils.getStackTrace(exception)));
      server.closeConnection(this);
      for (final Map.Entry<String, NodeJobEntry> entry: allEntries.entrySet()) {
        handleException(exception, entry.getValue().nodeBundle);
      }
    }
  }

  /**
   * 
   * @param exception the exception.
   * @param tmpBundle the bundle being processed.
   */
  void handleException(final Exception exception, final ServerTaskBundleNode tmpBundle) {
    try {
      if (tmpBundle != null) {
        server.getDispatchExpirationHandler().cancelAction(ServerTaskBundleNode.makeKey(tmpBundle));
        tmpBundle.setJobReturnReason(JobReturnReason.NODE_CHANNEL_ERROR);
        tmpBundle.taskCompleted(exception);
      }
      boolean callTaskCompleted = true;
      if ((tmpBundle != null) && !tmpBundle.getJob().isHandshake()) {
        boolean applyMaxResubmit = tmpBundle.getJob().getMetadata().getParameter("jppf.job.applyMaxResubmitOnNodeError", false);
        applyMaxResubmit |= tmpBundle.getJob().getSLA().isApplyMaxResubmitsUponNodeError();
        if (debugEnabled) log.debug("applyMaxResubmit={} for {}", applyMaxResubmit, this);
        if (!applyMaxResubmit) tmpBundle.resubmit();
        else {
          int count = 0;
          final List<DataLocation> results = new ArrayList<>(tmpBundle.getTaskList().size());
          for (final ServerTask task: tmpBundle.getTaskList()) {
            results.add(task.getInitialTask());
            final int max = tmpBundle.getJob().getSLA().getMaxTaskResubmits();
            if (task.incResubmitCount() <= max) {
              task.resubmit();
              count++;
            }
          }
          if (debugEnabled) log.debug("resubmit count={} for {}", count, this);
          if (count > 0) updateStatsUponTaskResubmit(count);
          tmpBundle.resultsReceived(results);
          callTaskCompleted = false;
        }
        if (callTaskCompleted) tmpBundle.getClientJob().taskCompleted(tmpBundle, exception);
        updateStatsUponTaskResubmit(tmpBundle.getTaskCount());
      }
    } catch (final Exception e) {
      log.error("error in handleException() for " + this + " : " , e);
    }
  }

  /**
   * Serialize specified bundle into a message to send.
   * @param bundle the byndle to process.
   * @return the created message.
   * @throws Exception if any error occurs.
   */
  public AbstractTaskBundleMessage serializeBundle(final ServerTaskBundleNode bundle) throws Exception {
    bundle.checkTaskCount();
    final TaskBundle taskBundle = bundle.getJob();
    final AbstractTaskBundleMessage message = newMessage();
    taskBundle.setParameter(BundleParameter.NODE_BUNDLE_ID, bundle.getId());
    if (!taskBundle.isHandshake()) {
      if (!isPeer()) taskBundle.removeParameter(BundleParameter.TASK_MAX_RESUBMITS);
      else if (bundle.getServerJob().isPersistent()) taskBundle.setParameter(BundleParameter.ALREADY_PERSISTED_P2P, true);
    }
    message.addLocation(IOHelper.serializeData(taskBundle, server.getDriver().getSerializer()));
    message.addLocation(bundle.getDataProvider());
    for (ServerTask task: bundle.getTaskList()) message.addLocation(task.getInitialTask());
    message.setBundle(bundle.getJob());
    return message;
  }

  /**
   * Deserialize a task bundle from the message read into this buffer.
   * @param message the message to process.
   * @return a pairing of the received result head and the serialized tasks.
   * @throws Exception if an error occurs during the deserialization.
   */
  public NodeBundleResults deserializeBundle(final AbstractTaskBundleMessage message) throws Exception {
    final List<DataLocation> locations = message.getLocations();
    if (message.getBundle() == null) message.setBundle((TaskBundle) IOHelper.unwrappedData(locations.get(0)));
    final TaskBundle bundle = message.getBundle();
    final List<DataLocation> tasks = new ArrayList<>();
    if (locations.size() > 1) {
      for (int i=1; i<locations.size(); i++) tasks.add(locations.get(i));
    }
    return new NodeBundleResults(bundle, tasks);
  }

  /**
   * Create a new message.
   * @return an {@link AbstractTaskBundleMessage} instance.
   */
  public AbstractTaskBundleMessage newMessage() {
    return isLocal() ? new LocalNodeMessage(this) : new RemoteNodeMessage(this);
  }

  @Override
  public boolean readMessage() throws Exception {
    if (message == null) message = newMessage();
    final NioMessage msg = message;
    byteCount = message.getChannelReadCount();
    boolean b = false;
    try {
      b = msg.read();
    } catch (final Exception e) {
      updateTrafficStats((AbstractTaskBundleMessage) msg);
      throw e;
    }
    byteCount = msg.getChannelReadCount() - byteCount;
    if (b) updateTrafficStats((AbstractTaskBundleMessage) msg);
    return b;
  }

  @Override
  public boolean writeMessage() throws Exception {
    final NioMessage msg = writeMessage;
    writeByteCount = msg.getChannelWriteCount();
    boolean b = false;
    try {
      b = msg.write();
    } catch (final Exception e) {
      updateTrafficStats((AbstractTaskBundleMessage) msg);
      throw e;
    }
    writeByteCount = msg.getChannelWriteCount() - writeByteCount;
    if (b) updateTrafficStats((AbstractTaskBundleMessage) msg);
    return b;
  }

  /**
   * Add a new job to the job map.
   * @param bundle the job to add.
   */
  public void addJobEntry(final ServerTaskBundleNode bundle) {
    final String uuid = bundle.getJob().getUuid();
    synchronized(jobToBundlesIds) {
      jobToBundlesIds.putValue(uuid, bundle.getId());
      entryMap.put(uuid + bundle.getId(), new NodeJobEntry(bundle));
    }
  }

  /**
   * Retrieve the job entry with the specified id.
   * @param uuid the job uuid.
   * @param bundleId the id of the bundle to remove.
   * @return a {@link NodeJobEntry} instance, or {@code null} if there is no entry with the specified id.
   */
  public NodeJobEntry getJobEntry(final String uuid, final long bundleId) {
    return entryMap.get(uuid + bundleId);
  }

  /**
   * Remove the job entry with the specified id.
   * @param uuid the job uuid.
   * @param bundleId the id of the bundle to remove.
   * @return the removed {@link NodeJobEntry} instance, or {@code null} if there is no entry with the specified id.
   */
  public NodeJobEntry removeJobEntry(final String uuid, final long bundleId) {
    if (debugEnabled) log.debug("removing job entry for uuid={}, bundleId={}", uuid, bundleId);
    synchronized(jobToBundlesIds) {
      jobToBundlesIds.removeValue(uuid, bundleId);
      return entryMap.remove(uuid + bundleId);
    }
  }

  /**
   * Update the inbound and outbound traffic statistics.
   * @param message the message for which to update the statistics.
   */
  private void updateTrafficStats(final AbstractTaskBundleMessage message) {
    if (message != null) {
      if (inSnapshot == null) inSnapshot = driver.getStatistics().getSnapshot(peer ? PEER_IN_TRAFFIC : NODE_IN_TRAFFIC);
      if (outSnapshot == null) outSnapshot = driver.getStatistics().getSnapshot(peer ? PEER_OUT_TRAFFIC : NODE_OUT_TRAFFIC);
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
  void offerMessageToSend(final ServerTaskBundleNode bundle, final AbstractTaskBundleMessage message) throws Exception {
    sendQueue.offer(message);
    if (!local) server.updateInterestOps(getSelectionKey(), SelectionKey.OP_WRITE, true);
  }

  @Override
  protected AbstractTaskBundleMessage nextMessageToSend() {
    return sendQueue.poll();
  }

  /**
   * Take a message from the pending queue, waiting if necessary.
   * @return a {@link AbstractTaskBundleMessage} instance.
   * @throws InterruptedException if the thread is interrupted.
   */
  public AbstractTaskBundleMessage takeNextMessageToSend() throws InterruptedException {
    return sendQueue.take();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("uuid=").append(uuid);
    sb.append(", peer=").append(peer);
    sb.append(", ssl=").append(ssl);
    sb.append(", local=").append(local);
    sb.append(", offline=").append(isOffline());
    sb.append(", jobEntries=").append(entryMap.size());
    sb.append(", sendQueue size=").append(sendQueue.size());
    sb.append(", interestOps=").append(getInterestOps());
    sb.append(']');
    return sb.toString();
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      if (debugEnabled) log.debug("closing channel {}", getChannel());
      final JMXConnectionWrapper jmx = isPeer() ? getPeerJmxConnection() : getJmxConnection();
      setJmxConnection(null);
      setPeerJmxConnection(null);
      if (jmx != null) ThreadUtils.startThread(() -> jmx.close(), "closing " + AsyncNodeContext.this);
      driver.getStatistics().addValue(JPPFStatisticsHelper.NODES, -1);
      synchronized(jobToBundlesIds) {
        jobToBundlesIds.clear();
        entryMap.clear();
      }
      sendQueue.clear();
      setExecutionStatus(ExecutorStatus.FAILED);
    }
  }

  @Override
  public Object getMonitor() {
    return this;
  }

  @Override
  public int getCurrentNbJobs() {
    synchronized(jobToBundlesIds) {
      return entryMap.size();
    }
  }

  @Override
  public Future<?> submit(final ServerTaskBundleNode nodeBundle) throws Exception {
    setExecutionStatus(ExecutorStatus.EXECUTING);
    addJobEntry(nodeBundle);
    nodeBundle.setOffline(isOffline());
    nodeBundle.setChannel(this);
    nodeBundle.getJob().setExecutionStartTime(System.nanoTime());
    final AbstractTaskBundleMessage message = serializeBundle(nodeBundle);
    offerMessageToSend(nodeBundle, message);
    nodeBundle.checkTaskCount();
    return new AsyncNodeContextFuture(this, nodeBundle);
  }

  @Override
  public boolean cancelJob(final String jobId, final boolean requeue) throws Exception {
    if (debugEnabled) log.debug("cancelling job uuid={} from {}, jmxConnection={}, peerJmxConnection={}", jobId, this, getJmxConnection(), getPeerJmxConnection());
    if (isOffline()) return false;
    JPPFVoidCallable cancelCallback = null;
    if (!isPeer() && (getJmxConnection() != null) && getJmxConnection().isConnected()) cancelCallback = () -> getJmxConnection().cancelJob(jobId, requeue);
    else if (isPeer() && (getPeerJmxConnection() != null) && getPeerJmxConnection().isConnected()) cancelCallback = () -> getPeerJmxConnection().cancelJob(jobId);
    if (cancelCallback != null) {
      try {
        cancelCallback.call();
      } catch (final Exception e) {
        if (debugEnabled) log.debug(e.getMessage(), e);
        else log.warn(ExceptionUtils.getMessage(e));
        throw e;
      }
      return true;
    }
    return false;
  }

  /**
   * Close and cleanup the resources used by the channel.
   */
  void cleanup() {
    if (debugEnabled) log.debug("handling cleanup for {}", channel);
    if (getReservationTansition() == NodeReservationHandler.Transition.REMOVE) server.getNodeReservationHandler().removeReservation(this);
    final Bundler<?> bundler = getBundler();
    if (bundler != null) {
      bundler.dispose();
      if (bundler instanceof ContextAwareness) ((ContextAwareness) bundler).setJPPFContext(null);
    }
    if (getOnClose() != null) getOnClose().run();
    setMessage(null);
  }

  @Override
  public NodeContextAttributes getAttributes() {
    return attributes;
  }

  /**
   * Update the statistics to account for the specified number of resubmitted tasks.
   * @param resubmittedTaskCount the number of tasks to resubmit.
   */
  void updateStatsUponTaskResubmit(final int resubmittedTaskCount) {
    final JPPFStatistics stats = server.getDriver().getStatistics();
    stats.addValue(JPPFStatisticsHelper.TASK_QUEUE_COUNT, resubmittedTaskCount);
  }

  /**
   * @return the server handling this channel context.
   */
  public AsyncNodeNioServer getServer() {
    return server;
  }

  /**
   * @return a lock used to synchronize I/O with a local node.
   */
  public ThreadSynchronization getLocalNodeLock() {
    return localNodeLock;
  }
}
