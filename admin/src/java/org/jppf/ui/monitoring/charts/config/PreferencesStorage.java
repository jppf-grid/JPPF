/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2009 JPPF Team.
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
import java.util.*;
import java.util.prefs.*;
import org.apache.commons.logging.*;
import org.jppf.ui.monitoring.charts.ChartType;
import org.jppf.ui.monitoring.data.Fields;

/**
 * This class provides an API to store and retrieve the chart configuration
 * preferences, using the preferences mechanism.
 * @author Laurent Cohen
 */
public class PreferencesStorage
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(PreferencesStorage.class);
	/**
	 * The root of the preferences subtree in which the chart configurations are saved.
	 */
	private static Preferences CHART_CONFIG_PREFERENCES = Preferences.userRoot().node("jppf/TabConfigurations");
	/**
	 * 
	 */
	private JPPFChartBuilder chartBuilder = null;

	/**
	 * Initialize this preferences storage with a specified chart builder.
	 * @param chartBuilder the chart builder from which to get the charts.
	 */
	public PreferencesStorage(JPPFChartBuilder chartBuilder)
	{
		this.chartBuilder = chartBuilder;
	}

	/**
	 * Load all chart configurations from the preferences tree, and create the corresponding charts.
	 */
	public void loadChartConfigurations()
	{
		Preferences pref = CHART_CONFIG_PREFERENCES;
		String[] tabChildrenNames = null;
		try
		{
			tabChildrenNames = pref.childrenNames();
		}
		catch(BackingStoreException e)
		{
			log.error(e.getMessage(), e);
			return;
		}
		if ((tabChildrenNames == null) || (tabChildrenNames.length <= 0)) return;
		TabConfiguration[] tabs = new TabConfiguration[tabChildrenNames.length];
		int cnt = 0;
		for (String s: tabChildrenNames)
		{
			Preferences child = pref.node(s);
			TabConfiguration tab = new TabConfiguration();
			tab.name = child.get("name", "Tab"+cnt);
			tab.position = child.getInt("position", -1);
			ChartConfiguration[] configs = loadTabCharts(child);
			for (ChartConfiguration config: configs) tab.configs.add(config);
			tabs[cnt] = tab;
			cnt++;
		}
		Arrays.sort(tabs, new Comparator<TabConfiguration>()
		{
			public int compare(TabConfiguration o1, TabConfiguration o2)
			{
				if (o1 == o2) return 0;
				if (o1 == null) return -1;
				if (o2 == null) return 1;
				return Integer.valueOf(o1.position).compareTo(o2.position);
			}
		});

		for (TabConfiguration tab: tabs)
		{
			chartBuilder.addTab(tab);
			for (ChartConfiguration config : tab.configs)
			{
				chartBuilder.createChart(config, false);
				tab.panel.add(config.chartPanel);
			}
		}
	}
	
	/**
	 * Load the chart configurations for a tab from a specified tab preferences node.
	 * @param tabNode the tab preferences node that contains the chart configuration nodes.
	 * @return an array of <code>ChartConfiguration</code> instances.
	 */
	public ChartConfiguration[] loadTabCharts(Preferences tabNode)
	{
		ChartConfiguration[] result = new ChartConfiguration[0];
		String[] tabChildrenNames = null;
		try
		{
			tabChildrenNames = tabNode.childrenNames();
		}
		catch(BackingStoreException e)
		{
			log.error(e.getMessage(), e);
			return result;
		}
		if ((tabChildrenNames == null) || (tabChildrenNames.length <= 0)) return result;
		result = new ChartConfiguration[tabChildrenNames.length];
		int cnt = 0;
		for (String s: tabChildrenNames)
		{
			Preferences child = tabNode.node(s);
			ChartConfiguration config = loadChartConfiguration(child);
			result[cnt] = config;
			cnt++;
		}
		Arrays.sort(result, new Comparator<ChartConfiguration>()
		{
			public int compare(ChartConfiguration o1, ChartConfiguration o2)
			{
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
	 * @param child the preferences node to laod the configuration from.
	 * @return a <code>ChartConfiguration</code> instance.
	 */
	public ChartConfiguration loadChartConfiguration(Preferences child)
	{
		ChartConfiguration config = new ChartConfiguration();
		config.name = child.get("name", "");
		config.precision = child.getInt("precision", 0);
		config.unit = child.get("unit", null);
		String fields = child.get("fields", "");
		String[] sFields = fields.split("\\|");
		List<Fields> list = new ArrayList<Fields>();
		for (int i=0; i<sFields.length; i++)
		{
			Fields f = lookupEnum(sFields[i]);
			if (f != null) list.add(f);
		}
		config.fields = list.toArray(new Fields[0]);
		String type = child.get("type", CHART_PLOTXY.name());
		try
		{
			config.type = ChartType.valueOf(type);
		}
		catch(IllegalArgumentException e)
		{
			log.error(e.getMessage(), e);
		}
		if (config.type == null) config.type = CHART_PLOTXY;
		return config;
	}

	/**
	 * Get a <code>Fields</code> instance from a fields name.
	 * @param name the name of the field to find.
	 * @return a <code>Fields</code>, or null if the field could not be found.
	 */
	private Fields lookupEnum(String name)
	{
		Fields field = null;
		try
		{
			field = Fields.valueOf(name);
		}
		catch (IllegalArgumentException e)
		{
			for (Fields f: Fields.values())
			{
				if (name.equals(f.toString()))
				{
					field = f;
					break;
				}
			}
		}
		return field;
	}

	/**
	 * Save all tabs and charts configurations in the user preferences.
	 */
	public void saveAll()
	{
		removeAllSaved();
		int cnt = 0;
		for (TabConfiguration tab: chartBuilder.getTabList())
		{
			tab.position = cnt++;
			saveTabConfiguration(tab);
			int chartCnt = 0;
			for (ChartConfiguration config: tab.configs)
			{
				config.position = chartCnt++;
				saveChartConfiguration(tab, config);
			}
		}
		try
		{
			CHART_CONFIG_PREFERENCES.flush();
		}
		catch(BackingStoreException e)
		{
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Save a specified tab configuration in the preferences tree.
	 * @param tab the tab to save.
	 */
	public void saveTabConfiguration(TabConfiguration tab)
	{
		String tabNodeName = "TabConfiguration"+tab.position;
		Preferences pref = CHART_CONFIG_PREFERENCES.node(tabNodeName);
		pref.put("name", tab.name);
		pref.putInt("position", tab.position);
	}
	
	/**
	 * Save a specified chart configuration in the preferences tree.
	 * @param tab the tab into which to save the configuration.
	 * @param config the configuration to save.
	 */
	public void saveChartConfiguration(TabConfiguration tab, ChartConfiguration config)
	{
		String tabNodeName = "TabConfiguration"+tab.position;
		String nodeName = "ChartConfiguration"+config.position;
		Preferences pref = CHART_CONFIG_PREFERENCES.node(tabNodeName+"/"+nodeName);
		pref.put("name", config.name);
		pref.putInt("precision", config.precision);
		if (config.unit != null ) pref.put("unit", config.unit);
		pref.put("type", config.type.name());
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<config.fields.length; i++)
		{
			if (i > 0) sb.append("|");
			sb.append(config.fields[i].name());
		}
		pref.put("fields", sb.toString());
		pref.putInt("position", config.position);
	}
	
	/**
	 * Remove all tabs and charts configurations from the user preferences.
	 */
	public void removeAllSaved()
	{
		try
		{
			String[] names = CHART_CONFIG_PREFERENCES.childrenNames();
			for (String name: names) CHART_CONFIG_PREFERENCES.node(name).removeNode();
		}
		catch(BackingStoreException e)
		{
			log.error(e.getMessage(), e);
		}
	}
}
