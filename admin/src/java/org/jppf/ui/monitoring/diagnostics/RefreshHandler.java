/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

package org.jppf.ui.monitoring.diagnostics;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.slf4j.*;

/**
 * Instances of this class hold information about the associations between JPPF drivers and
 * their attached nodes, for management and monitoring purposes.
 * @author Laurent Cohen
 */
public class RefreshHandler
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(RefreshHandler.class);
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
  private long refreshInterval = 3000L;
  /**
   * The panel to refresh.
   */
  private JVMHealthPanel panel = null;
  /**
   * Count of refresh invocations.
   */
  private AtomicLong refreshCount = new AtomicLong(0L);
  /**
   * Determines whether we are currently refreshing.
   */
  private AtomicBoolean refreshing = new AtomicBoolean(false);

  /**
   * Initialize this node handler.
   * @param nodeDataPanel - the panel to refresh.
   */
  public RefreshHandler(final JVMHealthPanel nodeDataPanel)
  {
    this.panel = nodeDataPanel;
    initialize();
  }

  /**
   * Initialize this node refresh handler.
   */
  private void initialize()
  {
    startRefreshTimer();
  }

  /**
   * Refresh the tree structure asynchronously (not in the AWT event thread).
   */
  public void refresh()
  {
    if (refreshing.get()) return;
    refreshing.set(true);
    try
    {
      panel.refreshSnapshots();
    }
    finally
    {
      refreshing.set(false);
    }
  }

  /**
   * Stop the automatic refresh of the nodes state through a timer.
   */
  public void stopRefreshTimer()
  {
    if (refreshTimer != null)
    {
      refreshTimer.cancel();
      refreshTimer = null;
    }
  }

  /**
   * Start the automatic refresh of the nodes state through a timer.
   */
  public void startRefreshTimer()
  {
    if (refreshTimer != null) return;
    if (refreshInterval <= 0L) return;
    refreshTimer = new Timer("JVM Health Update Timer");
    TimerTask task = new TimerTask()
    {
      @Override
      public void run()
      {
        refresh();
      }
    };
    refreshTimer.schedule(task, 1000L, refreshInterval);
  }
}
