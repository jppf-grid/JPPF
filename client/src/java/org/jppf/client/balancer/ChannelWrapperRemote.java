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

package org.jppf.client.balancer;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.jppf.JPPFException;
import org.jppf.client.*;
import org.jppf.load.balancer.BundlerHelper;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.node.protocol.*;
import org.jppf.serialization.ObjectSerializer;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Context associated with a remote channel serving state and tasks submission.
 * @author Martin JANDA
 * @author Laurent Cohen
 */
public class ChannelWrapperRemote extends AbstractChannelWrapperRemote {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ChannelWrapperRemote.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * Default initializer for remote channel wrapper.
   * @param channel to the driver to use.
   */
  public ChannelWrapperRemote(final JPPFClientConnection channel) {
    super(channel);
  }

  @Override
  public void setSystemInformation(final JPPFSystemInformation systemInfo) {
    super.setSystemInformation(systemInfo);
  }

  @Override
  public Future<?> submit(final ClientTaskBundle bundle) {
    if (!channel.isClosed()) {
      jobCount.set(1);
      if (debugEnabled) log.debug("submitting {} to {}", bundle, this);
      setStatus(JPPFClientConnectionStatus.EXECUTING);
      final ExecutorService executor = channel.getClient().getExecutor();
      executor.execute(new RemoteRunnable(bundle));
      if (debugEnabled) log.debug("submitted {} to {}", bundle, this);
    }
    return null;
  }

  /**
   * Sends the tasks to the drivr and gets the results back.
   * Also handles exceptions and failover and recovery scenarios when the driver connection breaks.
   */
  private class RemoteRunnable implements Runnable {
    /**
     * The task bundle to execute.
     */
    private final ClientTaskBundle clientBundle;

    /**
     * Initialize this runnable for remote execution.
     * @param clientBundle  the execution to perform.
     */
    public RemoteRunnable(final ClientTaskBundle clientBundle) {
      this.clientBundle = clientBundle;
    }

    @Override
    public void run() {
      Exception exception = null;
      final List<Task<?>> tasks = clientBundle.getTasksL();
      try {
        final long start = System.nanoTime();
        int count = 0;

        boolean completed = false;
        final JPPFJob newJob = createNewJob(clientBundle, tasks);
        if (debugEnabled) log.debug("{} executing {} tasks of job {}", ChannelWrapperRemote.this, tasks.size(), newJob);
        final Collection<ClassLoader> loaders = registerClassLoaders(newJob);
        while (!completed) {
          final TaskBundle bundle = createBundle(newJob, clientBundle.getBundleId());
          bundle.setUuid(uuid);
          bundle.setInitialTaskCount(clientBundle.getClientJob().initialTaskCount);
          final ClassLoader cl = loaders.isEmpty() ? null : loaders.iterator().next();
          final ObjectSerializer ser = channel.makeHelper(cl).getSerializer();
          final List<Task<?>> notSerializableTasks = channel.sendTasks(ser, cl, bundle, newJob);
          clientBundle.jobDispatched(ChannelWrapperRemote.this);
          if (!notSerializableTasks.isEmpty()) {
            if (debugEnabled) log.debug("{} got {} non-serializable tasks", ChannelWrapperRemote.this, notSerializableTasks.size());
            count += notSerializableTasks.size();
            clientBundle.resultsReceived(notSerializableTasks);
          }
          while (count < tasks.size()) {
            final List<Task<?>> results = channel.receiveResults(ser, cl);
            final int n = results.size();
            count += n;
            if (debugEnabled) log.debug("received " + n + " tasks from server" + (n > 0 ? ", first position=" + results.get(0).getPosition() : ""));
            this.clientBundle.resultsReceived(results);
          }
          completed = true;
        }
        final double elapsed = System.nanoTime() - start;
        BundlerHelper.updateBundler(bundler, tasks.size(), elapsed);
        getLoadBalancerPersistenceManager().storeBundler(channelID, bundler, bundlerAlgorithm);
      } catch (final Throwable t) {
        if (debugEnabled) log.debug(t.getMessage(), t);
        else log.warn(ExceptionUtils.getMessage(t));
        final boolean channelClosed = channel.isClosed();
        if (debugEnabled) log.debug("channelClosed={}, resetting={}", channelClosed, resetting);
        if (channelClosed && !resetting) return;
        exception = (t instanceof Exception) ? (Exception) t : new JPPFException(t);
        if ((t instanceof NotSerializableException) || (t instanceof InterruptedException)) {
          clientBundle.resultsReceived(t);
          return;
        }
        if (!channelClosed || resetting) {
          if (debugEnabled) log.debug("resubmitting {}", clientBundle);
          clientBundle.resubmit();
          reconnect();
        }
      } finally {
        try {
          final boolean channelClosed = channel.isClosed();
          if (debugEnabled) log.debug("finally: channelClosed={}, resetting={}", channelClosed, resetting);
          if (!channelClosed || resetting) {
            clientBundle.taskCompleted(exception instanceof IOException ? null : exception);
          }
          clientBundle.getClientJob().removeChannel(ChannelWrapperRemote.this);
          if (getStatus() == JPPFClientConnectionStatus.EXECUTING) setStatus(JPPFClientConnectionStatus.ACTIVE);
        } catch (final Exception e) {
          log.error(e.getMessage(), e);
        } finally {
          jobCount.set(0);
        }
      }
    }
  }
}
