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

import static org.jppf.ui.monitoring.charts.ChartType.CHART_PLOTXY;

import java.io.InputStream;
import java.util.*;
import java.util.prefs.*;

import org.jppf.ui.monitoring.charts.ChartType;
import org.jppf.ui.monitoring.data.*;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class provides an API to store and retrieve the chart configuration
 * preferences, using the preferences mechanism.
 * @author Laurent Cohen
 */
public class PreferencesStorage {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(PreferencesStorage.class);
  /**
   * The name of the root of the preferences subtree in which the chart configurations are saved.
   */
  private static final String CHART_CONFIG_PREFERENCES_NAME = "TabConfigurations";
  /**
   * 
   */
  private JPPFChartBuilder chartBuilder = null;

  /**
   * Initialize this preferences storage with a specified chart builder.
   * @param chartBuilder the chart builder from which to get the charts.
   */
  public PreferencesStorage(final JPPFChartBuilder chartBuilder) {
    this.chartBuilder = chartBuilder;
  }

  /**
   * Load all chart configurations from the preferences tree, and create the corresponding charts.
   */
  public void loadChartConfigurations() {
    final Preferences pref = OptionsHandler.getPreferences().node(CHART_CONFIG_PREFERENCES_NAME);
    String[] tabChildrenNames = null;
    try {
      tabChildrenNames = pref.childrenNames();
    } catch(final BackingStoreException e) {
      log.error(e.getMessage(), e);
      return;
    }
    if ((tabChildrenNames == null) || (tabChildrenNames.length <= 0)) return;
    final TabConfiguration[] tabs = new TabConfiguration[tabChildrenNames.length];
    int cnt = 0;
    for (final String s: tabChildrenNames) {
      final Preferences child = pref.node(s);
      final TabConfiguration tab = new TabConfiguration();
      tab.name = child.get("name", "Tab" + cnt);
      tab.position = child.getInt("position", -1);
      final ChartConfiguration[] configs = loadTabCharts(child);
      tab.configs.addAll(Arrays.asList(configs));
      tabs[cnt] = tab;
      cnt++;
    }
    Arrays.sort(tabs, new Comparator<TabConfiguration>() {
      @Override
      public int compare(final TabConfiguration o1, final TabConfiguration o2) {
        if (o1 == o2) return 0;
        if (o1 == null) return -1;
        if (o2 == null) return 1;
        return Integer.valueOf(o1.position).compareTo(o2.position);
      }
    });
    for (final TabConfiguration tab: tabs) {
      chartBuilder.addTab(tab);
      for (final ChartConfiguration config : tab.configs) {
        chartBuilder.createChart(config, false);
        tab.panel.add(config.chartPanel);
      }
    }
  }

