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

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jppf.client.monitoring.topology.TopologyDriver;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.management.diagnostics.HealthSnapshot;
import org.jppf.ui.monitoring.ShowIPHandler;
import org.jppf.ui.monitoring.event.*;
import org.jppf.utils.LoggingUtils;
import org.jppf.utils.stats.JPPFStatistics;
import org.slf4j.*;

/**
 * This class provides a convenient access to the statistics obtained from the JPPF server.
 * @author Laurent Cohen
 */
public class BaseStatsHandler {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(BaseStatsHandler.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Interval, in milliseconds, between refreshes from the server.
   */
  protected long refreshInterval = 2000L;
  /**
   * Number of data snapshots kept in memory.
   */
  protected int rolloverPosition = 200;
  /**
   * Contains all the data and its converted values received from the server.
   */
  protected final Map<String, ConnectionDataHolder> dataHolderMap = new HashMap<>();
  /**
   * List of listeners registered with this stats handler.
   */
  private List<StatsHandlerListener> listeners = new CopyOnWriteArrayList<>();
  /**
   * Number of data updates so far.
   */
  private int tickCount = 0;
  /**
   * Handles the toggle for showing host names vs. IP addresses.
   */
  private final ShowIPHandler showIPHandler = new ShowIPHandler();

  /**
   * Initialize this statistics handler.
   */
  public BaseStatsHandler() {
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
   * Request an update from the server.
   * @param driver represents the client connection to request the data from.
   * @return a {@link JPPFStatistics} object.
   */
  public JPPFStatistics requestUpdate(final TopologyDriver driver) {
    JPPFStatistics stats = null;
    try {
      final JMXDriverConnectionWrapper jmx = driver.getJmx();
      if ((jmx != null) && jmx.isConnected()) {
        stats = jmx.statistics();
        if (stats != null) update(driver, stats);
      }
    } catch(final Exception e) {
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
    final ConnectionDataHolder dataHolder = dataHolderMap.get(driver.getUuid());
    tickCount++;
    if (dataHolder == null) return;
    final TopologyDriver data = dataHolder.getDriver();
    final HealthSnapshot snapshot = (data == null) ? new HealthSnapshot() : data.getHealthSnapshot();
    dataHolder.update(stats, snapshot);
  }

  /**
   * Get the number of data snapshots kept in memory.
   * @return the rollover position as an int value.
   */
  public synchronized int getRolloverPosition() {
    return rolloverPosition;
  }

  /**
   * Set the number of data snapshots kept in memory. If the value if less than the former values, the corresponding
   * older data snapshots will be deleted.
   * @param rolloverPosition the rollover position as an int value.
   */
  public synchronized void setRolloverPosition(final int rolloverPosition) {
    if (rolloverPosition <= 0) throw new IllegalArgumentException("zero or less not accepted: " + rolloverPosition);
    for (ConnectionDataHolder dataHolder: dataHolderMap.values()) dataHolder.setCapacity(rolloverPosition);
    this.rolloverPosition = rolloverPosition;
  }

  /**
   * Get the current number of data snapshots for the specified driver.
   * @param driver the driver for which to get the information.
   * @return the number of snapshots as an int.
   */
  public int getStatsCount(final TopologyDriver driver) {
    if (driver == null) return 0;
    final ConnectionDataHolder dataHolder = dataHolderMap.get(driver.getUuid());
    return (dataHolder == null) ? -1 : dataHolder.getSize();
  }

  /**
   * Get the mapping of a data snapshot's fields, at a specified position, to their corresponding string values.
   * @param locale the locale to use for values formatting.
   * @param driver the driver for which to get the information.
   * @param position the position to get the data at.
   * @return a map of field names to their values represented as strings.
   */
  public synchronized Map<Fields, String> getStringValues(final Locale locale, final TopologyDriver driver, final int position) {
    if (driver == null) return null;
    final ConnectionDataHolder dataHolder = dataHolderMap.get(driver.getUuid());
    if (dataHolder == null) return StatsConstants.NO_STRING_VALUES;
    final StatsFormatter formatter = getFormatter(locale);
    synchronized(formatter) {
      return formatter.formatValues(dataHolder.getDoubleValuesAt(position));
    }
  }

  /**
   * Get the mapping of the most recent data snapshot's fields to their corresponding string values.
   * @param locale the locale to use for values formatting.
   * @param driver the driver for which to get the information.
   * @return a map of field names to their values represented as strings.
   */
  public synchronized Map<Fields, String> getLatestStringValues(final Locale locale, final TopologyDriver driver) {
    if (driver == null) return StatsConstants.NO_STRING_VALUES;
    final ConnectionDataHolder dataHolder = dataHolderMap.get(driver.getUuid());
    if (dataHolder == null) return StatsConstants.NO_STRING_VALUES;
    final StatsFormatter formatter = getFormatter(locale);
    synchronized(formatter) {
      return formatter.formatValues(dataHolder.getLatestDoubleValues());
    }
  }

  /**
   * Format the value of the specified field according tot he specified locale.
   * @param locale the locale in which to format.
   * @param driver the driver to add.
   * @param field the field whose value is to be formatted.
   * @return value formatted accoridng to the locale.
   */
  public String formatLatestValue(final Locale locale, final TopologyDriver driver, final Fields field) {
    final Map<Fields, Double> map = getLatestDoubleValues(driver);
    final StatsFormatter formatter = getFormatter(locale);
    synchronized(formatter) {
      return formatter.formatValue(field, map.get(field));
    }
  }

  /**
   * Get the mapping of a data snapshot's fields, at a specified position, to their corresponding double values.
   * @param driver the driver for which to get the information.
   * @param position the position to get the data at.
   * @return a map of field names to their values represented as double values.
   */
  public synchronized Map<Fields, Double> getDoubleValues(final TopologyDriver driver, final int position) {
    if (driver == null) return null;
    final ConnectionDataHolder dataHolder = dataHolderMap.get(driver.getUuid());
    return (dataHolder == null) ? null : dataHolder.getDoubleValuesAt(position);
  }

  /**
   * Get the mapping of the most recent data snapshot's fields to their corresponding double values.
   * @param driver the driver for which to get the information.
   * @return a map of field names to their values represented as double values.
   */
  public Map<Fields, Double> getLatestDoubleValues(final TopologyDriver driver) {
    if (driver == null) return null;
    final ConnectionDataHolder dataHolder = dataHolderMap.get(driver.getUuid());
    return (dataHolder == null) ? null : dataHolder.getLatestDoubleValues();
  }

  /**
   * Register a <code>StatsHandlerListener</code> with this stats formatter.
   * @param listener the listener to register.
   */
  public void addStatsHandlerListener(final StatsHandlerListener listener) {
    if (debugEnabled) log.debug("adding stats handler listener {}", listener);
    if (listener != null) listeners.add(listener);
  }

  /**
   * Unregister a <code>StatsHandlerListener</code> from this stats formatter.
   * @param listener the listener to unregister.
   */
  public void removeStatsHandlerListener(final StatsHandlerListener listener) {
    if (debugEnabled) log.debug("removing stats handler listener {}", listener);
    if (listener != null) listeners.remove(listener);
  }

  /**
   * Notify all listeners of a change in this stats formatter.
   * @param type the type of event to fire.
   */
  public void fireStatsHandlerEvent(final StatsHandlerEvent.Type type) {
    if (log.isTraceEnabled()) log.trace("firing stats handler event of type {}", type);
    final StatsHandlerEvent event = new StatsHandlerEvent(this, type);
    for (StatsHandlerListener listener: listeners) {
      try {
        listener.dataUpdated(event);
      } catch (final Exception e) {
        log.error(e.getMessage(), e);
      }
    }
  }

  /**
   * Get the number of data updates so far.
   * @return the number of updates as an int.
   */
  public int getTickCount() {
    return tickCount;
  }

  /**
   * Get, and create  if needed, the formatter for the specified locale. 
   * @param locale the locale to use for values formatting.
   * @return a {@link StatsFormatter} instance.
   */
  protected StatsFormatter getFormatter(final Locale locale) {
    return new StatsFormatter(locale);
  }

  /**
   * @return the object that handles the toggle for showing host names vs. IP addresses.
   */
  public ShowIPHandler getShowIPHandler() {
    return showIPHandler;
  }
}
