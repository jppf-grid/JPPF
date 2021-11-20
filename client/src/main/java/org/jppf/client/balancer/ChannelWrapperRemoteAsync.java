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

package org.jppf.client.balancer;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.jppf.JPPFException;
import org.jppf.client.*;
import org.jppf.client.event.ClientConnectionStatusListener;
import org.jppf.load.balancer.BundlerHelper;
import org.jppf.node.protocol.*;
import org.jppf.serialization.ObjectSerializer;
import org.jppf.utils.ExceptionUtils;
import org.slf4j.*;

/**
 * Context associated with a remote channel serving state and tasks submission.
 * @author Martin JANDA
 * @author Laurent Cohen
 */
public class ChannelWrapperRemoteAsync extends AbstractChannelWrapperRemote {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ChannelWrapperRemoteAsync.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The queue of jobs submitted to this connection.
   */
  private final BlockingQueue<ClientTaskBundle> bundleQueue = new LinkedBlockingQueue<>();
  /**
   * Contains the client bundles currently awaiting results from the driver.
   */
  private final Map<Long, RemoteResponse> responseMap = new ConcurrentHashMap<>();
  /**
   * Whether this wrapper's initialization was performed.
   */
  private boolean initDone;
  /**
   * Futures for the sending and receiving tasks.
   */
  private final List<Future<?>> futures = new ArrayList<>();
  /**
   * For synchronization.
   */
  private final Object statusLock = new Object();
  /**
   * Notifies waiting threads that the connection status has changed.
   */
  private final ClientConnectionStatusListener listener = event -> {
    synchronized(statusLock) {
      statusLock.notifyAll();
    }
  };
  /**
   * Used to synchronize on the jobs resubmission.
   */
  private final Object resubmitLock = new Object();

  /**
   * Default initializer for remote channel wrapper.
   * @param channel to the driver to use.
   */
  public ChannelWrapperRemoteAsync(final JPPFClientConnection channel) {
    super(channel);
    channel.addClientConnectionStatusListener(listener);
  }

  @Override
  public void initChannelID() {
    super.initChannelID();
    if (!initDone) {
      initDone = true;
      final ExecutorService executor = this.channel.getConnectionPool().getClient().getExecutor();
      futures.add(executor.submit(new RemoteSender()));
      futures.add(executor.submit(new RemoteReceiver()));
    }
  }

  @Override
  public Future<?> submit(final ClientTaskBundle bundle) {
    if (debugEnabled) log.debug("submitting {} to {}", bundle, this);
    if (!channel.isClosed()) {
      jobCount.incrementAndGet();
      if (getCurrentNbJobs() >= getMaxJobs()) setStatus(JPPFClientConnectionStatus.EXECUTING);
      bundleQueue.offer(bundle);
      if (debugEnabled) log.debug("submitted {} to {}", bundle, this);
    } else {
      if (debugEnabled) log.debug("resubmitting {}", bundle);
      resubmitBundle(bundle, null);
    }
    return null;
  }

  @Override
  public boolean isLocal() {
    return false;
  }

  /**
   * Thread which sends the tasks to the driver.
   * Also handles exceptions and failover and recovery scenarios when the driver connection breaks.
   */
  private class RemoteSender implements Runnable {
    /**
     * Logger for this class.
     */
    private Logger thisLog = LoggerFactory.getLogger(RemoteSender.class);
    /**
     * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
     */
    private boolean thisDebugEnabled = thisLog.isDebugEnabled();

