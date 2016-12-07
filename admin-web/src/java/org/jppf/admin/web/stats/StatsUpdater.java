/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.admin.web.stats;

import java.util.*;
import java.util.concurrent.*;

import org.jppf.client.monitoring.topology.*;
import org.jppf.ui.monitoring.data.*;
import org.jppf.utils.JPPFThreadFactory;

/**
 *
 * @author Laurent Cohen
 */
public class StatsUpdater extends BaseStatsHandler {
  /**
   * Monitors and maintains a representation of the grid topology.
   */
  private final TopologyManager topologyManager;
  /**
   * Mapping of reusable formatters per locale.
   */
  private final Map<Locale, StatsFormatter> formatterMap = new HashMap<>();
  /**
   * Used to schedule updates of statistics snapshots.
   */
  private final ScheduledExecutorService scheduler;
  /**
   * Listens to drivers joining and leaving the grid.
   */
  private final DriverListener driverListener;
  /**
   * The currently selected driver.
   */
  private TopologyDriver currentDriver;

  /**
   * @param topologyManager monitors and maintains a representation of the grid topology.
   */
  public StatsUpdater(final TopologyManager topologyManager) {
    this.topologyManager = topologyManager;
    driverListener = new DriverListener();
    scheduler = new ScheduledThreadPoolExecutor(1, new JPPFThreadFactory("StatsUpdater"));
    for (TopologyDriver driver: topologyManager.getDrivers()) addDriver(driver);
    topologyManager.addTopologyListener(driverListener);
    scheduler.scheduleWithFixedDelay(new DriverHandler(), 100L, 1000L, TimeUnit.MILLISECONDS);
  }

  /**
   * Called when a new driver is discovered in the grid topology.
   * @param driver the driver to add.
   */
  private synchronized void addDriver(final TopologyDriver driver) {
    ConnectionDataHolder holder = new ConnectionDataHolder(getRolloverPosition(), driver);
    dataHolderMap.put(driver.getUuid(), holder);
    if (currentDriver == null) currentDriver = driver;
  }

  /**
   * Called when a driver disappears from the grid topology.
   * @param driver the driver to remove.
   */
  private synchronized void removeDriver(final TopologyDriver driver) {
    dataHolderMap.remove(driver.getUuid());
  }

  /**
   * Close and cleanup this stats updater and release the resources it is using.
   */
  public synchronized void close() {
    if (scheduler != null) scheduler.shutdownNow();
    dataHolderMap.clear();
    topologyManager.removeTopologyListener(driverListener);
  }

  /**
   * Listens to drivers joining and leaving the grid.
   */
  private class DriverListener extends TopologyListenerAdapter {
    @Override
    public void driverAdded(final TopologyEvent event) {
      addDriver(event.getDriver());
    }

    @Override
    public void driverRemoved(final TopologyEvent event) {
      removeDriver(event.getDriver());
    }
  }

  /**
   * Scheduled task which periodically updates the statistics for all known drivers.
   */
  private class DriverHandler implements Runnable {
    @Override
    public void run() {
      synchronized(StatsUpdater.this) {
        /*
        for (Map.Entry<String, ConnectionDataHolder> entry: dataHolderMap.entrySet()) {
          String uuid = entry.getKey();
          if (topologyManager.getDriver(uuid) == null) removeDriver(entry.getValue().getDriver());
        }
        for (TopologyDriver driver: topologyManager.getDrivers()) {
          if (!dataHolderMap.containsKey(driver.getUuid())) addDriver(driver);
        }
        */
        for (ConnectionDataHolder holder: dataHolderMap.values()) requestUpdate(holder.getDriver());
      }
    }
  }

  @Override
  protected StatsFormatter getFormatter(final Locale locale) {
    synchronized(formatterMap) {
      StatsFormatter formatter = formatterMap.get(locale);
      if (formatter == null) {
        formatter = new StatsFormatter(locale);
        formatterMap.put(locale, formatter);
      }
      return formatter;
    }
  }

  /**
   * @return the currently selected driver.
   */
  public synchronized TopologyDriver getCurrentDriver() {
    return currentDriver;
  }

  /**
   * Set the current driver.
   * @param currentDriver the driver to set.
   */
  public synchronized void setCurrentDriver(final TopologyDriver currentDriver) {
    this.currentDriver = currentDriver;
  }
}
