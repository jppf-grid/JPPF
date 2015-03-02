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

package org.jppf.ui.monitoring.data;

import java.util.TimerTask;

import javax.swing.*;

import org.jppf.client.monitoring.topology.TopologyDriver;
import org.jppf.ui.options.*;
import org.jppf.utils.ThreadSynchronization;
import org.slf4j.*;

/**
 * Task executed when a new driver connection is created.
 */
class NewConnectionTask extends ThreadSynchronization implements Runnable {
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
  private final TopologyDriver driver;
  /**
   * The {@link StatsHandler}.
   */
  private final StatsHandler statsHandler;
  /**
   * The {@link ClientHandler}.
   */
  private final ClientHandler clientHandler;

  /**
   * Initialized this task with the specified client connection.
   * @param statsHandler the {@link StatsHandler}.
   * @param driver represents the new connection that was created.
   */
  public NewConnectionTask(final StatsHandler statsHandler, final TopologyDriver driver) {
    this.statsHandler = statsHandler;
    this.clientHandler = statsHandler.getClientHandler();
    this.driver = driver;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void run() {
    synchronized(statsHandler) {
      if (statsHandler.dataHolderMap.get(driver.getUuid()) != null) return;
      if (debugEnabled) log.debug("adding client connection " + driver);
      ConnectionDataHolder cdh = new ConnectionDataHolder();
      cdh.setDriverData(driver);
      statsHandler.dataHolderMap.put(driver.getUuid(), cdh);
      if (statsHandler.timer != null) {
        TimerTask task = new StatsRefreshTask(driver);
        statsHandler.timer.schedule(task, 1000L, statsHandler.refreshInterval);
      }
    }
    try {
      SwingUtilities.invokeAndWait(new ComboUpdate());
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * 
   */
  private class ComboUpdate implements Runnable {
    @Override
    public void run() {
      OptionElement serverList = clientHandler.getServerListOption();
      JComboBox box = (serverList == null) ? null : ((ComboBoxOption) serverList).getComboBox();
      if (box != null) {
        int count = box.getItemCount();
        boolean found = false;
        for (int i=0; i<count; i++) {
          Object o = box.getItemAt(i);
          if (driver.equals(o)) {
            found = true;
            break;
          }
        }
        if (!found) {
          box.addItem(driver);
          int maxLen = 0;
          Object proto = null;
          for (int i=0; i<box.getItemCount(); i++) {
            Object o = box.getItemAt(i);
            int n = o.toString().length();
            if (n > maxLen) {
              maxLen = n;
              proto = o;
            }
          }
          if (proto != null) box.setPrototypeDisplayValue(proto);
        }
      }
      if (clientHandler.currentDriver == null) {
        clientHandler.setCurrentDriver(driver);
        if (box != null) box.setSelectedItem(driver);
      }
    }
  };
}
