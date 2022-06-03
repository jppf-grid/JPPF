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

package org.jppf.server.nio.nodeserver.async;

import static org.jppf.utils.stats.JPPFStatisticsHelper.*;

import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.jppf.execute.ExecutorStatus;
import org.jppf.io.*;
import org.jppf.job.JobReturnReason;
import org.jppf.load.balancer.*;
import org.jppf.management.JMXConnectionWrapper;
import org.jppf.node.protocol.*;
import org.jppf.node.protocol.graph.*;
import org.jppf.server.nio.AbstractTaskBundleMessage;
import org.jppf.server.nio.nodeserver.*;
import org.jppf.server.protocol.*;
import org.jppf.utils.ExceptionUtils;
import org.jppf.utils.collections.*;
import org.jppf.utils.concurrent.*;
import org.jppf.utils.stats.*;
import org.slf4j.*;

/**
 * Context or state information associated with a channel that exchanges heartbeat messages between the server and a node or client.
 * @author Laurent Cohen
 */
public class AsyncNodeContext extends BaseNodeContext {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AsyncNodeContext.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Mappings of job uuids to a collection of ids of bundles that are distributed to the node.
   */
  private final CollectionMap<String, Long> jobToBundlesIds = new ArrayListHashMap<>();
  /**
   * A map of the client bundles sent over this connection.
   */
  private final Map<String, ServerTaskBundleNode> entryMap = new ConcurrentHashMap<>();
  /**
   * This queue contains all the result bundles to send back to the client.
   */
  private final BlockingQueue<AbstractTaskBundleMessage> sendQueue = new LinkedBlockingQueue<>();
  /**
   * Lock to synchronize I/O on a local node.
   */
  private final ThreadSynchronization localNodeReadLock;
  /**
   * Lock to synchronize I/O on a local node.
   */
  private final ThreadSynchronization localNodeWriteLock;
  /**
   * The maximum number of concurrent jobs for this channel.
   */
  private final AtomicInteger maxJobs = new AtomicInteger(0);
  /**
   * Whether the job is accepting new jobs.
   */
  private AtomicBoolean acceptingNewJobs = new AtomicBoolean(true);
  /**
   * Whether an exception was already handled for this node..
   */
  private AtomicBoolean exceptionHandled = new AtomicBoolean(false);

  /**
   * @param server the server that handles this context.
   * @param socketChannel the associated socket channel.
   * @param local whether this channel context is local.
   */
  public AsyncNodeContext(final AsyncNodeNioServer server, final SocketChannel socketChannel, final boolean local) {
    super(server);
    this.socketChannel = socketChannel;
    this.local = (socketChannel == null);
    this.localNodeReadLock = local ? new ThreadSynchronization() : null;
    this.localNodeWriteLock = local ? new ThreadSynchronization() : null;
  }