    @Override
    public void run() {
      if (debugEnabled) log.debug("entering sender loop for {}", ChannelWrapperRemoteAsync.this);
      final JPPFClientConnectionImpl connection = (JPPFClientConnectionImpl) channel;
      while (!channel.isClosed()) {
        ClientTaskBundle clientBundle = null;
        try {
          awaitStatus();
          clientBundle = bundleQueue.take();
          final long bundleId = clientBundle.getBundleId();
          final List<Task<?>> tasks = clientBundle.getTasksL();
          if (thisDebugEnabled) {
            final int size = tasks.size();
            final int[] positions = new int[size];
            for (int i=0; i<size; i++) positions[i] = tasks.get(i).getPosition();
            thisLog.debug("{} executing {} tasks of job {} with bundleId = {}, positions={}", ChannelWrapperRemoteAsync.this, size, clientBundle, bundleId, Arrays.toString(positions));
          }
          final Collection<ClassLoader> loaders = registerClassLoaders(clientBundle.getUuid(), tasks);
          final TaskBundle bundle = createBundle(clientBundle, bundleId);
          bundle.setUuid(uuid);
          bundle.setInitialTaskCount(clientBundle.getClientJob().initialTaskCount);
          final ClassLoader cl = loaders.isEmpty() ? null : loaders.iterator().next();
          final ObjectSerializer ser = connection.makeHelper(cl).getSerializer();
          final long start = System.nanoTime();
          final RemoteResponse response = new RemoteResponse(clientBundle, 0, cl, ser, start);
          synchronized(response) {
            if (response.currentCount < response.taskCount) responseMap.put(bundleId, response);
            if (thisDebugEnabled) thisLog.debug("{} sending {}", ChannelWrapperRemoteAsync.this, clientBundle);
            final List<Task<?>> notSerializableTasks = connection.sendTasks(ser, cl, bundle, clientBundle);
            clientBundle.jobDispatched(ChannelWrapperRemoteAsync.this);
            if (!notSerializableTasks.isEmpty()) {
              if (thisDebugEnabled) thisLog.debug("got {} non-serializable tasks for {}", notSerializableTasks.size(), clientBundle);
              response.currentCount = notSerializableTasks.size();
              clientBundle.resultsReceived(notSerializableTasks);
            }
            if (response.currentCount >= response.taskCount) handleBundleComplete(clientBundle, null);
          }
        } catch (final Throwable t) {
          handleThrowable(clientBundle, t, true);
        }
      }
      if (debugEnabled) log.debug("exiting sender loop for {}", ChannelWrapperRemoteAsync.this);
    }
  }

  /**
   * Thread which receives the task results from the driver.
   */
  private class RemoteReceiver implements Runnable {
    /**
     * Logger for this class.
     */
    private Logger thisLog = LoggerFactory.getLogger(RemoteReceiver.class);
    /**
     * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
     */
    private boolean thisDebugEnabled = thisLog.isDebugEnabled();

    @Override
    public void run() {
      if (debugEnabled) log.debug("entering receiver loop for {}", ChannelWrapperRemoteAsync.this);
      final JPPFClientConnectionImpl connection = (JPPFClientConnectionImpl) channel;
      while (!channel.isClosed()) {
        ClientTaskBundle clientBundle = null;
        Exception exception = null;
        boolean complete = false;
        try {
          awaitStatus();
          final TaskBundle bundle = connection.receiveHeader(null, null);
          if (bundle == null) {
            thisLog.debug("received null header");
            continue;
            //throw new IllegalStateException("received null header");
          }
          if (thisDebugEnabled) thisLog.debug("received bundle {}", bundle);
          final long bundleId = bundle.getParameter(BundleParameter.CLIENT_BUNDLE_ID);
          final RemoteResponse response = responseMap.remove(bundleId);
          if (response == null) {
            thisLog.debug("response object no longer in queue for bundleId = {}", bundleId);
            continue;
          }
          synchronized(response) {
            clientBundle = response.clientBundle;
            final List<Task<?>> tasks = connection.receiveTasks(bundle, response.ser, response.cl);
            if (thisDebugEnabled) thisLog.debug("received {} tasks for {}", tasks.size(), clientBundle);
            response.handleResults(tasks);
            if (response.currentCount < response.taskCount) {
              responseMap.put(bundleId, response);
            } else {
              complete = true;
              BundlerHelper.updateBundler(bundler, tasks.size(), response.elapsed);
              getLoadBalancerPersistenceManager().storeBundler(channelID, bundler, bundlerAlgorithm);
            }
          }
        } catch (final Throwable t) {
          exception = handleThrowable(clientBundle, t, false);
        } finally {
          if (complete) handleBundleComplete(clientBundle, exception);
        }
      }
      if (debugEnabled) log.debug("exiting receiver loop for {}", ChannelWrapperRemoteAsync.this);
    }
  }

