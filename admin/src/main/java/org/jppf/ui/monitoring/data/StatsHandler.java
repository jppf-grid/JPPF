/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

import org.jppf.client.monitoring.jobs.*;
import org.jppf.client.monitoring.topology.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.management.diagnostics.MonitoringDataProviderHandler;
import org.jppf.ui.monitoring.event.StatsHandlerEvent;
import org.jppf.ui.utils.GuiUtils;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.jppf.utils.stats.JPPFStatistics;
import org.slf4j.*;

/**
 * This class provides a convenient access to the statistics obtained from the JPPF server.
 * @author Laurent Cohen
 */
public final class StatsHandler extends BaseStatsHandler {
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
  private static StatsHandler instance;
  /**
   * Timer used to query the stats from the server.
   */
  protected java.util.Timer timer;
  /**
   * The client handler.
   */
  private final ClientHandler clientHandler;
  /**
   * The object which monitors and maintains a representation of the grid topology.
   */
  private final TopologyManager topologyManager;
  /**
   * The object which monitors and maintains a representation of the jobs hierarchy.
   */
  private final JobMonitor jobMonitor;
  /**
   * Localized formatter for stats values.
   */
  private final StatsFormatter formatter = new StatsFormatter(Locale.getDefault());

  /**
   * Get the singleton instance of this class.
   * @return a <code>StatsHandler</code> instance.
   */
  public synchronized static StatsHandler getInstance() {
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
    MonitoringDataProviderHandler.getProviders();
    MonitoringDataProviderHandler.getAllProperties();
    refreshInterval = JPPFConfiguration.getProperties().get(JPPFProperties.ADMIN_REFRESH_INTERVAL_STATS);
    if (refreshInterval > 0L) timer = new java.util.Timer("JPPF Driver Statistics Update Timer");
    if (debugEnabled) log.debug("initializing TopologyManager");
    topologyManager = new TopologyManager();
    if (debugEnabled) log.debug("initializing JobMonitor");
    final String modeStr = JPPFConfiguration.get(JPPFProperties.GUI_PUBLISH_MODE);
    final long period = JPPFConfiguration.get(JPPFProperties.GUI_PUBLISH_PERIOD);
    JobMonitorUpdateMode mode = JobMonitorUpdateMode.IMMEDIATE_NOTIFICATIONS;
    if ("deferred_notifications".equalsIgnoreCase(modeStr)) mode = JobMonitorUpdateMode.DEFERRED_NOTIFICATIONS;
    else if ("polling".equalsIgnoreCase(modeStr)) mode = JobMonitorUpdateMode.POLLING;
    jobMonitor = new JobMonitor(mode, period, topologyManager);
    if (debugEnabled) log.debug("initializing ClientHandler");
    clientHandler = new ClientHandler(this);
    if (debugEnabled) log.debug("done initializing StatsHandler");
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
   * Request an update from the current server connection.
   */
  public void requestUpdate() {
    if (debugEnabled) log.debug("stats update requested");
    final ConnectionDataHolder cdh = getCurrentDataHolder();
    if (cdh != null) requestUpdate(cdh.getDriver());
  }

  @Override
  public synchronized void update(final TopologyDriver driver, final JPPFStatistics stats) {
    super.update(driver, stats);
    final TopologyDriver current = clientHandler.getCurrentDriver();
    if ((current != null) && driver.getUuid().equals(current.getUuid())) fireStatsHandlerEvent(StatsHandlerEvent.Type.UPDATE);
  }

  /**
   * Get the current number of data snapshots.
   * @return the number of snapshots as an int.
   */
  public int getStatsCount() {
    final TopologyDriver driver = clientHandler.getCurrentDriver();
    return (driver == null) ? 0 : super.getStatsCount(driver);
  }

  /**
   * Return the {@link ConnectionDataHolder} for the current connection.
   * @return a {@link ConnectionDataHolder} instance.
   */
  public synchronized ConnectionDataHolder getCurrentDataHolder() {
    final TopologyDriver driver = clientHandler.getCurrentDriver();
    return (driver == null) ? null : dataHolderMap.get(driver.getUuid());
  }

  /**
   * Get the mapping of a data snapshot's fields, at a specified position, to their corresponding string values.
   * @param position the position to get the data at.
   * @return a map of field names to their values represented as strings.
   */
  public synchronized Map<Fields, String> getStringValues(final int position) {
    final TopologyDriver driver = clientHandler.getCurrentDriver();
    return (driver == null) ? null : getStringValues(Locale.getDefault(), driver, position);
  }

  /**
   * Get the mapping of the most recent data snapshot's fields to their corresponding string values.
   * @return a map of field names to their values represented as strings.
   */
  public Map<Fields, String> getLatestStringValues() {
    final TopologyDriver driver = clientHandler.getCurrentDriver();
    return (driver == null) ? StatsConstants.NO_STRING_VALUES : getLatestStringValues(Locale.getDefault(), driver);
  }

  /**
   * Get the mapping of a data snapshot's fields, at a specified position, to their corresponding double values.
   * @param position the position to get the data at.
   * @return a map of field names to their values represented as double values.
   */
  public synchronized Map<Fields, Double> getDoubleValues(final int position) {
    final TopologyDriver driver = clientHandler.getCurrentDriver();
    return (driver == null) ? null : getDoubleValues(driver, position);
  }

  /**
   * Get the mapping of the most recent data snapshot's fields to their corresponding double values.
   * @return a map of field names to their values represented as double values.
   */
  public Map<Fields, Double> getLatestDoubleValues() {
    final TopologyDriver driver = clientHandler.getCurrentDriver();
    return (driver == null) ? null : getLatestDoubleValues(driver);
  }

  /**
   * Copy the currently displayed statistics to the clipboard.
   * @param format specifies the format, among the constants defined in {@link StatsExporter}.
   */
  public void copyStatsToClipboard(final int format) {
    try {
      final TopologyDriver driver = getCurrentDataHolder().getDriver();
      final StatsExporter exporter = (format == StatsExporter.CSV) ? new CsvStatsExporter(this, driver) : new TextStatsExporter(this, driver, Locale.getDefault());
      final String text = exporter.formatAll();
      final Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
      clip.setContents(new StringSelection(text), null);
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Reset the statistics of the currently selected driver, if any.
   */
  public void resetCurrentStats() {
    if (debugEnabled) log.debug("resetting current stats");
    final Runnable r = new Runnable() {
      @Override
      public void run() {
        final TopologyDriver driver = clientHandler.getCurrentDriver();
        if (driver == null) return;
        final JMXDriverConnectionWrapper jmx = driver.getJmx();
        if ((jmx != null) && jmx.isConnected()) {
          try {
            jmx.resetStatistics();
          } catch (final Exception e) {
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

  /**
   * Get the object which monitors and maintains a representation of the jobs hierarchy.
   * @return a {@link JobMonitor} instance.
   */
  public JobMonitor getJobMonitor() {
    return jobMonitor;
  }

  @Override
  protected StatsFormatter getFormatter(final Locale locale) {
    return formatter;
  }
}
