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

package org.jppf.client.monitoring.topology;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.slf4j.*;

/**
 * Instances of this class hold information about the associations between JPPF drivers and
 * their attached nodes, for management and monitoring purposes.
 * @author Laurent Cohen
 * @since 5.0
 * @exclude
 */
abstract class AbstractRefreshHandler {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractRefreshHandler.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Timer used to query the driver management data.
   */
  private Timer refreshTimer = null;
  /**
   * Interval, in milliseconds, between refreshes from the server.
   */
  private final long refreshInterval;
  /**
   * Count of refresh invocations.
   */
  private AtomicLong refreshCount = new AtomicLong(0L);
  /**
   * Determines whether we are currently refreshing.
   */
  private AtomicBoolean refreshing = new AtomicBoolean(false);
  /**
   * The topology manager to which topology change notifications are to be sent. 
   */
  final TopologyManager manager;
  /**
   * Name given to this refresher and its timer thread.
   */
  private final String name;

  /**
   * Initialize this node handler.
   * @param manager the topology manager.
   * @param refreshInterval interval in milliseconds between refreshes.
   * @param name the name given to this refresher and its timer thread.
   */
  public AbstractRefreshHandler(final TopologyManager manager, final String name, final long refreshInterval) {
    this.manager = manager;
    this.refreshInterval = refreshInterval;
    this.name = name;
    initialize();
  }

  /**
   * Initialize this node refresh handler.
   */
  private void initialize() {
    startRefreshTimer();
  }

  /**
   * Refresh the tree structure asynchronously (not in the AWT event thread).
   */
  public void refresh() {
    if (refreshing.compareAndSet(false, true)) {
      try {
        performRefresh();
      } finally {
        refreshing.set(false);
      }
    }
  }

  /**
   * Refresh the refresh.
   * @exclude
   */
  protected abstract void performRefresh();

  /**
   * Stop the automatic refresh of the nodes state through a timer.
   */
  public void stopRefreshTimer() {
    if (refreshTimer != null) {
      refreshTimer.cancel();
      refreshTimer = null;
    }
  }

  /**
   * Start the automatic refresh of the nodes state through a timer.
   */
  public void startRefreshTimer() {
    if (refreshTimer != null) return;
    if (refreshInterval <= 0L) return;
    refreshTimer = new Timer(name == null ? "RefreshHandler Timer" : name);
    TimerTask task = new TimerTask() {
      @Override
      public void run() {
        refresh();
      }
    };
    refreshTimer.schedule(task, 1000L, refreshInterval);
  }
}
