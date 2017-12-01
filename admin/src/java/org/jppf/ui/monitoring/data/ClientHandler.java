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

import java.awt.*;
import java.util.List;
import java.util.concurrent.*;

import javax.swing.*;

import org.jppf.client.*;
import org.jppf.client.monitoring.topology.*;
import org.jppf.load.balancer.LoadBalancingInformation;
import org.jppf.management.*;
import org.jppf.ui.monitoring.diagnostics.Thresholds;
import org.jppf.ui.monitoring.event.*;
import org.jppf.ui.options.*;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.ui.treetable.AbstractTreeCellRenderer;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.JPPFThreadFactory;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 * @since 5.0
 */
public class ClientHandler extends TopologyListenerAdapter implements AutoCloseable {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ClientHandler.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Determines whether trace log statements are enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * The current client connection for which statistics and charts are displayed.
   */
  TopologyDriver currentDriver = null;
  /**
   * The stats handler.
   */
  private final StatsHandler statsHandler;
  /**
   * Option containing the combobox with the list of driver connections.
   */
  private OptionElement serverListOption = null;
  /**
   * The threshold values.
   */
  private final Thresholds thresholds = new Thresholds();
  /**
   * Thread pool used to process new connection events.
   */
  private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, new JPPFThreadFactory("StatScheduler"));
  /**
   * Monitors and maintains a representation of the grid topology.
   */
  private final TopologyManager manager;

  /**
   *
   * @param statsHandler the stats handler.
   */
  ClientHandler(final StatsHandler statsHandler) {
    this.statsHandler = statsHandler;
    manager = statsHandler.getTopologyManager();
    manager.addTopologyListener(this);
    statsHandler.getShowIPHandler().addShowIPListener(new ShowIPListener() {
      @Override
      public void stateChanged(final ShowIPEvent event) {
        if (serverListOption != null) serverListOption.getUIComponent().repaint();
      }
    });
  }

  @Override
  public void driverAdded(final TopologyEvent event) {
    scheduler.submit(new NewConnectionTask(statsHandler.getRolloverPosition(), statsHandler, event.getDriver()));
  }

  @Override
  public void driverRemoved(final TopologyEvent event) {
    if (!manager.getJPPFClient().isClosed()) scheduler.submit(new ConnectionFailedTask(statsHandler, event.getDriver()));
  }

  /**
   * Get the current client connection for which statistics and charts are displayed.
   * @return a <code>TopologyDriver</code> instance.
   */
  public synchronized TopologyDriver getCurrentDriver() {
    return currentDriver;
  }

  /**
   * Set the current client connection for which statistics and charts are displayed.
   * @param driver a <code>JPPFClientConnection</code> instance.
   */
  public synchronized void setCurrentDriver(final TopologyDriver driver) {
    if ((currentDriver == null) || ((driver != null) && !driver.getUuid().equals(currentDriver.getUuid()))) {
      scheduler.submit(new SetCurrentConnectionTask(driver));
    }
  }

  /**
   * Task submitted when changing the current connection.
   */
  private class SetCurrentConnectionTask implements Runnable {
    /**
     * The connection to set.
     */
    private final TopologyDriver driver;

    /**
     * Initialize this task with the specified client connection.
     * @param driver the connection to set.
     */
    public SetCurrentConnectionTask(final TopologyDriver driver) {
      this.driver = driver;
    }

    @Override
    public void run() {
      final boolean currentDriverNull = (currentDriver == null);
      if (driver != null) {
        synchronized(statsHandler) {
          currentDriver = driver;
          JPPFClientConnectionStatus status = currentDriver.getConnection().getStatus();
          if (status.isWorkingStatus()) {
            statsHandler.fireStatsHandlerEvent(StatsHandlerEvent.Type.RESET);
            if (currentDriverNull) {
              Runnable r = new Runnable() {
                @Override public void run() {
                  log.debug("first refreshLoadBalancer()");
                  // to cancel the task
                  if (refreshLoadBalancer()) throw new IllegalStateException("");
                }
              };
              scheduler.scheduleWithFixedDelay(r, 0L, 1000L, TimeUnit.MILLISECONDS);
            }
          }
        }
      }
    }
  }

  /**
   * Refresh the load balancer settings view for the currently slected driver.
   * @return {@code true} to indicate success, {@code false} otherwise.
   */
  public boolean refreshLoadBalancer() {
    OptionElement option = OptionsHandler.getPage("JPPFAdminTool");
    if (option == null) {
      log.debug("JPPFAdminTool element is null");
      return false;
    }
    OptionElement lbOption = OptionsHandler.findOptionWithName(option, "LoadBalancingPanel");
    if (lbOption == null) {
      log.debug("LoadBalancingPanel element is null");
      return false;
    }
    log.debug("LoadBalancingPanel = " + lbOption);
    JMXDriverConnectionWrapper jmx = currentJmxConnection();
    AbstractOption messageArea = (AbstractOption) lbOption.findFirstWithName("/LoadBalancingMessages");
    if ((jmx == null) || !jmx.isConnected()) {
      messageArea.setValue("Not connected to a server, please click on 'Refresh' to try again");
      return false;
    }
    messageArea.setValue("");
    try {
      LoadBalancingInformation info = jmx.loadBalancerInformation();
      log.debug("info = {}", info);
      if (info != null) {
        ComboBoxOption combo = (ComboBoxOption) lbOption.findFirstWithName("/Algorithm");
        List<? extends Object> items = combo.getItems();
        if ((items == null) || items.isEmpty()) combo.setItems(info.getAlgorithmNames());
        combo.setValue(info.getAlgorithm());
        AbstractOption params = (AbstractOption) lbOption.findFirstWithName("/LoadBalancingParameters");
        params.setValue(info.getParameters().asString());
        return true;
      }
    } catch(Exception e) {
      log.error(e.getMessage(), e);
    }
    return false;
  }

  /**
   * Get the JMX connection for the current driver connection.
   * @return a <code>JMXDriverConnectionWrapper</code> instance.
   */
  public JMXDriverConnectionWrapper currentJmxConnection() {
    TopologyDriver driver = getCurrentDriver();
    return (driver == null) ? null : driver.getJmx();
  }

  /**
   * Get the option containing the combobox with the list of driver connections.
   * @return an <code>OptionElement</code> instance.
   */
  public synchronized OptionElement getServerListOption() {
    OptionElement page =  OptionsHandler.getPage("JPPFAdminTool");
    return (page == null) ? null : page.findFirstWithName("ServerChooser");
  }

  /**
   * Set the option containing the combobox with the list of driver connections.
   * @param serverListOption an <code>OptionElement</code> instance.
   */
  public synchronized void setServerListOption(final OptionElement serverListOption) {
    this.serverListOption = serverListOption;
    JComboBox<?> box = ((ComboBoxOption) serverListOption).getComboBox();
    box.setRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
        DefaultListCellRenderer renderer = (DefaultListCellRenderer) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        TopologyDriver driver = (TopologyDriver) value;
        if (driver != null) {
          JPPFManagementInfo info = driver.getManagementInfo();
          if (info != null) renderer.setText((statsHandler.getShowIPHandler().isShowIP() ? info.getIpAddress() : info.getHost()) + ":" + info.getPort());
          else renderer.setText(driver.getDisplayName());
        }
        return renderer;
      }
    });
    List<TopologyDriver> list = manager.getDrivers();
    if (debugEnabled) log.debug("setting serverList option=" + serverListOption + ", connections = " + list);
    for (TopologyDriver driver: list) scheduler.submit(new NewConnectionTask(statsHandler.getRolloverPosition(), statsHandler, driver));
    notifyAll();
  }

  /**
   * Close all connections to the driver(s).
   */
  @Override
  public void close() {
    scheduler.shutdownNow();
    JPPFClient client = manager.getJPPFClient();
    if (client != null) client.close();
  }

  /**
   * Get the threshold values.
   * @return a {@link Thresholds} object.
   */
  public Thresholds getThresholds() {
    return thresholds;
  }

  /**
   * Get the meter intervals for the psecified field.
   * @param field the field to find the interval for.
   * @return an array, possibly empty but never null, of interval objects.
   */
  public Object[] getMeterIntervals(final Fields field) {
    if (traceEnabled) log.trace("getting intervals for {}", field);
    switch(field) {
      case HEALTH_HEAP_PCT:
      case HEALTH_NON_HEAP_PCT:
      case HEALTH_RAM_PCT:
        return getMeterIntervals(Thresholds.Name.MEMORY_WARNING, Thresholds.Name.MEMORY_CRITICAL);
      case HEALTH_CPU:
      case HEALTH_SYSTEM_CPU:
        return getMeterIntervals(Thresholds.Name.CPU_WARNING, Thresholds.Name.CPU_CRITICAL);
    }
    return StringUtils.ZERO_OBJECT;
  }

  /**
   * Get the meter intervals for the spcified threshold names.
   * @param names the names of threashold values to use, the order matters.
   * @return an array of interval objects.
   */
  private Object[] getMeterIntervals(final Thresholds.Name...names) {
    if ((names != null) && (names.length > 0)) {
      try {
        int len = names.length + 2;
        double[] values = new double[len];
        // convert from [name1, ..., nameN] to [0, value1, ..., valueN, 100]
        values[0] = 0d;
        for (int i=0; i<names.length; i++) {
          values[i+1] = 100d * thresholds.getValue(names[i]);
          if (traceEnabled) log.trace("value for '{}' = {}", names[i], values[i+1]);
        }
        values[len-1] = 100d;
        Object[] intervals = new Object[len - 1];
        Class<?> intervalClass = ReflectionHelper.getClass0("org.jfree.chart.plot.MeterInterval");
        Class<?> rangeClass = ReflectionHelper.getClass0("org.jfree.data.Range");
        for (int  i=0; i<len-1; i++) {
          // Range range = new Range(values[i], values[i+1]);
          Object range = ReflectionHelper.invokeConstructor(rangeClass, new Class<?>[] {double.class, double.class}, values[i], values[i+1]);
          String label = (i == 0) ? "Normal" : LocalizationUtils.getLocalized("org.jppf.ui.i18n.NodeDataPage", names[i-1].getDisplayName());
          Class<?>[] paramTypes = {String.class, rangeClass, Paint.class, Stroke.class, Paint.class};
          Color outline = Color.WHITE;
          Color background = Color.GRAY;
          switch(label) {
            case "Warning":
              background = AbstractTreeCellRenderer.SUSPENDED_COLOR;
              break;
            case "Critical":
              background = AbstractTreeCellRenderer.INACTIVE_COLOR;
              break;
          }
          Stroke stroke = new BasicStroke(2f);
          // MeterInterval interval = new MeterInterval(label, range); intervals[i] = interval;
          intervals[i] = ReflectionHelper.invokeConstructor(intervalClass, paramTypes, label, range, outline, stroke, background );
        }
        return intervals;
      } catch(Exception e) {
        log.error(e.getMessage(), e);
      }
    }
    return StringUtils.ZERO_OBJECT;
  }
}
