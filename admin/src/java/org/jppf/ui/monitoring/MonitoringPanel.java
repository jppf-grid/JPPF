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
package org.jppf.ui.monitoring;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

import org.jppf.ui.actions.AbstractUpdatableAction;
import org.jppf.ui.layout.WrapLayout;
import org.jppf.ui.monitoring.data.*;
import org.jppf.ui.monitoring.event.*;
import org.jppf.ui.options.*;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.ui.utils.GuiUtils;
import org.jppf.utils.*;
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
   * Preference key for loading/storing the list of visible stats.
   */
  private static final String VISIBLE_STATS_KEY = "visible.server.stats";
  /**
   *
   */
  private static final String EXECUTION = "ExecutionTable";
  /**
   *
   */
  private static final String NODE_EXECUTION = "NodeExecutionTable";
  /**
   *
   */
  private static final String TRANSPORT = "NetworkOverheadTable";
  /**
   *
   */
  private static final String CONNECTION = "ConnectionsTable";
  /**
   *
   */
  private static final String QUEUE = "QueueTable";
  /**
   *
   */
  private static final String JOB = "JobTable";
  /**
   *
   */
  private static final String NODE_CL_REQUEST_TIME = "NodeClassLoadingRequestTable";
  /**
   *
   */
  private static final String CLIENT_CL_REQUEST_TIME = "ClientClassLoadingRequestTable";
  /**
   *
   */
  private static final String INBOUND_NETWORK_TRAFFIC = "InboundTrafficTable";
  /**
   *
   */
  private static final String OUTBOUND_NETWORK_TRAFFIC = "OutboundTrafficTable";
  /**
   * The stats formatter that provides the data.
   */
  private transient StatsHandler statsHandler = null;
  /**
   *
   */
  private final Map<String, Fields[]> allTablesMap = createFieldsMap();
  /**
   *
   */
  private final Map<String, Item> allItems = createItems();
  /**
   * Holds a list of table models to update when new stats are received.
   */
  private final List<MonitorTableModel> tableModels = new ArrayList<>();
  /**
   *
   */
  private final List<Item> visibleItems = new ArrayList<>();
  /**
   *
   */
  private final List<JComponent> visibleTableComps = new ArrayList<>();

  /**
   * Default constructor.
   */
  public MonitoringPanel() {
    this.statsHandler = StatsHandler.getInstance();
    WrapLayout wl = new WrapLayout(FlowLayout.LEADING);
    wl.setAlignOnBaseline(true);
    setLayout(wl);
    statsHandler.addStatsHandlerListener(this);
    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(final ComponentEvent e) {
        MonitoringPanel.this.revalidate();
      }
    });
  }

  /**
   * Add all visible tables to the view.
   */
  private void addTables() {
    for (Item item: visibleItems) addTablePanel(allTablesMap.get(item.name), item.name);
  }

  /**
   * Remove all tables from the view.
   */
  private void clearTablesFromView() {
    for (JComponent comp: visibleTableComps) remove(comp);
    visibleTableComps.clear();
    tableModels.clear();
  }

  /**
   * Add a table panel to this panel.
   * @param fields the fields displayed in the table.
   * @param title the reference to the localized title of the table.
   */
  private void addTablePanel(final Fields[] fields, final String title) {
    Item item = allItems.get(title);
    JComponent comp = makeTablePanel(fields, item.label);
    comp.setToolTipText(item.tooltip);
    add(comp);
    visibleTableComps.add(comp);
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
   * @param props the names of the values to display.
   * @param title the title of the chartPanel.
   * @return a <code>JComponent</code> instance.
   */
  private JComponent makeTablePanel(final Fields[] props, final String title) {
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
    MonitorTableModel model = new MonitorTableModel(props);
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
    table.getColumnModel().getColumn(0).setMinWidth(200);
    table.getColumnModel().getColumn(0).setMaxWidth(300);
    tableModels.add(model);
    panel.add(table, "growx, pushx");
    table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    table.doLayout();
    table.setShowGrid(false);
    return panel;
  }

  /**
   * Create a mapping of table names to the corresponding set of fields.
   * @return a map of names to {@code Field[]}.
   */
  private Map<String, Fields[]> createFieldsMap() {
    Map<String, Fields[]> map = new LinkedHashMap<>();
    addFieldsMapping(map, EXECUTION, EXECUTION_PROPS);
    addFieldsMapping(map, NODE_EXECUTION, NODE_EXECUTION_PROPS);
    addFieldsMapping(map, TRANSPORT, TRANSPORT_PROPS);
    addFieldsMapping(map, CONNECTION, CONNECTION_PROPS);
    addFieldsMapping(map, QUEUE, QUEUE_PROPS);
    addFieldsMapping(map, JOB, JOB_PROPS);
    addFieldsMapping(map, NODE_CL_REQUEST_TIME, NODE_CL_REQUEST_TIME_PROPS);
    addFieldsMapping(map, CLIENT_CL_REQUEST_TIME, CLIENT_CL_REQUEST_TIME_PROPS);
    addFieldsMapping(map, INBOUND_NETWORK_TRAFFIC, INBOUND_NETWORK_TRAFFIC_PROPS);
    addFieldsMapping(map, OUTBOUND_NETWORK_TRAFFIC, OUTBOUND_NETWORK_TRAFFIC_PROPS);
    //return map;
    return Collections.unmodifiableMap(map);
  }

  /**
   * Add a mmping of the localized specified name to a set of fields.
   * @param map the map to add the mapping to.
   * @param name the name to localize and use as a key in the map.
   * @param fields the fileds associated with the key.
   */
  private void addFieldsMapping(final Map<String, Fields[]> map, final String name, final Fields[] fields) {
    map.put(name, fields);
  }

  /**
   * Create the map of all items.
   * @return a mapping of non-localized names to {@link Item} objects.
   */
  private Map<String, Item> createItems() {
    String[] names = { EXECUTION, NODE_EXECUTION, TRANSPORT, CONNECTION, QUEUE, JOB, NODE_CL_REQUEST_TIME, CLIENT_CL_REQUEST_TIME, INBOUND_NETWORK_TRAFFIC, OUTBOUND_NETWORK_TRAFFIC };
    Map<String, Item> map = new LinkedHashMap<>();
    for (String name: names) map.put(name, new Item(name));
    //return map;
    return Collections.unmodifiableMap(map);
  }

  /**
   * Popup the pick list dialog to select the visible stats.
   * @param btn the button that produced a click event event.
   */
  public void selectStats(final AbstractButton btn) {
    try {
      Point location = ((btn != null) && btn.isShowing()) ? location = btn.getLocationOnScreen() : new Point(0, 0);
      OptionElement panel = OptionsHandler.loadPageFromXml("org/jppf/ui/options/xml/VisibleStatsPanel.xml");
      final PickListOption option = (PickListOption) panel.findFirstWithName("visible.stats.selection");
      option.populate(new ArrayList<>(allItems.values()), visibleItems);
      final JDialog dialog = new JDialog(OptionsHandler.getMainWindow(), LocalizationUtils.getLocalized(BASE, "visible.stats.panel.label"), false);
      dialog.setIconImage(GuiUtils.loadIcon("/org/jppf/ui/resources/table-column-hide.png").getImage());
      JButton applyBtn = (JButton) panel.findFirstWithName("/visible.stats.apply").getUIComponent();
      AbstractAction applyAction = new AbstractAction() {
        @Override public void actionPerformed(final ActionEvent event) {
          visibleItems.clear();
          List<Item> value = (List<Item>) option.getPickList().getPickedItems();
          visibleItems.addAll(value);
          clearTablesFromView();
          addTables();
          MonitoringPanel.this.repaint();
        }
      };
      applyBtn.addActionListener(applyAction);
      JButton closeBtn = (JButton) panel.findFirstWithName("/visible.stats.close").getUIComponent();
      AbstractAction closeAction = new AbstractAction() {
        @Override public void actionPerformed(final ActionEvent event) {
          dialog.setVisible(false);
          dialog.dispose();
        }
      };
      closeBtn.addActionListener(closeAction);
      AbstractUpdatableAction.setOkCancelKeys(panel, applyAction, closeAction);
      dialog.getContentPane().add(panel.getUIComponent());
      dialog.pack();
      dialog.setLocationRelativeTo(null);
      if (location != null) dialog.setLocation(location);
      dialog.setVisible(true);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Load the ist of visible stats from the prefrences.
   */
  public void loadVisibleStats() {
    visibleItems.clear();
    Preferences pref = OptionsHandler.getPreferences();
    String s = pref.get(VISIBLE_STATS_KEY, null);
    if (s != null) {
      String[] names = RegexUtils.SPACES_PATTERN.split(s);
      for (String name: names) {
        Item item = allItems.get(name);
        if (item != null) visibleItems.add(item);
      }
    }
    else visibleItems.addAll(allItems.values());
    addTables();
  }

  /**
   * Store the ist of visible stats into the prefrences.
   */
  public void storeVisibleStats() {
    Preferences pref = OptionsHandler.getPreferences();
    StringBuilder sb = new StringBuilder();
    int count  = 0;
    for (Item item: visibleItems) {
      if (count > 0) sb.append(' ');
      sb.append(item.name);
      count++;
    }
    pref.put(VISIBLE_STATS_KEY, sb.toString());
  }

  /**
   * Instances of this class are the items displayed int he pick list.
   */
  public static class Item {
    /**
     * The non-localized name of this item
     */
    public final String name;
    /**
     * The localized name of this item
     */
    public final String label;
    /**
     * The localized name of this item
     */
    public final String tooltip;

    /**
     * Initialize this item.
     * @param name the non-localized name of this item.
     */
    public Item(final String name) {
      this.name = name;
      this.label = LocalizationUtils.getLocalized(BASE, name + ".label");
      this.tooltip = LocalizationUtils.getLocalized(BASE, name + ".tooltip");
    }

    @Override
    public String toString() {
      return label;
    }
  }
}