  /**
   * Load all chart configurations from the preferences tree, and create the corresponding charts.
   */
  public void loadDefaultChartConfigurations() {
    try (final InputStream is =  FileUtils.getFileInputStream("ui-default-charts-settings.xml")) {
      //final Preferences pref = OptionsHandler.getPreferences().node(CHART_CONFIG_PREFERENCES_NAME);
      Preferences.importPreferences(is);
      loadChartConfigurations();
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Load the chart configurations for a tab from a specified tab preferences node.
   * @param tabNode the tab preferences node that contains the chart configuration nodes.
   * @return an array of <code>ChartConfiguration</code> instances.
   */
  public ChartConfiguration[] loadTabCharts(final Preferences tabNode) {
    ChartConfiguration[] result = new ChartConfiguration[0];
    String[] tabChildrenNames = null;
    try {
      tabChildrenNames = tabNode.childrenNames();
    } catch(final BackingStoreException e) {
      log.error(e.getMessage(), e);
      return result;
    }
    if ((tabChildrenNames == null) || (tabChildrenNames.length <= 0)) return result;
    result = new ChartConfiguration[tabChildrenNames.length];
    int cnt = 0;
    for (final String s: tabChildrenNames) {
      final Preferences child = tabNode.node(s);
      final ChartConfiguration config = loadChartConfiguration(child);
      result[cnt] = config;
      cnt++;
    }
    Arrays.sort(result, new Comparator<ChartConfiguration>() {
      @Override
      public int compare(final ChartConfiguration o1, final ChartConfiguration o2) {
        if (o1 == o2) return 0;
        if (o1 == null) return -1;
        if (o2 == null) return 1;
        return Integer.valueOf(o1.position).compareTo(o2.position);
      }
    });
    return result;
  }

  /**
   * Load a chart configuration from a preferences node.
   * @param child the preferences node to load the configuration from.
   * @return a <code>ChartConfiguration</code> instance.
   */
  public ChartConfiguration loadChartConfiguration(final Preferences child) {
    final ChartConfiguration config = new ChartConfiguration();
    config.name = child.get("name", "");
    config.precision = child.getInt("precision", 0);
    config.unit = child.get("unit", null);
    final String fields = child.get("fields", "");
    final String[] sFields = RegexUtils.PIPE_PATTERN.split(fields);
    final List<Fields> list = new ArrayList<>();
    for (final String sField : sFields) {
      final Fields f = StatsConstants.getFieldForName(sField);
      if (f != null) list.add(f);
    }
    config.fields = list.toArray(new Fields[list.size()]);
    final String type = child.get("type", CHART_PLOTXY.name());
    try {
      config.type = ChartType.valueOf(type);
    } catch(final IllegalArgumentException e) {
      log.error(e.getMessage(), e);
    }
    if (config.type == null) config.type = CHART_PLOTXY;
    return config;
  }

  /**
   * Save all tabs and charts configurations in the user preferences.
   */
  public void saveAll() {
    removeAllSaved();
    int cnt = 0;
    for (TabConfiguration tab: chartBuilder.getTabList()) {
      tab.position = cnt++;
      saveTabConfiguration(tab);
      int chartCnt = 0;
      for (ChartConfiguration config: tab.configs) {
        config.position = chartCnt++;
        saveChartConfiguration(tab, config);
      }
    }
    try {
      final Preferences pref = OptionsHandler.getPreferences().node(CHART_CONFIG_PREFERENCES_NAME);
      pref.flush();
    } catch(final BackingStoreException e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Save a specified tab configuration in the preferences tree.
   * @param tab the tab to save.
   */
  public void saveTabConfiguration(final TabConfiguration tab) {
    final String tabNodeName = "TabConfiguration" + tab.position;
    final Preferences pref = OptionsHandler.getPreferences().node(CHART_CONFIG_PREFERENCES_NAME).node(tabNodeName);
    pref.put("name", tab.name);
    pref.putInt("position", tab.position);
  }

  /**
   * Save a specified chart configuration in the preferences tree.
   * @param tab the tab into which to save the configuration.
   * @param config the configuration to save.
   */
  public void saveChartConfiguration(final TabConfiguration tab, final ChartConfiguration config) {
    final String tabNodeName = "TabConfiguration" + tab.position;
    final String nodeName = "ChartConfiguration" + config.position;
    final Preferences pref = OptionsHandler.getPreferences().node(CHART_CONFIG_PREFERENCES_NAME).node(tabNodeName + '/' + nodeName);
    pref.put("name", config.name);
    pref.putInt("precision", config.precision);
    if (config.unit != null ) pref.put("unit", config.unit);
    pref.put("type", config.type.name());
    final StringBuilder sb = new StringBuilder();
    for (int i=0; i<config.fields.length; i++) {
      if (i > 0) sb.append('|');
      sb.append(config.fields[i].getName());
    }
    pref.put("fields", sb.toString());
    pref.putInt("position", config.position);
  }

  /**
   * Remove all tabs and charts configurations from the user preferences.
   */
  public void removeAllSaved() {
    try {
      final Preferences pref = OptionsHandler.getPreferences().node(CHART_CONFIG_PREFERENCES_NAME);
      final String[] names = pref.childrenNames();
      for (final String name: names) pref.node(name).removeNode();
    } catch(final BackingStoreException e) {
      log.error(e.getMessage(), e);
    }
  }
}
