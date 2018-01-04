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
public class MonitoringPanel extends JPanel implements StatsHandlerListener {
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
   * The stats formatter that provides the data.
   */
  private transient StatsHandler statsHandler = null;
  /**
   * Mapping of table names to the corresponding items in the pick list for selecting the visible tables.
   */
  protected final Map<String, LocalizedListItem> allItems = StatsConstants.createLocalizedItems(Locale.getDefault());
  /**
   * Holds a list of table models to update when new stats are received.
   */
  private final List<MonitorTableModel> tableModels = new ArrayList<>();
  /**
   * The list of displayed (visible) tables.
   */
  private final List<LocalizedListItem> visibleItems = new ArrayList<>();
  /**
   * The list of displayed (visible) tables components.
   */
  private final List<JComponent> visibleTableComps = new ArrayList<>();
  /**
   * The aximum width of a label in the first column in each table.
   */
  private int maxLabelWidth = 0;

  /**
   * Default constructor.
   */
  public MonitoringPanel() {
    this.statsHandler = StatsHandler.getInstance();
    final JTable tmp = new JTable();
    final FontMetrics metrics = tmp.getFontMetrics(tmp.getFont());
    for (final Map.Entry<String, Fields[]> entry: StatsConstants.ALL_TABLES_MAP.entrySet()) {
      final int n = computeMaxWidth(entry.getValue(), metrics);
      if (n > maxLabelWidth) maxLabelWidth = n;
    }
    final WrapLayout wl = new WrapLayout(FlowLayout.LEADING);
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
    for (LocalizedListItem item: visibleItems) addTablePanel(StatsConstants.ALL_TABLES_MAP.get(item.name), item.name);
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
    final LocalizedListItem item = allItems.get(title);
    final JComponent comp = makeTablePanel(fields, item.label);
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
   * @return a {@code JComponent} instance.
   */
  private JComponent makeTablePanel(final Fields[] props, final String title) {
    final JPanel panel = new JPanel();
    panel.setAlignmentY(0f);
    panel.setLayout(new MigLayout("fill"));
    panel.setBorder(BorderFactory.createTitledBorder(title));
    final JTable table = new JTable() {
      @Override
      public boolean isCellEditable(final int row, final int column) {
        return false;
      }
    };
    final MonitorTableModel model = new MonitorTableModel(props);
    table.setModel(model);
    table.setOpaque(true);
    final DefaultTableCellRenderer rend1 = new DefaultTableCellRenderer();
    rend1.setHorizontalAlignment(SwingConstants.RIGHT);
    rend1.setOpaque(true);
    table.getColumnModel().getColumn(1).setCellRenderer(rend1);
    final DefaultTableCellRenderer rend0 = new DefaultTableCellRenderer();
    rend0.setHorizontalAlignment(SwingConstants.LEFT);
    rend0.setOpaque(true);
    table.getColumnModel().getColumn(0).setCellRenderer(rend0);
    table.getColumnModel().getColumn(0).setMinWidth(maxLabelWidth + 10);
    //table.getColumnModel().getColumn(0).setMaxWidth(300);
    tableModels.add(model);
    panel.add(table, "growx, pushx");
    table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    table.doLayout();
    table.setShowGrid(false);
    return panel;
  }

  /**
   * Popup the pick list dialog to select the visible stats.
   * @param btn the button that produced a click event event.
   */
  public void selectStats(final AbstractButton btn) {
    try {
      Point location = ((btn != null) && btn.isShowing()) ? location = btn.getLocationOnScreen() : new Point(0, 0);
      final OptionElement panel = OptionsHandler.loadPageFromXml("org/jppf/ui/options/xml/VisibleStatsPanel.xml");
      final PickListOption option = (PickListOption) panel.findFirstWithName("visible.stats.selection");
      option.populate(new ArrayList<Object>(allItems.values()), new ArrayList<Object>(visibleItems));
      final JDialog dialog = new JDialog(OptionsHandler.getMainWindow(), LocalizationUtils.getLocalized(BASE, "visible.stats.panel.label"), false);
      dialog.setIconImage(GuiUtils.loadIcon("/org/jppf/ui/resources/table-column-hide.png").getImage());
      final JButton applyBtn = (JButton) panel.findFirstWithName("/visible.stats.apply").getUIComponent();
      final AbstractAction applyAction = new AbstractAction() {
        @Override public void actionPerformed(final ActionEvent event) {
          visibleItems.clear();
          final List<Object> picked = option.getPickList().getPickedItems();
          final List<LocalizedListItem> value = (picked == null) ? new ArrayList<LocalizedListItem>() : new ArrayList<LocalizedListItem>(picked.size());
          for (final Object o: picked) value.add((LocalizedListItem) o);
          visibleItems.addAll(value);
          clearTablesFromView();
          addTables();
          MonitoringPanel.this.repaint();
          MonitoringPanel.this.updateUI();
        }
      };
      applyBtn.addActionListener(applyAction);
      final JButton closeBtn = (JButton) panel.findFirstWithName("/visible.stats.close").getUIComponent();
      final AbstractAction closeAction = new AbstractAction() {
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
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Load the ist of visible stats from the prefrences.
   */
  public void loadVisibleStats() {
    visibleItems.clear();
    final Preferences pref = OptionsHandler.getPreferences();
    final String s = pref.get(VISIBLE_STATS_KEY, null);
    if (s != null) {
      final String[] names = RegexUtils.SPACES_PATTERN.split(s);
      for (final String name: names) {
        final LocalizedListItem item = allItems.get(name);
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
    final Preferences pref = OptionsHandler.getPreferences();
    final StringBuilder sb = new StringBuilder();
    int count  = 0;
    for (final LocalizedListItem item: visibleItems) {
      if (count > 0) sb.append(' ');
      sb.append(item.name);
      count++;
    }
    pref.put(VISIBLE_STATS_KEY, sb.toString());
  }

  /**
   * Compute the maximum width of the fileds names in the given font.
   * @param fields the fields to compute from.
   * @param metrics the font used to compute the width.
   * @return the maximum width in pixels.
   */
  private static int computeMaxWidth(final Fields[] fields, final FontMetrics metrics) {
    int max = 0;
    for (final Fields field: fields) {
      final int n = metrics.stringWidth(field.toString());
      if (n > max) max = n;
    }
    return max;
  }
}
