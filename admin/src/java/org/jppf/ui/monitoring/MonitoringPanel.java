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
package org.jppf.ui.monitoring;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

import org.jppf.ui.layout.WrapLayout;
import org.jppf.ui.monitoring.data.*;
import org.jppf.ui.monitoring.event.*;
import org.jppf.utils.LocalizationUtils;
import org.slf4j.*;

import net.miginfocom.swing.MigLayout;

/**
 * This class provides a graphical interface for monitoring the status and health
 * of the JPPF server.<br>
 * It also provides a few customization options, such as setting the interval between 2 server refreshes,
 * and switching the color scheme (skin) fot the whole UI.
 * @author Laurent Cohen
 */
public class MonitoringPanel extends JPanel implements StatsHandlerListener, StatsConstants {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(MonitoringPanel.class);
  /**
   * Base name for localization bundle lookups.
   */
  private static final String BASE = "org.jppf.ui.i18n.StatsPage";
  /**
   * The stats formatter that provides the data.
   */
  private transient StatsHandler statsHandler = null;
  /**
   * Holds a list of table models to update when new stats are received.
   */
  private java.util.List<MonitorTableModel> tableModels = new ArrayList<>();
  /**
   * The aximum width of a label in the first column in each table.
   */
  private int maxLabelWidth = 0;

  /**
   * Default constructor.
   */
  public MonitoringPanel() {
    this.statsHandler = StatsHandler.getInstance();
    WrapLayout wl = new WrapLayout(FlowLayout.LEADING);
    wl.setAlignOnBaseline(true);
    setLayout(wl);
    Map<Fields[], String> map = new LinkedHashMap<>();
    map.put(EXECUTION_PROPS, "ExecutionTable");
    map.put(NODE_EXECUTION_PROPS, "NodeExecutionTable");
    map.put(TRANSPORT_PROPS, "NetworkOverheadTable");
    map.put(CONNECTION_PROPS, "ConnectionsTable");
    map.put(QUEUE_PROPS, "QueueTable");
    map.put(JOB_PROPS, "JobTable");
    map.put(NODE_CL_REQUEST_TIME_PROPS, "NodeClassLoadingRequestTable");
    map.put(CLIENT_CL_REQUEST_TIME_PROPS, "ClientClassLoadingRequestTable");
    map.put(INBOUND_NETWORK_TRAFFIC_PROPS, "InboundTrafficTable");
    map.put(OUTBOUND_NETWORK_TRAFFIC_PROPS, "OutboundTrafficTable");
    JTable tmp = new JTable();
    FontMetrics metrics = tmp.getFontMetrics(tmp.getFont());
    for (Map.Entry<Fields[], String> entry: map.entrySet()) {
      int n = computeMaxWidth(entry.getKey(), metrics);
      if (n > maxLabelWidth) maxLabelWidth = n;
    }
    for (Map.Entry<Fields[], String> entry: map.entrySet()) addTablePanel(entry.getKey(), entry.getValue());
    statsHandler.addStatsHandlerListener(this);
    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(final ComponentEvent e) {
        MonitoringPanel.this.revalidate();
      }
    });
  }

  /**
   * Add a table panel to this panel.
   * @param fields the fields displayed in the table.
   * @param title the reference to the localized title of the table.
   */
  private void addTablePanel(final Fields[] fields, final String title) {
    JComponent comp = makeTablePanel(fields, LocalizationUtils.getLocalized(BASE, title + ".label"));
    comp.setToolTipText(LocalizationUtils.getLocalized(BASE, title + ".tooltip"));
    add(comp);
  }

  /**
   * Called when new stats have been received from the server.
   * @param event holds the new stats values.
   */
  @Override
  public void dataUpdated(final StatsHandlerEvent event) {
    for (final MonitorTableModel model: tableModels) {
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          model.fireTableDataChanged();
        }
      });
    }
  }

  /**
   * Create a chartPanel displaying a group of values.
   * @param fields the names of the values to display.
   * @param title the title of the chartPanel.
   * @return a <code>JComponent</code> instance.
   */
  private JComponent makeTablePanel(final Fields[] fields, final String title) {
    JPanel panel = new JPanel();
    panel.setAlignmentY(0f);
    panel.setLayout(new MigLayout("fill"));
    panel.setBorder(BorderFactory.createTitledBorder(title));
    JTable table = new JTable() {
      @Override
      public boolean isCellEditable(final int row, final int column) {
        return false;
      }
    };
    MonitorTableModel model = new MonitorTableModel(fields);
    table.setModel(model);
    table.setOpaque(true);
    DefaultTableCellRenderer rend1 = new DefaultTableCellRenderer();
    rend1.setHorizontalAlignment(SwingConstants.RIGHT);
    rend1.setOpaque(true);
    table.getColumnModel().getColumn(1).setCellRenderer(rend1);
    DefaultTableCellRenderer rend0 = new DefaultTableCellRenderer();
    rend0.setHorizontalAlignment(SwingConstants.LEFT);
    rend0.setOpaque(true);
    table.getColumnModel().getColumn(0).setCellRenderer(rend0);
    table.getColumnModel().getColumn(0).setMinWidth(maxLabelWidth + 10);
    tableModels.add(model);
    panel.add(table, "growx, pushx");
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    table.doLayout();
    table.setShowGrid(false);
    return panel;
  }

  /**
   * Compute the maximum width of the fileds names in the given font.
   * @param fields the fields to compute from.
   * @param metrics the font used to compute the width.
   * @return the maximum width in pixels.
   */
  private int computeMaxWidth(final Fields[] fields, final FontMetrics metrics) {
    int max = 0;
    for (Fields field: fields) {
      int n = metrics.stringWidth(field.toString());
      if (n > max) max = n;
    }
    return max;
  }
}
