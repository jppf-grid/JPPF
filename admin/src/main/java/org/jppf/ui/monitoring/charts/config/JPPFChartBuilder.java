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
package org.jppf.ui.monitoring.charts.config;

import static org.jppf.ui.monitoring.charts.ChartType.*;

import java.awt.Color;
import java.util.*;

import javax.swing.*;

import org.jppf.ui.monitoring.charts.*;
import org.jppf.ui.monitoring.data.StatsHandler;
import org.jppf.ui.monitoring.event.*;
import org.jppf.ui.utils.GuiUtils;
import org.knowm.xchart.CategorySeries.CategorySeriesRenderStyle;
import org.knowm.xchart.PieSeries.PieSeriesRenderStyle;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYSeries.XYSeriesRenderStyle;
import org.knowm.xchart.internal.chartpart.Chart;
import org.slf4j.*;

/**
 * This class is used as a factory to create different charts, as well as for propagating the data updates
 * to all defined charts.
 * @author Laurent Cohen
 */
public class JPPFChartBuilder extends JTabbedPane implements StatsHandlerListener {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JPPFChartBuilder.class);
  /**
   * Mapping of chart types to the chart handler used to create and update them.
   */
  private Map<ChartType, ChartHandler> handlerMap = new EnumMap<>(ChartType.class);
  /**
   * The list of tab names handled by this chart builder.
   */
  private List<TabConfiguration> tabList = new ArrayList<>();
  /**
   * Mapping of tab names to their respective configuration parameters.
   */
  private Map<String, TabConfiguration> tabMap = new HashMap<>();
  /**
   * Used to store and retrieve the configuration, to and from the preferences tree.
   */
  private transient PreferencesStorage storage;
  /**
   * 
   */
  private final ChartDataCache cache = ChartDataCache.getInstance();
  /**
   * 
   */
  private final StatsHandler statsHandler;

  /**
   * Initialize this charts builder.
   */
  public JPPFChartBuilder() {
    storage = new PreferencesStorage(this);
    statsHandler = StatsHandler.getInstance();
    initHandlerMap();
    statsHandler.addStatsHandlerListener(this);
  }

  /**
   * Initialize the mapping of chart types to the chart handler used to create and update them.
   */
  private void initHandlerMap() {
    handlerMap.put(CHART_PLOTXY, new XChartPlotXYHandler(statsHandler, XYSeriesRenderStyle.Line));
    handlerMap.put(CHART_AREA, new XChartPlotXYHandler(statsHandler, XYSeriesRenderStyle.Area));
    handlerMap.put(CHART_SCATTER, new XChartPlotXYHandler(statsHandler, XYSeriesRenderStyle.Scatter));
    handlerMap.put(CHART_STEP, new XChartPlotXYHandler(statsHandler, XYSeriesRenderStyle.Step));
    handlerMap.put(CHART_STEP_AREA, new XChartPlotXYHandler(statsHandler, XYSeriesRenderStyle.StepArea));
    handlerMap.put(CHART_3DBAR, new XChartBarHandler(statsHandler, CategorySeriesRenderStyle.Bar, false));
    handlerMap.put(CHART_3DBAR_SERIES, new XChartBarHandler(statsHandler, CategorySeriesRenderStyle.Bar, false));
    handlerMap.put(CHART_STACKED_3DBAR_SERIES, new XChartBarHandler(statsHandler, CategorySeriesRenderStyle.Bar, true));
    handlerMap.put(CHART_3DPIE, new XChartPieHandler(statsHandler, PieSeriesRenderStyle.Pie));
    handlerMap.put(CHART_RING, new XChartPieHandler(statsHandler, PieSeriesRenderStyle.Donut));
    handlerMap.put(CHART_METER, new XChartDialHandler(statsHandler));
    handlerMap.put(CHART_STACKED_AREA, new XChartBarHandler(statsHandler, CategorySeriesRenderStyle.Area, true));
    //handlerMap.put(CHART_DIFFERENCE, new DifferenceChartHandler(statsHandler));
  }

  /**
   * Create a chart based on a chart configuration.
   * @param config holds the configuration parameters for the chart created, modified by this method.
   * @param preview determines whether the configuration should be added to the list of active configurations or not.
   * A negative value means it's simply appended.
   * @return the configuration with its created chart set.
   */
  public ChartConfiguration createChart(final ChartConfiguration config, final boolean preview) {
    final ChartConfiguration cfg = preview ? new ChartConfiguration(config) : config;
    cache.addFields(cfg.fields, StatsHandler.getInstance());
    final ChartHandler handler = handlerMap.get(cfg.type);
    if (handler == null) return null;
    handler.createChart(cfg);
    if (cfg.chart instanceof Object[]) {
      final JPanel panel = new JPanel();
      panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
      final Object[] charts = (Object[]) cfg.chart;
      for (int i=0; i<charts.length; i++) {
        final Object chart = charts[i];
        final JPanel chartPanel = new XChartPanel<>((Chart<?, ?>) chart);
        panel.add(chartPanel);
      }
      cfg.chartPanel = panel;
    } else {
      cfg.chartPanel = new XChartPanel<>((Chart<?, ?>) cfg.chart);
    }
    cfg.chartPanel.setBackground(Color.WHITE);
    cfg.chartPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
    return cfg;
  }

  /**
   * Remove a tab from the list of tabs.
   * @param tab the configuration information for the tab to remove.
   */
  public void addTab(final TabConfiguration tab) {
    tab.panel = GuiUtils.createBoxPanel(BoxLayout.Y_AXIS);
    if (tab.position < 0) {
      addTab(tab.name, tab.panel);
      tabList.add(tab);
    } else {
      insertTab(tab.name, null, tab.panel, null, tab.position);
      tabList.add(tab.position, tab);
    }
    tabMap.put(tab.name, tab);
    for (int i=0; i<tabList.size(); i++) tabList.get(i).position = i;
    tab.panel.updateUI();
  }

  /**
   * Remove a tab from the list of tabs.
   * @param tab the configuration information for the tab to remove.
   */
  public void removeTab(final TabConfiguration tab) {
    remove(tab.panel);
    tabList.remove(tab);
    tabMap.remove(tab.name);
    for (int i=0; i<tabList.size(); i++) tabList.get(i).position = i;
  }

  /**
   * Add a chart to a tab.
   * @param tab the tab to add a chart to.
   * @param config the chart to add.
   */
  public void addChart(final TabConfiguration tab, final ChartConfiguration config) {
    createChart(config, false);
    if (config.position < 0) {
      config.position = tab.configs.size();
      tab.configs.add(config);
      tab.panel.add(config.chartPanel);
    } else {
      tab.configs.add(config.position, config);
      tab.panel.add(config.chartPanel, config.position);
    }
  }

  /**
   * Remove a specified configuration from the list of active configurations.
   * @param tab the configuration information for the tab containing the chart.
   * @param config the configuration to remove.
   */
  public void removeChart(final TabConfiguration tab, final ChartConfiguration config) {
    cache.removeFields(config.fields);
    tab.configs.remove(config);
    final JPanel panel = tab.panel;
    panel.remove(config.chartPanel);
    panel.updateUI();
  }

  /**
   * Update the data displayed in the charts.
   * @param event holds the new stats values.
   */
  @Override
  public void dataUpdated(final StatsHandlerEvent event) {
    cache.update(statsHandler);
    for (TabConfiguration tab: tabMap.values()) {
      for (final ChartConfiguration config: tab.configs) {
        SwingUtilities.invokeLater(() -> {
          try {
            final ChartHandler handler = handlerMap.get(config.type);
            if (event.getType() == StatsHandlerEvent.Type.UPDATE) handler.updateDataset(config);
            else handler.populateDataset(config);
          } catch (final Exception e) {
            log.error(e.getMessage(), e);
          }
        });
      }
    }
  }

  /**
   * Get the tabbed pane in which each pane contains user-defined charts.
   * @return a <code>JTabbedPane</code> instance.
   */
  public JTabbedPane getTabbedPane() {
    return this;
  }

  /**
   * Get the list of active tabs in this chart builder.
   * @return a list of tabs names.
   */
  public List<TabConfiguration> getTabList() {
    return tabList;
  }

  /**
   * Get the tab chartPanel with the specified name.
   * @param tabName the name of the tab to lookup.
   * @return a <code>JPanel</code> instance.
   */
  public JPanel getTabPanel(final String tabName) {
    return tabMap.get(tabName).panel;
  }

  /**
   * Create a set of default charts if none is defined.
   */
  public void createInitialCharts() {
    storage.loadChartConfigurations();
    if (tabList.isEmpty()) storage.loadDefaultChartConfigurations();
  }

  /**
   * Get the object used to store and retrieve the configuration, to and from the preferences tree.
   * @return a PreferencesStorage object.
   */
  public PreferencesStorage getStorage() {
    return storage;
  }

  /**
   * Remove all chart configurationss and rebuild from the stored preferences.
   */
  public void reset() {
    try {
      StatsHandler.getInstance().removeStatsHandlerListener(this);
      final List<TabConfiguration> tmpTabs = new ArrayList<>(tabList);
      for (final TabConfiguration tabConfig: tmpTabs) {
        final List<ChartConfiguration> tmpCharts = new ArrayList<>(tabConfig.configs);
        for (final ChartConfiguration chartConfig: tmpCharts) removeChart(tabConfig, chartConfig);
        removeTab(tabConfig);
      }
      createInitialCharts();
    } finally {
      StatsHandler.getInstance().addStatsHandlerListener(this);
    }
  }
}
