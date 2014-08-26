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

import javax.swing.JComboBox;

import org.jppf.client.*;
import org.jppf.ui.options.ComboBoxOption;
import org.jppf.utils.ThreadSynchronization;
import org.slf4j.*;

/**
 * Task executed when a new driver connection is created.
 */
class ConnectionFailedTask extends ThreadSynchronization implements Runnable
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ConnectionFailedTask.class);
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
  public ConnectionFailedTask(final StatsHandler statsHandler, final JPPFClientConnection c)
  {
    this.statsHandler = statsHandler;
    this.c = c;
  }

  /**
   * Perform the task.
   */
  @Override
  public void run()
  {
    synchronized(statsHandler)
    {
      if (statsHandler.dataHolderMap.get(c.getName()) != null)
      {
        statsHandler.dataHolderMap.remove(c.getName());
      }
    }
    JComboBox box = null;
    while (statsHandler.getClientHandler().getServerListOption() == null) goToSleep(50L);
    synchronized(statsHandler)
    {
      if (debugEnabled) log.debug("removing client connection " + c.getName() + " from driver combo box");
      box = ((ComboBoxOption) statsHandler.getClientHandler().getServerListOption()).getComboBox();
      int count = box.getItemCount();
      int idx = -1;
      for (int i=0; i<count; i++)
      {
        Object o = box.getItemAt(i);
        if (c.equals(o))
        {
          box.removeItemAt(i);
          idx = i;
          break;
        }
      }
      if ((idx >= 0) && (box.getItemCount() > 0))
      {
        if ((statsHandler.getClientHandler().currentConnection == null) || c.equals(statsHandler.getClientHandler().currentConnection))
        {
          int n = Math.min(idx, box.getItemCount()-1);
          JPPFClientConnection conn = (JPPFClientConnection) box.getItemAt(n);
          statsHandler.getClientHandler().currentConnection = conn;
          box.setSelectedItem(conn);
        }
      }
    }
  }
}
