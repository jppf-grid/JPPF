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

package org.jppf.node.throttling;

import java.util.*;

import org.jppf.node.protocol.*;
import org.jppf.node.protocol.NotificationBundle.NotificationType;
import org.jppf.server.node.JPPFNode;
import org.jppf.utils.ServiceProviderHandler;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * This class loads and invokes all implementations of the {@link JPPFNodeThrottling} service found in the classpath.
 * Invocation of the services is done at two points of the node's life cycle:
 * <ol>
 * <li>during the initial handshake with the driver</li>
 * <li>at regular intervals via a specific timer</li>
 * </ol>
 * The duration of the interval between periodic checks is configurable with the following property expressing a time in milliseconds (default 2000 ms):<br>
 * {@code jppf.node.throttling.check.period = 2000}
 * @author Laurent Cohen
 * @exclude
 */
public class NodeThrottlingHandler extends ServiceProviderHandler<JPPFNodeThrottling> {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(NodeThrottlingHandler.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * The node to which the throttling applies.
   */
  private final JPPFNode node;
  /**
   * Whether the node is currently accepting jobs.
   */
  private boolean currentlyAccepts;
  /**
   * The timer running the periodic checks.
   */
  private Timer timer;
  /**
   * The task that performs the periodic checks.
   */
  private CheckTask checkTask;

  /**
   * Initialize ths handler with the specified node.
   * @param node the node to which the throttling applies.
   */
  public NodeThrottlingHandler(final JPPFNode node) {
    super(JPPFNodeThrottling.class, node.getClassLoader());
    currentlyAccepts = true;
    this.node = node;
    if (!node.isOffline()) loadProviders();
  }

  /**
   * Start this {@code NodeThrottlingHandler}.
   * @return this {@code NodeThrottlingHandler}, for method call chaining.
   */
  public NodeThrottlingHandler start() {
    if (!node.isOffline()) {
      timer = new Timer("NodeThrottlingTimer", true);
      long period = node.getConfiguration().get(JPPFProperties.NODE_THROTTLING_CHECK_PERIOD);
      if (period <= 0L) period = JPPFProperties.NODE_THROTTLING_CHECK_PERIOD.getDefaultValue();
      timer.schedule(checkTask = new CheckTask(), 1000L, period);
      if (debugEnabled) log.debug("node throttling initialized with a check period of {} ms", period);
    }
    return this;
  }

  /**
   * Stop this {@code NodeThrottlingHandler} and release its resources.
   * @return this {@code NodeThrottlingHandler}, for method call chaining.
   */
  public NodeThrottlingHandler stop() {
    if (debugEnabled) log.debug("stopping node throttling");
    if (checkTask != null) checkTask.cancel();
    if (timer != null) {
      timer.cancel();
      timer.purge();
    }
    return this;
  }

  /**
   * 
   * @return true if the node accepts new jobs, false otherwise.
   */
  public synchronized boolean check() {
    new CheckTask(false).run();
    return currentlyAccepts;
  }

  /**
   * A timer task called periodically that checks whther the node should accept new jobs.
   */
  private final class CheckTask extends TimerTask {
    /**
     * Whether to send a notification to the driver.
     */
    private final boolean sendNotification;

    /**
     * Create this task.
     */
    private CheckTask() {
      this(true);
    }

    /**
     * Create this task.
     * @param sendNotification whether to send a notification to the driver.
     */
    private CheckTask(final boolean sendNotification) {
      this.sendNotification = sendNotification;
    }

    @Override
    public void run() {
      if (providers.isEmpty()) return;
      boolean newAccepts = true;
      for (final JPPFNodeThrottling provider: providers) {
        if (!provider.acceptsNewJobs(node)) {
          newAccepts = false;
          break;
        }
      }
      synchronized(NodeThrottlingHandler.this) {
        if (sendNotification && (newAccepts != currentlyAccepts)) {
          if (debugEnabled) log.debug("throttling state has changed to {}, sending notification", newAccepts);
          final NotificationBundle notif = new NotificationBundle(NotificationType.THROTTLING);
          notif.setParameter(BundleParameter.NODE_ACCEPTS_NEW_JOBS, newAccepts);
          try {
            node.getJobWriter().putJob(notif, null);
          } catch (final Exception e) {
            log.error("error adding notification bundle to the send queue", e);
          }
        }
        currentlyAccepts = newAccepts;
      }
    }
  }
}
