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

import java.awt.*;
import java.util.List;
import java.util.concurrent.*;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.load.balancer.LoadBalancingInformation;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.ui.monitoring.diagnostics.Thresholds;
import org.jppf.ui.monitoring.event.StatsHandlerEvent;
import org.jppf.ui.options.*;
import org.jppf.ui.treetable.AbstractTreeCellRenderer;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 * @since 5.0
 */
public class ClientHandler implements ClientListener {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ClientHandler.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * JPPF client used to submit execution requests.
   */
  private JPPFClient jppfClient = null;
  /**
   * The current client connection for which statistics and charts are displayed.
   */
  JPPFClientConnection currentConnection = null;
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
  private ExecutorService executor = Executors.newFixedThreadPool(1, new JPPFThreadFactory("StasHandler"));

  /**
   * 
   * @param statsHandler the stats handler.
   */
  ClientHandler(final StatsHandler statsHandler) {
    this.statsHandler = statsHandler;
    getJppfClient();
  }

  /**
   * Notify this listener that a new driver connection was created.
   * @param event the event to notify this listener of.
   */
  @Override
  public synchronized void newConnection(final ClientEvent event) {
    final JPPFClientConnection c = event.getConnection();
    JPPFClientConnectionStatus status = c.getStatus();
    if ((status != null) && status.isWorkingStatus()) executor.submit(new NewConnectionTask(statsHandler, c));
    else c.addClientConnectionStatusListener(new ClientConnectionStatusListener() {
      @Override
      public void statusChanged(final ClientConnectionStatusEvent event) {
        if (c.getStatus().isWorkingStatus()) executor.submit(new NewConnectionTask(statsHandler, c));
      }
    });
  }

  @Override
  public synchronized void connectionFailed(final ClientEvent event) {
    if ((jppfClient != null) && !jppfClient.isClosed()) executor.submit(new ConnectionFailedTask(statsHandler, event.getConnection()));
  }

  /**
   * Get the current client connection for which statistics and charts are displayed.
   * @return a <code>JPPFClientConnection</code> instance.
   */
  public synchronized JPPFClientConnection getCurrentConnection() {
    return currentConnection;
  }

  /**
   * Set the current client connection for which statistics and charts are displayed.
   * @param connection a <code>JPPFClientConnection</code> instance.
   */
  public synchronized void setCurrentConnection(final JPPFClientConnection connection) {
    if ((currentConnection == null) || ((connection != null) && !connectionId(connection).equals(connectionId(currentConnection)))) {
      executor.submit(new Runnable() {
        @Override public void run() {
          final boolean currentConnectionNull = (currentConnection == null);
          if (connection != null) {
            synchronized(statsHandler) {
              currentConnection = connection;
              JPPFClientConnectionStatus status = currentConnection.getStatus();
              if ((status != null) && status.isWorkingStatus()) {
                statsHandler.fireStatsHandlerEvent(StatsHandlerEvent.Type.RESET);
                if (currentConnectionNull) executor.submit(new Runnable() {
                  @Override public void run() {
                    log.info("first refreshLoadBalancer()");
                    refreshLoadBalancer();
                  }
                });
              }
            }
          }
        }
      });
    }
  }

  /**
   * Get the identifier for the specified connection.
   * @param c the connection for which to get the identifier.
   * @return the identifier as a string, or {@code null} if the ocnnection is {@code null}.
   */
  public String connectionId(final JPPFClientConnection c) {
    return (c == null) ? null : c.getDriverUuid();
  }

  /**
   * Refresh the load balancer settings view for the currently slected driver.
   */
  @SuppressWarnings("unchecked")
  public void refreshLoadBalancer() {
    OptionElement option = getServerListOption();
    if (option == null) {
      log.info("server chooser is null");
      return;
    }
    JMXDriverConnectionWrapper connection = currentJmxConnection();
    AbstractOption messageArea = (AbstractOption) option.findFirstWithName("/LoadBalancingMessages");
    if ((connection == null) || !connection.isConnected()) {
      messageArea.setValue("Not connected to a server, please click on 'Refresh' to try again");
      return;
    }
    messageArea.setValue("");
    try {
      LoadBalancingInformation info = connection.loadBalancerInformation();
      log.info("info = {}", info);
      if (info != null) {
        ComboBoxOption combo = (ComboBoxOption) option.findFirstWithName("/Algorithm");
        List items = combo.getItems();
        if ((items == null) || items.isEmpty()) combo.setItems(info.getAlgorithmNames());
        combo.setValue(info.getAlgorithm());
        AbstractOption params = (AbstractOption) option.findFirstWithName("/LoadBalancingParameters");
        params.setValue(info.getParameters().asString());
      }
    }
    catch(Exception ignore) {
    }
  }

  /**
   * Get the JPPF client used to submit data update and administration requests.
   * @return a <code>JPPFClient</code> instance.
   */
  public JPPFClient getJppfClient() {
    return getJppfClient(null);
  }

  /**
   * Get the JPPF client used to submit data update and administration requests.
   * @param clientListener a listener to register with the JPPF client.
   * @return a <code>JPPFClient</code> instance.
   */
  public synchronized JPPFClient getJppfClient(final ClientListener clientListener) {
    if (jppfClient == null) {
      jppfClient = (clientListener == null) ? new JPPFClient(this) : new JPPFClient(this, clientListener);
    } else if (clientListener != null) {
      jppfClient.addClientListener(clientListener);
    }
    return jppfClient;
  }

  /**
   * Get the JMX connection for the current driver connection.
   * @return a <code>JMXDriverConnectionWrapper</code> instance.
   */
  public JMXDriverConnectionWrapper currentJmxConnection() {
    JPPFClientConnection c = getCurrentConnection();
    if (c == null) return null;
    return c.getConnectionPool().getJmxConnection();
  }

  /**
   * Get the option containing the combobox with the list of driver connections.
   * @return an <code>OptionElement</code> instance.
   */
  public synchronized OptionElement getServerListOption() {
    return serverListOption;
  }

  /**
   * Set the option containing the combobox with the list of driver connections.
   * @param serverListOption an <code>OptionElement</code> instance.
   */
  public synchronized void setServerListOption(final OptionElement serverListOption) {
    this.serverListOption = serverListOption;
    List<JPPFClientConnection> list = getJppfClient().getAllConnections();
    if (debugEnabled) log.debug("setting serverList option=" + serverListOption + ", connections = " + list);
    for (JPPFClientConnection c: list) executor.submit(new NewConnectionTask(statsHandler, c));
    notifyAll();
  }

  /**
   * Close all connections to the driver(s).
   */
  public void close() {
    if (jppfClient != null) jppfClient.close();
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
    if (debugEnabled) log.debug("getting intervals for {}", field);
    switch(field) {
      case HEALTH_HEAP_PCT:
      case HEALTH_NON_HEAP_PCT:
        return getMeterIntervals(Thresholds.Name.MEMORY_WARNING, Thresholds.Name.MEMORY_CRITICAL);
      case HEALTH_CPU:
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
        double lastValue = 0d;
        int len = names.length + 2;
        double[] values = new double[len];
        // convert from [name1, ..., nameN] to [0, value1, ..., valueN, 100] 
        values[0] = 0d;
        for (int i=0; i<names.length; i++) {
          values[i+1] = 100d * thresholds.getValue(names[i]);
          if (debugEnabled) log.debug("value for '{}' = {}", names[i], values[i+1]);
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
