/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.ui.monitoring.data;

import java.util.TimerTask;

import javax.swing.JComboBox;

import org.jppf.client.*;
import org.jppf.ui.options.ComboBoxOption;
import org.jppf.utils.ThreadSynchronization;
import org.slf4j.*;

/**
 * Task executed when a new driver connection is created.
 */
class NewConnectionTask extends ThreadSynchronization implements Runnable
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(NewConnectionTask.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The new connection that was created.
   */
  private JPPFClientConnection c = null;
  /**
   * The {@link StatsHandler}.
   */
  private final StatsHandler statsHandler;

  /**
   * Initialized this task with the specified client connection.
   * @param statsHandler the {@link StatsHandler}.
   * @param c the new connection that was created.
   */
  public NewConnectionTask(final StatsHandler statsHandler, final JPPFClientConnection c)
  {
    this.statsHandler = statsHandler;
    this.c = c;
  }

  /**
   * Perform the task.
   * @see java.lang.Runnable#run()
   */
  @Override
  @SuppressWarnings("unchecked")
  public void run()
  {
    synchronized(statsHandler)
    {
      if (statsHandler.dataHolderMap.get(c.getName()) == null)
      {
        statsHandler.dataHolderMap.put(c.getName(), new ConnectionDataHolder());
      }
      if (statsHandler.timer != null)
      {
        TimerTask task = new StatsRefreshTask((JPPFClientConnectionImpl) c);
        statsHandler.timer.schedule(task, 1000L, statsHandler.refreshInterval);
      }
    }
    JComboBox box = null;
    while (statsHandler.getServerListOption() == null) goToSleep(50L);
    synchronized(statsHandler)
    {
      if (debugEnabled) log.debug("adding client connection " + c.getName());
      box = ((ComboBoxOption) statsHandler.getServerListOption()).getComboBox();
      int count = box.getItemCount();
      boolean found = false;
      for (int i=0; i<count; i++)
      {
        Object o = box.getItemAt(i);
        if (c.equals(o))
        {
          found = true;
          break;
        }
      }
      if (!found)
      {
        box.addItem(c);
        int maxLen = 0;
        Object proto = null;
        for (int i=0; i<box.getItemCount(); i++)
        {
          Object o = box.getItemAt(i);
          int n = o.toString().length();
          if (n > maxLen)
          {
            maxLen = n;
            proto = o;
          }
        }
        if (proto != null) box.setPrototypeDisplayValue(proto);
      }
      if (statsHandler.currentConnection == null)
      {
        //statsHandler.currentConnection = (JPPFClientConnectionImpl) c;
        statsHandler.setCurrentConnection((JPPFClientConnectionImpl) c);
        box.setSelectedItem(c);
      }
    }
  }
}
