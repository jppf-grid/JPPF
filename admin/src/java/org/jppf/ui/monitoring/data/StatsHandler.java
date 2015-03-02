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

import java.awt.Toolkit;
import java.awt.datatransfer.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jppf.client.monitoring.topology.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.management.diagnostics.HealthSnapshot;
import org.jppf.ui.monitoring.event.*;
import org.jppf.ui.utils.GuiUtils;
import org.jppf.utils.*;
import org.jppf.utils.stats.JPPFStatistics;
import org.slf4j.*;

/**
 * This class provides a convenient access to the statistics obtained from the JPPF server.
 * @author Laurent Cohen
 */
public final class StatsHandler implements StatsConstants {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(StatsHandler.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Singleton instance of this class.
   */
  private static StatsHandler instance = null;
  /**
   * The object holding the current statistics values.
   */
  private JPPFStatistics stats = new JPPFStatistics();
  /**
   * List of listeners registered with this stats handler.
   */
  private List<StatsHandlerListener> listeners = new ArrayList<>();
  /**
   * List of listeners tot he state of og the ShowIP toggle registered with this stats handler.
   */
  private List<ShowIPListener> showIPListeners = new CopyOnWriteArrayList<>();
  /**
   * Timer used to query the stats from the server.
   */
  java.util.Timer timer = null;
  /**
   * Interval, in milliseconds, between refreshes from the server.
   */
  long refreshInterval = 2000L;
  /**
   * Number of data snapshots kept in memory.
   */
  private int rolloverPosition = 200;
  /**
   * Contains all the data and its converted values received from the server.
   */
  Map<String, ConnectionDataHolder> dataHolderMap = new HashMap<>();
  /**
   * Number of data updates so far.
   */
  private int tickCount = 0;
  /**
   * The client handler.
   */
  private final ClientHandler clientHandler;
  /**
   * The object which monitors and maintains a representation of the grid topology.
   */
  private final TopologyManager topologyManager;
  /**
   * {@code true} to show IP addresses, {@code false} to display host names.
   */
  private boolean showIP = false;

  /**
   * Get the singleton instance of this class.
   * @return a <code>StatsHandler</code> instance.
   */
  public static StatsHandler getInstance() {
    if (instance == null) instance = new StatsHandler();
    return instance;
  }

  /**
   * Determine whether an instance of this class was created.
   * @return {@code true} if an instance was created, {@code false} otherwise.
   */
  public static boolean hasInstance() {
    return instance != null;
  }

  /**
   * Initialize this statistics handler.
   */
  private StatsHandler() {
    if (debugEnabled) log.debug("initializing StatsHandler");
    refreshInterval = JPPFConfiguration.getProperties().getLong("jppf.admin.refresh.interval.stats", 1000L);
    if (refreshInterval > 0L) timer = new java.util.Timer("JPPF Driver Statistics Update Timer");
    if (debugEnabled) log.debug("initializing TopologyManager");
    topologyManager = new TopologyManager();
    if (debugEnabled) log.debug("done initializing TopologyManager");
    clientHandler = new ClientHandler(this);
  }

  /**
   * Stop the automatic refresh of the stats through a timer.
   */
  public void stopRefreshTimer() {
    if (timer != null) {
      timer.cancel();
      timer = null;
    }
  }

  /**
   * Get the interval between refreshes from the server.
   * @return the interval in milliseconds.
   */
  public long getRefreshInterval() {
    return refreshInterval;
  }

  /**
   * Set the interval between refreshes from the server.
   * @param refreshInterval the interval in milliseconds.
   */
  public void setRefreshInterval(final long refreshInterval) {
    this.refreshInterval = refreshInterval;
  }

  /**
   * Request an update from the current server connection.
   */
  public void requestUpdate() {
    ConnectionDataHolder cdh = getCurrentDataHolder();
    if (cdh != null) requestUpdate(cdh.getDriverData());
  }

  /**
   * Request an update from the server.
   * @param driver represents the client connection to request the data from.
   * @return a {@link JPPFStatistics} object.
   */
  public JPPFStatistics requestUpdate(final TopologyDriver driver) {
    JPPFStatistics stats = null;
    try {
      JMXDriverConnectionWrapper jmx = driver.getJmx();
      if ((jmx != null) && jmx.isConnected()) {
        stats = jmx.statistics();
        if (stats != null) update(driver, stats);
      }
    } catch(Exception e) {
      log.error(e.getMessage(), e);
    }
    return stats;
  }

  /**
   * Update the current statistics with new values obtained from the server.
   * @param driver represents the client connection from which the data is obtained.
   * @param stats the object holding the new statistics values.
   */
  public synchronized void update(final TopologyDriver driver, final JPPFStatistics stats) {
    if (stats == null) return;
    ConnectionDataHolder dataHolder = dataHolderMap.get(driver.getUuid());
    tickCount++;
    if (dataHolder == null) return;
    dataHolder.getDataList().add(stats);
    dataHolder.getStringValuesMaps().add(StatsFormatter.getStringValuesMap(stats));
    dataHolder.getDoubleValuesMaps().add(StatsFormatter.getDoubleValuesMap(stats));
    TopologyDriver data = dataHolder.getDriverData();
    HealthSnapshot snapshot = (data == null) ? new HealthSnapshot() : data.getHealthSnapshot();
    StatsFormatter.stringValues2(dataHolder.getLatestStringValues(), snapshot);
    StatsFormatter.doubleValues2(dataHolder.getLatestDoubleValues(), snapshot);
    int diff = dataHolder.getDataList().size() - rolloverPosition;
    for (int i=0; i<diff; i++) {
      dataHolder.getDataList().remove(0);
      dataHolder.getStringValuesMaps().remove(0);
      dataHolder.getDoubleValuesMaps().remove(0);
    }
    TopologyDriver current = clientHandler.getCurrentDriver();
    if ((current != null) && driver.getUuid().equals(current.getUuid())) fireStatsHandlerEvent(StatsHandlerEvent.Type.UPDATE);
  }

  /**
   * Register a <code>StatsHandlerListener</code> with this stats formatter.
   * @param listener the listener to register.
   */
  public void addStatsHandlerListener(final StatsHandlerListener listener) {
    if (listener != null) listeners.add(listener);
  }

  /**
   * Unregister a <code>StatsHandlerListener</code> from this stats formatter.
   * @param listener the listener to unregister.
   */
  public void removeStatsHandlerListener(final StatsHandlerListener listener) {
    if (listener != null) listeners.remove(listener);
  }

  /**
   * Notify all listeners of a change in this stats formatter.
   * @param type the type of event to fire.
   */
  public void fireStatsHandlerEvent(final StatsHandlerEvent.Type type) {
    StatsHandlerEvent event = new StatsHandlerEvent(this, type);
    for (StatsHandlerListener listener: listeners) listener.dataUpdated(event);
  }

  /**
   * Register a <code>StatsHandlerListener</code> with this stats formatter.
   * @param listener the listener to register.
   */
  public void addShowIPListener(final ShowIPListener listener) {
    if (listener != null) showIPListeners.add(listener);
  }

  /**
   * Unregister a <code>StatsHandlerListener</code> from this stats formatter.
   * @param listener the listener to unregister.
   */
  public void removeShowIPListener(final ShowIPListener listener) {
    if (listener != null) showIPListeners.remove(listener);
  }

  /**
   * Determine whether to show IP addresses or host names.
   * @return {@code true} to show IP addresses, {@code false} to display host names.
   */
  public boolean isShowIP() {
    return showIP;
  }

  /**
   * Specify whether to show IP addresses or host names.
   * @param showIP {@code true} to show IP addresses, {@code false} to display host names.
   */
  public void setShowIP(final boolean showIP) {
    if (showIP != this.showIP) {
      boolean oldState = this.showIP;
      this.showIP = showIP;
      ShowIPEvent event = new ShowIPEvent(this, oldState);
      for (ShowIPListener listener: showIPListeners) listener.stateChanged(event);
    }
  }

  /**
   * Get the number of data snapshots kept in memory.
   * @return the rollover position as an int value.
   */
  public synchronized int getRolloverPosition() {
    return rolloverPosition;
  }

  /**
   * Set the number of data snapshots kept in memory. If the value if less than the former values, the corresponding,
   * older, data snapshots will be deleted.
   * @param rolloverPosition - the rollover position as an int value.
   */
  public synchronized void setRolloverPosition(final int rolloverPosition) {
    if (rolloverPosition <= 0) throw new IllegalArgumentException("zero or less not accepted: " + rolloverPosition);
    ConnectionDataHolder dataHolder = dataHolderMap.get(clientHandler.getCurrentDriver().getUuid());
    int diff = dataHolder.getDataList().size() - rolloverPosition;
    for (int i=0; i<diff; i++) {
      dataHolder.getDataList().remove(0);
      dataHolder.getStringValuesMaps().remove(0);
      dataHolder.getDoubleValuesMaps().remove(0);
    }
    this.rolloverPosition = rolloverPosition;
  }

  /**
   * Get the current number of data snapshots.
   * @return the number of snapshots as an int.
   */
  public int getStatsCount() {
    TopologyDriver driver = clientHandler.getCurrentDriver();
    if (driver == null) return 0;
    ConnectionDataHolder dataHolder = dataHolderMap.get(driver.getUuid());
    return (dataHolder == null) ? -1 : dataHolder.getDataList().size();
  }

  /**
   * Get the data snapshot at a specified position.
   * @param position - the position to get the data at.
   * @return a <code>JPPFStats</code> instance.
   */
  public synchronized JPPFStatistics getStats(final int position) {
    TopologyDriver driver = clientHandler.getCurrentDriver();
    if (driver == null) return stats;
    ConnectionDataHolder dataHolder = dataHolderMap.get(driver.getUuid());
    return dataHolder.getDataList().get(position);
  }

  /**
   * Get the latest data snapshot.
   * @return a <code>JPPFStatistics</code> instance.
   */
  public JPPFStatistics getLatestStats() {
    return getStats(getStatsCount() - 1);
  }

  /**
   * Return the {@link ConnectionDataHolder} for the current connection.
   * @return a {@link ConnectionDataHolder} instance.
   */
  public synchronized ConnectionDataHolder getCurrentDataHolder() {
    TopologyDriver driver = clientHandler.getCurrentDriver();
    return (driver == null) ? null : dataHolderMap.get(driver.getUuid());
  }

  /**
   * Get the mapping of a data snapshot's fields, at a specified position, to their corresponding string values.
   * @param position the position to get the data at.
   * @return a map of field names to their values represented as strings.
   */
  public synchronized Map<Fields, String> getStringValues(final int position) {
    TopologyDriver driver = clientHandler.getCurrentDriver();
    if (driver == null) return null;
    ConnectionDataHolder dataHolder = dataHolderMap.get(driver.getUuid());
    return dataHolder.getStringValuesMaps().get(position);
  }

  /**
   * Get the mapping of the most recent data snapshot's fields to their corresponding string values.
   * @return a map of field names to their values represented as strings.
   */
  public Map<Fields, String> getLatestStringValues() {
    TopologyDriver driver = clientHandler.getCurrentDriver();
    if (driver == null) return new HashMap<>();
    int n = getStatsCount() - 1;
    if (n < 0) return null;
    ConnectionDataHolder dataHolder = dataHolderMap.get(driver.getUuid());
    List<Map<Fields, String>> list = dataHolder.getStringValuesMaps();
    if (n < list.size()) return list.get(n);
    return list.get(list.size()-1);
  }

  /**
   * Get the mapping of a data snapshot's fields, at a specified position, to their corresponding double values.
   * @param position the position to get the data at.
   * @return a map of field names to their values represented as double values.
   */
  public synchronized Map<Fields, Double> getDoubleValues(final int position) {
    TopologyDriver driver = clientHandler.getCurrentDriver();
    if (driver == null) return null;
    ConnectionDataHolder dataHolder = dataHolderMap.get(driver.getUuid());
    return dataHolder.getDoubleValuesMaps().get(position);
  }

  /**
   * Get the mapping of the most recent data snapshot's fields to their corresponding double values.
   * @return a map of field names to their values represented as double values.
   */
  public Map<Fields, Double> getLatestDoubleValues() {
    TopologyDriver driver = clientHandler.getCurrentDriver();
    if (driver == null) return null;
    int n = getStatsCount() - 1;
    if (n < 0) return null;
    ConnectionDataHolder dataHolder = dataHolderMap.get(driver.getUuid());
    List<Map<Fields, Double>> list = dataHolder.getDoubleValuesMaps();
    if (n < list.size()) return list.get(n);
    return list.get(list.size()-1);
  }

  /**
   * Get the number of data updates so far.
   * @return the number of updates as an int.
   */
  public int getTickCount() {
    return tickCount;
  }

  /**
   * Copy the currently displayed statistics to the clipboard.
   * @param format specifies the format, among the constants defined in {@link StatsExporter}.
   */
  public void copyStatsToClipboard(final int format) {
    try
    {
      StatsExporter exporter = (format == StatsExporter.CSV) ? new CsvStatsExporter(this) : new TextStatsExporter(this);
      String text = exporter.formatAll();
      Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
      clip.setContents(new StringSelection(text), null);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Reset the statistics of the currently selected driver, if any.
   */
  public void resetCurrentStats() {
    Runnable r = new Runnable() {
      @Override
      public void run() {
        TopologyDriver driver = clientHandler.getCurrentDriver();
        if (driver == null) return;
        JMXDriverConnectionWrapper jmx = driver.getJmx();
        if ((jmx != null) && jmx.isConnected()) {
          try {
            jmx.resetStatistics();
          } catch (Exception e) {
            if (debugEnabled) log.debug("couldn't reset statistics on {} : {}", driver, ExceptionUtils.getStackTrace(e));
            else log.error("couldn't reset statistics on {} : {}", driver, ExceptionUtils.getMessage(e));
          }
        }
      }
    };
    GuiUtils.runAction(r, "Reset server stats");
  }

  /**
   * Get the client handler.
   * @return a {@code ClientHandler} object.
   */
  public ClientHandler getClientHandler() {
    return clientHandler;
  }

  /**
   * Get the object which monitors and maintains a representation of the grid topology.
   * @return a {@link TopologyManager} object.
   */
  public TopologyManager getTopologyManager() {
    return topologyManager;
  }
}
