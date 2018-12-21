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

package org.jppf.server.nio.nodeserver;

import static org.jppf.utils.stats.JPPFStatisticsHelper.*;

import java.util.*;
import java.util.concurrent.Future;

import org.jppf.execute.ExecutorStatus;
import org.jppf.io.*;
import org.jppf.job.JobReturnReason;
import org.jppf.load.balancer.*;
import org.jppf.management.JMXConnectionWrapper;
import org.jppf.nio.*;
import org.jppf.node.protocol.*;
import org.jppf.server.nio.AbstractTaskBundleMessage;
import org.jppf.server.protocol.*;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.ThreadUtils;
import org.jppf.utils.stats.*;
import org.slf4j.*;

/**
 * Context associated with a channel serving tasks to a node.
 * @author Laurent Cohen
 */
public abstract class AbstractNodeContext extends AbstractNioContext<NodeState> implements AbstractBaseNodeContext<NodeState> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractNodeContext.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The task bundle to send or receive.
   */
  protected ServerTaskBundleNode bundle;
  /**
   * Performs all operations that relate to channel states.
   */
  final NodeNioServer server;
  /**
   * 
   */
  final NodeContextAttributes attributes;

  /**
   * Initialized abstract node context.
   * @param server the NIO server that created this context.
   */
  protected AbstractNodeContext(final NodeNioServer server) {
    this.server = server;
    this.attributes = new NodeContextAttributes(this, server.getBundlerHandler(), server);
    this.attributes.setDriver(server.getDriver());
  }

  /**
   * Get the task bundle to send or receive.
   * @return a {@code ServerJob} instance.
   */
  public ServerTaskBundleNode getBundle() {
    return bundle;
  }

  /**
   * Set the task bundle to send or receive.
   * @param bundle a {@link ServerTaskBundleNode} instance.
   */
  public void setBundle(final ServerTaskBundleNode bundle) {
    this.bundle = bundle;
    if (bundle != null) bundle.checkTaskCount();
  }

  @Override
  public void handleException(final Exception exception) {
    if (closed.compareAndSet(false, true)) {
      if (debugEnabled) {
        if (exception != null) log.debug("handling '{}' for {}", ExceptionUtils.getMessage(exception), channel);
        else log.debug("handling null for {}, call stack:\n{}", channel, ExceptionUtils.getCallStack());
      }
      final ServerTaskBundleNode tmpBundle = bundle;
      cleanup();
      handleException(exception, tmpBundle);
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
    if (bundle != null) setBundle(null);
    setMessage(null);
  }

  /**
   * Serialize this context's bundle into a byte buffer.
   * @throws Exception if any error occurs.
   */
  void serializeBundle() throws Exception {
    bundle.checkTaskCount();
    final TaskBundle taskBundle = bundle.getJob();
    final AbstractTaskBundleMessage message = newMessage();
    if (!taskBundle.isHandshake()) {
      taskBundle.setParameter(BundleParameter.NODE_BUNDLE_ID, bundle.getId());
      if (!isPeer()) taskBundle.removeParameter(BundleParameter.TASK_MAX_RESUBMITS);
      else if (bundle.getServerJob().isPersistent()) taskBundle.setParameter(BundleParameter.ALREADY_PERSISTED_P2P, true);
    }
    message.addLocation(IOHelper.serializeData(taskBundle, server.getDriver().getSerializer()));
    message.addLocation(bundle.getDataProvider());
    for (ServerTask task: bundle.getTaskList()) message.addLocation(task.getInitialTask());
    message.setBundle(bundle.getJob());
    setMessage(message);
  }

  /**
   * Deserialize a task bundle from the message read into this buffer.
   * @return a pairing of the received result head and the serialized tasks.
   * @throws Exception if an error occurs during the deserialization.
   */
  NodeBundleResults deserializeBundle() throws Exception {
    final List<DataLocation> locations = ((AbstractTaskBundleMessage) message).getLocations();
    final TaskBundle bundle = ((AbstractTaskBundleMessage) message).getBundle();
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
  public abstract AbstractTaskBundleMessage newMessage();

  @Override
  public boolean readMessage(final ChannelWrapper<?> channel) throws Exception {
    if (message == null) message = newMessage();
    boolean b = false;
    try {
      b = message.read();
    } catch (final Exception e) {
      updateTrafficStats();
      throw e;
    }
    if (b) updateTrafficStats();
    return b;
  }

  @Override
  public boolean writeMessage(final ChannelWrapper<?> channel) throws Exception {
    boolean b = false;
    try {
      b = message.write();
    } catch (final Exception e) {
      updateTrafficStats();
      throw e;
    }
    if (b) updateTrafficStats();
    return b;
  }

  @Override
  public boolean setState(final NodeState state) {
    final ExecutorStatus oldExecutionStatus = getExecutionStatus();
    ExecutorStatus newExecutionStatus = ExecutorStatus.DISABLED;
    final boolean b = super.setState(state);
    switch (state) {
      case IDLE:
        newExecutionStatus = (getChannel().isOpen() && isEnabled()) ? ExecutorStatus.ACTIVE : ExecutorStatus.FAILED;
        break;
      case SENDING_BUNDLE:
      case WAITING_RESULTS:
        newExecutionStatus = ExecutorStatus.EXECUTING;
        break;
    }
    if (newExecutionStatus != oldExecutionStatus) {
      setExecutionStatus(newExecutionStatus);
      if (debugEnabled) log.debug("changing state to {}, newStatus={}, oldStatus={}, node={}", state, newExecutionStatus, oldExecutionStatus, this);
      fireExecutionStatusChanged(oldExecutionStatus, newExecutionStatus);
    }
    return b;
  }

  @Override
  public void close() throws Exception {
    if (debugEnabled) log.debug("closing channel {}", getChannel());
    try {
      getChannel().close();
    } catch(final Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
    }
    final JMXConnectionWrapper jmx = isPeer() ? getPeerJmxConnection() : getJmxConnection();
    setJmxConnection(null);
    setPeerJmxConnection(null);
    if (jmx != null) {
      ThreadUtils.startThread(() -> {
        try {
          jmx.close();
        } catch (@SuppressWarnings("unused") final Exception ignore) {
        }
      }, "closing " + getChannel());
    }
  }

  @Override
  public Object getMonitor() {
    return getChannel();
  }

  @Override
  public boolean cancelJob(final String jobId, final boolean requeue) throws Exception {
    if (debugEnabled) log.debug("cancelling job uuid={} from {}, jmxConnection={}, peerJmxConnection={}", jobId, this, getJmxConnection(), getPeerJmxConnection());
    if (!isPeer() && (getJmxConnection() != null) && getJmxConnection().isConnected()) {
      try {
        getJmxConnection().cancelJob(jobId, requeue);
      } catch (final Exception e) {
        if (debugEnabled) log.debug(e.getMessage(), e);
        else log.warn(ExceptionUtils.getMessage(e));
        throw e;
      }
      return true;
    } else if (isPeer() && (getPeerJmxConnection() != null) && getPeerJmxConnection().isConnected()) {
      try {
        getPeerJmxConnection().cancelJob(jobId);
      } catch (final Exception e) {
        if (debugEnabled) log.debug(e.getMessage(), e);
        else log.warn(ExceptionUtils.getMessage(e));
        throw e;
      }
      return true;
    }
    return false;
  }

  @Override
  public Future<?> submit(final ServerTaskBundleNode nodeBundle) throws Exception {
    setBundle(nodeBundle);
    nodeBundle.setOffline(isOffline());
    nodeBundle.setChannel(this);
    server.getTransitionManager().transitionChannel(getChannel(), NodeTransition.TO_SENDING_BUNDLE);
    if (getChannel().getSelector() != null) getChannel().getSelector().wakeUp();
    nodeBundle.checkTaskCount();
    return new NodeContextFuture(this);
  }

  /**
   * Update the inbound and outbound traffic statistics.
   */
  private void updateTrafficStats() {
    if (message != null) {
      if (inSnapshot == null) inSnapshot = server.getDriver().getStatistics().getSnapshot(peer ? PEER_IN_TRAFFIC : NODE_IN_TRAFFIC);
      if (outSnapshot == null) outSnapshot = server.getDriver().getStatistics().getSnapshot(peer ? PEER_OUT_TRAFFIC : NODE_OUT_TRAFFIC);
      double value = message.getChannelReadCount();
      if (value > 0d) inSnapshot.addValues(value, 1L);
      value = message.getChannelWriteCount();
      if (value > 0d) outSnapshot.addValues(value, 1L);
    }
  }

  /**
   * Update the statistics to account for the specified number of resubmitted tasks.
   * @param resubmittedTaskCount the number of tasks to resubmit.
   */
  void updateStatsUponTaskResubmit(final int resubmittedTaskCount) {
    final JPPFStatistics stats = server.getDriver().getStatistics();
    stats.addValue(JPPFStatisticsHelper.TASK_QUEUE_COUNT, resubmittedTaskCount);
  }

  @Override
  public int getCurrentNbJobs() {
    return (bundle == null) ? 0 : 1;
  }

  @Override
  public NodeContextAttributes getAttributes() {
    return attributes;
  }
}