  /**
   * Sends the tasks to the driver and gets the results back.
   * Also handles exceptions and failover and recovery scenarios when the driver connection breaks.
   */
  private static class RemoteResponse {
    /**
     * The task bundle to execute.
     */
    final ClientTaskBundle clientBundle;
    /**
     * The number of tasks in the job.
     */
    final int taskCount;
    /**
     * The class loader to use to deserialize the results.
     */
    final ClassLoader cl;
    /**
     * The serializer to use to deserialize the results.
     */
    final ObjectSerializer ser;
    /**
     * Round-trip start time for the job in nanos.
     */
    final long start;
    /**
     * Round-trip elapsed time for the job in nanos.
     */
    long elapsed;
    /**
     * The current count of processed tasks.
     */
    int currentCount;

    /**
     * Initialize this runnable for remote execution.
     * @param clientBundle the job being processed.
     * @param notSerializableTasksCount the number of tasks that could not be serialized, possibly 0.
     * @param cl the class loader to use to deserialize the results.
     * @param ser the serializer to use to deserialize the results.
     * @param start the time at which the bundle was sent.
     */
    public RemoteResponse(final ClientTaskBundle clientBundle, final int notSerializableTasksCount, final ClassLoader cl, final ObjectSerializer ser, final long start) {
      this.clientBundle = clientBundle;
      this.taskCount = clientBundle.getTasksL().size();
      this.currentCount = notSerializableTasksCount;
      this.cl = cl;
      this.ser = ser;
      this.start = start;
    }

    /**
     * Handle a set of results.
     * @param results the results to process.
     */
    void handleResults(final List<Task<?>> results) {
      elapsed = System.nanoTime() - start;
      final int n = results.size();
      currentCount += n;
      if (debugEnabled) log.debug("received {} tasks from server{}", n, (n > 0 ? ", first position=" + results.get(0).getPosition() : ""));
      clientBundle.resultsReceived(results);
    }
  }

  /**
   * Handle a throwable raised while processing a job.
   * @param clientBundle the job being processed.
   * @param t the Throwable to handle.
   * @param fromSender {@code true} if this method is called from the sender thread, {@code false} if it is called from the receiver thread.
   * @return an eventual exception to use in job results processing.
   */
  private Exception handleThrowable(final ClientTaskBundle clientBundle, final Throwable t, final boolean fromSender) {
    final String from = fromSender ? "sender" : "receiver";
    if (debugEnabled) log.debug("handling throwable from {} for {}:\nchannel = {}", from, clientBundle, this, t);
    final boolean channelClosed = channel.isClosed();
    if (debugEnabled) log.debug("from {}, channelClosed={}, resetting={}", from, channelClosed, resetting);
    if (!channelClosed) {
      final String jobMsg = (clientBundle == null) ? "" : " while handling job " + clientBundle;
      log.warn("from {}, throwable was raised{} on channel {}: {}", from, jobMsg, this, ExceptionUtils.getMessage(t));
    }
    final Exception exception = (t == null) ? null : ((t instanceof Exception) ? (Exception) t : new JPPFException(t));
    try {
      if (t instanceof NotSerializableException) {
        if (clientBundle != null) clientBundle.resultsReceived(t);
      } else {
        reconnect();
        if (clientBundle != null) {
          if (debugEnabled) log.debug("from {}, resubmitting {}", from, clientBundle);
          resubmitBundle(clientBundle, null);
        }
        resubmitQueueBundles();
      }
    } finally {
      if ((clientBundle != null)) {
        if (jobCount.get() > 0) jobCount.decrementAndGet();
        if ((getStatus() == JPPFClientConnectionStatus.EXECUTING) && (getCurrentNbJobs() < getMaxJobs())) setStatus(JPPFClientConnectionStatus.ACTIVE);
      }
    }
    return exception;
  }

