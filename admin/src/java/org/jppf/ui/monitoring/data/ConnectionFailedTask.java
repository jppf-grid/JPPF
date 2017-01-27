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

package org.jppf.ui.monitoring.data;

import javax.swing.JComboBox;

import org.jppf.client.JPPFClientConnection;
import org.jppf.client.monitoring.topology.TopologyDriver;
import org.jppf.ui.options.ComboBoxOption;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Task executed when a new driver connection is created.
 */
class ConnectionFailedTask extends ThreadSynchronization implements Runnable {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ConnectionFailedTask.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The new connection that was created.
   */
  private final TopologyDriver driver;
  //private JPPFClientConnection c = null;
  /**
   * The {@link StatsHandler}.
   */
  private final StatsHandler statsHandler;

  /**
   * Initialized this task with the specified client connection.
   * @param statsHandler the {@link StatsHandler}.
   * @param driver the new connection that was created.
   */
  public ConnectionFailedTask(final StatsHandler statsHandler, final TopologyDriver driver) {
    this.statsHandler = statsHandler;
    this.driver = driver;
  }

  /**
   * Perform the task.
   */
  @Override
  public void run() {
    synchronized(statsHandler) {
      if (statsHandler.dataHolderMap.get(driver.getUuid()) != null) {
        ConnectionDataHolder cdh = statsHandler.dataHolderMap.remove(driver.getUuid());
        if (cdh != null) cdh.close();
      }
    }
    JComboBox<?> box = null;
    while (statsHandler.getClientHandler().getServerListOption() == null) goToSleep(50L);
    JPPFClientConnection c = driver.getConnection();
    synchronized(statsHandler) {
      if (debugEnabled) log.debug("removing client connection " + c.getName() + " from driver combo box");
      box = ((ComboBoxOption) statsHandler.getClientHandler().getServerListOption()).getComboBox();
      int count = box.getItemCount();
      int idx = -1;
      for (int i=0; i<count; i++) {
        Object o = box.getItemAt(i);
        if (c.equals(o)) {
          box.removeItemAt(i);
          idx = i;
          break;
        }
      }
      if ((idx >= 0) && (box.getItemCount() > 0)) {
        if ((statsHandler.getClientHandler().currentDriver == null) || c.equals(statsHandler.getClientHandler().currentDriver)) {
          int n = Math.min(idx, box.getItemCount()-1);
          TopologyDriver item = (TopologyDriver) box.getItemAt(n);
          statsHandler.getClientHandler().currentDriver = item;
          box.setSelectedItem(item);
        }
      }
    }
  }
}
