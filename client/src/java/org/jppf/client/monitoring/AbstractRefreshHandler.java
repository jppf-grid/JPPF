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

package org.jppf.client.monitoring;

import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * Instances of this class hold information about the associations between JPPF drivers and
 * their attached nodes, for management and monitoring purposes.
 * @author Laurent Cohen
 * @since 5.0
 * @exclude
 */
public abstract class AbstractRefreshHandler {
  /**
   * Timer used to query the driver management data.
   */
  private Timer refreshTimer = null;
  /**
   * Interval, in milliseconds, between refreshes from the server.
   */
  private final long refreshInterval;
  /**
   * Determines whether we are currently refreshing.
   */
  private AtomicBoolean refreshing = new AtomicBoolean(false);
  /**
   * Determines whether refreshes are currently suspended.
   */
  private AtomicBoolean suspended = new AtomicBoolean(false);
  /**
   * Name given to this refresher and its timer thread.
   */
  private final String name;

  /**
   * Initialize this node handler.
   * @param refreshInterval interval in milliseconds between refreshes.
   * @param name the name given to this refresher and its timer thread.
   */
  public AbstractRefreshHandler(final String name, final long refreshInterval) {
    this.refreshInterval = refreshInterval;
    this.name = name;
  }

  /**
   * Refresh the tree structure asynchronously (not in the AWT event thread).
   */
  public void refresh() {
    if (refreshing.compareAndSet(false, true) && !suspended.get()) {
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
    refreshTimer = new Timer(name == null ? "RefreshHandler Timer" : name, true);
    final TimerTask task = new TimerTask() {
      @Override
      public void run() {
        if (!suspended.get()) refresh();
      }
    };
    refreshTimer.schedule(task, 1000L, refreshInterval);
  }

  /**
   * Determine whether refreshes are currently suspended.
   * @return {@code true} if refreshes are suspended, {@code false} otherwise.
   * @since 5.1
   */
  public boolean isSuspended() {
    return suspended.get();
  }

  /**
   * Specify whether refreshes are currently suspended.
   * @param suspended {@code true} to suspend refreshes, {@code false} to resume them.
   * @since 5.1
   */
  public void setSuspended(final boolean suspended) {
    this.suspended.set(suspended);
  }
}