  /**
   * Resubmit all queued bundles along with those awaiting a response.
   */
  private void resubmitQueueBundles() {
    synchronized(resubmitLock) {
      if (debugEnabled) log.debug("resubmitting all queued jobs of {}", this);
      final Set<ClientTaskBundle> resubmitted = new HashSet<>(jobCount.get());
      final Set<Long> resubmittedIds = new HashSet<>(jobCount.get());
      bundleQueue.drainTo(resubmitted);
      final Map<Long, RemoteResponse> map = new ConcurrentHashMap<>(responseMap);
      responseMap.clear();
      resubmitted.forEach(bundle -> {
        resubmittedIds.add(bundle.getBundleId());
        if (debugEnabled) log.debug("resubmitting {}", bundle);
        resubmitBundle(bundle, null);
      });
      jobCount.set(0);
      map.forEach((id, response) -> {
        if (!resubmittedIds.contains(id)) {
          if (debugEnabled) log.debug("resubmitting {}", response.clientBundle);
          resubmitBundle(response.clientBundle, null);
        }
      });
    }
  }

  /**
   * Resubmit the specified bundle.
   * @param clientBundle the bundle to resubmit.
   * @param e an eventual exception that might have cause the resubmission.
   */
  private void resubmitBundle(final ClientTaskBundle clientBundle, final Exception e) {
    if (debugEnabled) log.debug("resubmitting {} with exception {}", clientBundle, e);
    clientBundle.resubmit();
    clientBundle.taskCompleted(null);
    clientBundle.getClientJob().removeChannel(this);
  }

  /**
   * Handle the completion of a client bundle.
   * @param clientBundle the client bundle to process.
   * @param exception an eventual exception raised during processing of the bundle, may be {@code null}.
   */
  private void handleBundleComplete(final ClientTaskBundle clientBundle, final Exception exception) {
    try {
      final boolean channelClosed = channel.isClosed();
      if (debugEnabled) log.debug("channelClosed={}, resetting={}, bundle={}, exception={}", channelClosed, resetting, clientBundle, exception);
      if (!channelClosed || resetting) {
        if (clientBundle != null) clientBundle.taskCompleted(exception instanceof IOException ? null : exception);
      }
      if (clientBundle != null) clientBundle.getClientJob().removeChannel(this);
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    } finally {
      jobCount.decrementAndGet();
      if ((getStatus() == JPPFClientConnectionStatus.EXECUTING) && (getCurrentNbJobs() < getMaxJobs())) setStatus(JPPFClientConnectionStatus.ACTIVE);
      ((JobManagerClient) channel.getConnectionPool().getClient().getJobManager()).getJobScheduler().wakeUp();
    }
  }

  @Override
  public void close() {
    if (debugEnabled) log.debug("closing {}, resetting={}", this, resetting);
    resubmitQueueBundles();
    channel.removeClientConnectionStatusListener(listener);
    synchronized(listener) {
      listener.notifyAll();
    }
    super.close();
    futures.forEach(future -> future.cancel(true));
    futures.clear();
    this.initDone = false;
  }

  /**
   * Wait for the connection to be in either a working or terminated state.
   * @throws Exception if any error occurs.
   */
  private void awaitStatus() throws Exception {
    synchronized(statusLock) {
      while (true) {
        final JPPFClientConnectionStatus status = getStatus();
        if (status.isWorkingStatus() || status.isTerminatedStatus()) break;
        statusLock.wait();
      }
    }
  }

  @Override
  public boolean isAsynchronous() {
    return true;
  }

  @Override
  public int getMaxJobs() {
    return channel.getConnectionPool().getMaxJobs();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[')
      .append("bundleQueue=").append(bundleQueue.size())
      .append(", responseMap=").append(responseMap.size())
      .append(", jobCount=").append(getCurrentNbJobs())
      .append(", resetting=").append(resetting)
      .append(", bundlerAlgorithm=").append(bundlerAlgorithm)
      .append(", channel=");
    try {
      sb.append(channel);
    } catch (final Exception e) {
      sb.append(ExceptionUtils.getMessage(e));
    }
    return sb.append(']').toString();
  }
}