  @Override
  public void handleException(final Exception exception) {
    if (!isClosed() && exceptionHandled.compareAndSet(false, true)) {
      if (log.isTraceEnabled()) log.trace("handling exception on {}\n{}\ncall stack:\n{}", this, (exception == null) ? "null" : ExceptionUtils.getStackTrace(exception), ExceptionUtils.getCallStack());
      else if (debugEnabled) log.debug("handling exception on {}\n{}", this, (exception == null) ? "null" : ExceptionUtils.getStackTrace(exception));
      final Map<String, ServerTaskBundleNode> allEntries;
      synchronized(jobToBundlesIds) {
        allEntries = new HashMap<>(entryMap);
        clear();
      }
      server.closeConnection(this);
      if (debugEnabled) log.debug("handling exception for {} node bundles of {}", allEntries.size(), this);
      for (final Map.Entry<String, ServerTaskBundleNode> entry: allEntries.entrySet()) handleException(exception, entry.getValue());
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
      if ((tmpBundle != null) && !tmpBundle.getJob().isHandshake()) {
        final boolean applyMaxResubmit = tmpBundle.getJob().getSLA().isApplyMaxResubmitsUponNodeError();
        if (debugEnabled) log.debug("applyMaxResubmit={} for {}", applyMaxResubmit, this);
        final List<DataLocation> results = new ArrayList<>(tmpBundle.getTaskList().size());
        if (!applyMaxResubmit) {
          tmpBundle.resubmit();
          for (final ServerTask task: tmpBundle.getTaskList()) {
            results.add(task.getInitialTask());
            task.resubmit();
          }
        } else {
          int count = 0;
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
        }
        tmpBundle.resultsReceived(results);
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
    taskBundle.setBundleId(bundle.getId());
    final TaskGraphInfo graphInfo = bundle.getTaskGraphInfo();
    if (!taskBundle.isHandshake()) {
      if (!isPeer()) taskBundle.removeParameter(BundleParameter.TASK_MAX_RESUBMITS);
      else {
        if (bundle.getServerJob().isPersistent()) taskBundle.setParameter(BundleParameter.ALREADY_PERSISTED_P2P, true);
        final int[] positions = new int[bundle.getTaskCount()];
        int count = 0;
        for (final ServerTask task: bundle.getTaskList()) positions[count++] = task.getPosition();
        taskBundle.setParameter(BundleParameter.TASK_POSITIONS, positions);
      }
      if (graphInfo != null) {
        final int depCount = graphInfo.getDependencies().size();
        if (debugEnabled) log.debug("there are {} dependencies for {}", depCount, this);
        taskBundle.setParameter(BundleParameter.JOB_TASK_GRAPH_INFO, graphInfo);
      }
    }
    message.addLocation(IOHelper.serializeData(taskBundle, server.getDriver().getSerializer()));
    message.addLocation(bundle.getDataProvider());
    for (ServerTask task: bundle.getTaskList()) message.addLocation(task.getInitialTask());
    if (graphInfo != null) {
      for (final PositionalElement<?> elt: graphInfo.getDependencies()) {
        final ServerTask task = (ServerTask) elt;
        if (debugEnabled) log.debug("adding dependency {} with result = {}", task, task.getResult());
        message.addLocation(task.getResult());
      }
    }
    message.setBundle(taskBundle);
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
    if (readMessage == null) readMessage = newMessage();
    readByteCount = readMessage.getChannelReadCount();
    boolean b = false;
    try {
      b = readMessage.read();
    } catch (final Exception e) {
      updateTrafficStats((AbstractTaskBundleMessage) readMessage);
      throw e;
    }
    readByteCount = readMessage.getChannelReadCount() - readByteCount;
    if (b) updateTrafficStats((AbstractTaskBundleMessage) readMessage);
    return b;
  }

  @Override
  public boolean writeMessage() throws Exception {
    writeByteCount = writeMessage.getChannelWriteCount();
    boolean b = false;
    try {
      b = writeMessage.write();
    } catch (final Exception e) {
      updateTrafficStats((AbstractTaskBundleMessage) writeMessage);
      throw e;
    }
    writeByteCount = writeMessage.getChannelWriteCount() - writeByteCount;
    if (b) updateTrafficStats((AbstractTaskBundleMessage) writeMessage);
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
      entryMap.put(uuid + bundle.getId(), bundle);
    }
  }

  /**
   * Retrieve the job entry with the specified id.
   * @param uuid the job uuid.
   * @param bundleId the id of the bundle to remove.
   * @return a {@link ServerTaskBundleNode} instance, or {@code null} if there is no entry with the specified id.
   */
  public ServerTaskBundleNode getJobEntry(final String uuid, final long bundleId) {
    return entryMap.get(uuid + bundleId);
  }

  /**
   * Remove the job entry with the specified id.
   * @param uuid the job uuid.
   * @param bundleId the id of the bundle to remove.
   * @return the removed {@link ServerTaskBundleNode} instance, or {@code null} if there is no entry with the specified id.
   */
  public ServerTaskBundleNode removeJobEntry(final String uuid, final long bundleId) {
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
    sb.append(", maxJobs=").append(getMaxJobs());
    sb.append(", jobEntries=").append(entryMap.size());
    sb.append(", sendQueue size=").append(sendQueue.size());
    sb.append(", interestOps=").append(getInterestOps());
    sb.append(", executionStatus=").append(getExecutionStatus());
    sb.append(", socketChannel=").append(socketChannel);
    sb.append(']');
    return sb.toString();
  }

  @Override
  public void close() {
    terminate();
  }

  /**
   * @return {@code true} if calling this method effectively closed the node connection, {@code false} if it was already closed.
   */
  boolean terminate() {
    final boolean res = closed.compareAndSet(false, true);
    if (res) {
      if (debugEnabled) log.debug("closing channel {}", this);
      final JMXConnectionWrapper jmx = isPeer() ? getPeerJmxConnection() : getJmxConnection();
      setJmxConnection(null);
      setPeerJmxConnection(null);
      if (jmx != null) ThreadUtils.startThread(() -> jmx.close(), "closing " + AsyncNodeContext.this);
      driver.getStatistics().addValue(JPPFStatisticsHelper.NODES, -1);
      clear();
      sendQueue.clear();
      setExecutionStatus(ExecutorStatus.FAILED);
    }
    return res;
  }

  /**
   * Clear all job entries and messages to send.
   */
  private void clear() {
    synchronized(jobToBundlesIds) {
      jobToBundlesIds.clear();
      entryMap.clear();
    }
    sendQueue.clear();
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
    addJobEntry(nodeBundle);
    if (debugEnabled) log.debug("submitting {} to {}", nodeBundle, this);
    if (getCurrentNbJobs() >= getMaxJobs()) setExecutionStatus(ExecutorStatus.EXECUTING);
    nodeBundle.setOffline(isOffline());
    nodeBundle.setChannel(this);
    nodeBundle.getJob().setExecutionStartTime(System.nanoTime());
    final AbstractTaskBundleMessage message = serializeBundle(nodeBundle);
    offerMessageToSend(nodeBundle, message);
    nodeBundle.checkTaskCount();
    return new AsyncNodeContextFuture(this, nodeBundle);
  }

  /**
   * Close and cleanup the resources used by the channel.
   */
  void cleanup() {
    if (debugEnabled) log.debug("handling cleanup for {}", this);
    if (getReservationTansition() == NodeReservationHandler.Transition.REMOVE) server.getNodeReservationHandler().removeReservation(this);
    final Bundler<?> bundler = getBundler();
    if (bundler != null) {
      bundler.dispose();
      if (bundler instanceof ContextAwareness) ((ContextAwareness) bundler).setJPPFContext(null);
    }
    setReadMessage(null);
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
   * @return a lock used to synchronize input I/O with a local node.
   */
  public ThreadSynchronization getLocalNodeReadLock() {
    return localNodeReadLock;
  }

  /**
   * @return a lock used to synchronize output I/O with a local node.
   */
  public ThreadSynchronization getLocalNodeWriteLock() {
    return localNodeWriteLock;
  }

  @Override
  public int getMaxJobs() {
    return maxJobs.get();
  }

  /**
   * Set the maximum number of concurrent jobs for this channel.
   * @param maxJobs the max number of jobs to set.
   */
  public void setMaxJobs(final int maxJobs) {
    this.maxJobs.set(maxJobs);
  }

  /**
   * Get the number of dispatches to the node for the specified job.
   * @param jobUuid the uuid of the job to check.
   * @return the number of dispatches of the job.
   */
  public int getNbBundlesForJob(final String jobUuid) {
    synchronized(jobToBundlesIds) {
      final Collection<Long> bundleIds = jobToBundlesIds.getValues(jobUuid);
      return (bundleIds == null) ? 0 : bundleIds.size();
    }
  }

  /**
   * @return whether the job is accepting new jobs.
   */
  public boolean isAcceptingNewJobs() {
    return acceptingNewJobs.get();
  }

  /**
   * @param acceptingNewJobs whether the job is accepting new jobs.
   */
  public void setAcceptingNewJobs(final boolean acceptingNewJobs) {
    if (debugEnabled) log.debug("node is {}accepting new jobs: {}", acceptingNewJobs ? "" : "not ", this);
    this.acceptingNewJobs.set(acceptingNewJobs);
  }
}
