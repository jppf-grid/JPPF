/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation;
 * either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 */
package org.jppf.ui.monitoring.charts.config;

import java.util.*;
import java.util.prefs.*;
import javax.swing.*;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartPanel;
import org.jppf.ui.monitoring.GuiUtils;
import org.jppf.ui.monitoring.charts.*;
import org.jppf.ui.monitoring.data.StatsHandler;
import org.jppf.ui.monitoring.event.*;
import static org.jppf.ui.monitoring.charts.ChartType.*;
import static org.jppf.ui.monitoring.data.StatsConstants.*;

/**
 * This class is used as a factory to create different charts, as well as for propagating the data updates
 * to all defined charts.
 * @author Laurent Cohen
 */
public class JPPFChartBuilder implements StatsHandlerListener
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(JPPFChartBuilder.class);
	/**
	 * The root of the preferences subtree in which the chart configurations are saved.
	 */
	private static Preferences CHART_CONFIG_PREFERENCES = Preferences.userRoot().node("jppf/TabConfigurations");
	/**
	 * The stats formatter that provides the data.
	 */
	private StatsHandler statsHandler = null;
	/**
	 * Mapping of chart types to the chart handler used to create and update them.
	 */
	private Map<ChartType, ChartHandler> handlerMap = new HashMap<ChartType, ChartHandler>();
	/**
	 * The tabbed pane in which each pane contains user-defined charts.
	 */
	private JTabbedPane tabbedPane = new JTabbedPane();
	/**
	 * The list of tab names handled by this chart builder.
	 */
	private List<TabConfiguration> tabList = new ArrayList<TabConfiguration>();
	/**
	 * Mapping of tab names to their respective configuration parameters.
	 */
	private Map<String, TabConfiguration> tabMap = new HashMap<String, TabConfiguration>();

	/**
	 * Initialize this charts builder with a specified stats formatter.
	 * @param statsHandler the stats formatter that provides the data.
	 */
	public JPPFChartBuilder(StatsHandler statsHandler)
	{
		this.statsHandler = statsHandler;
		initHandlerMap();
	}
	
	/**
	 * Initialize the mapping of chart types to the chart handler used to create and update them.
	 */
	private void initHandlerMap()
	{
		handlerMap.put(CHART_PLOTXY, new PlotXYChartHandler(statsHandler));
		handlerMap.put(CHART_3DBAR, new Bar3DChartHandler(statsHandler));
		handlerMap.put(CHART_AREA, new AreaChartHandler(statsHandler));
		handlerMap.put(CHART_3DPIE, new Pie3DChartHandler(statsHandler));
		handlerMap.put(CHART_RING, new RingChartHandler(statsHandler));
		handlerMap.put(CHART_DIFFERENCE, new DifferenceChartHandler(statsHandler));
		//handlerMap.put(CHART_THERMOMETER, new ThermometerChartHandler(statsHandler));
	}

	/**
	 * Create a chart based on a chart configuration.
	 * @param config holds the configuration parameters for the chart created, modified by this method.
	 * @param preview determines whether the configuration should be added to the list of active configurations or not.
	 * A negative value means it's simply appended.
	 * @return the configuration with its created chart set.
	 */
	public ChartConfiguration createChart(ChartConfiguration config, boolean preview)
	{
		ChartHandler handler = handlerMap.get(config.type);
		if (handler == null) return null;
		handler.createChart(config);
		config.chartPanel = new ChartPanel(config.chart);
		return config;
	}

	/**
	 * Remove a tab from the list of tabs.
	 * @param tab the configuration information for the tab to remove.
	 */
	public void removeTab(TabConfiguration tab)
	{
		tabbedPane.remove(tab.panel);
		tabList.remove(tab);
		tabMap.remove(tab.name);
		for (int i=0; i<tabList.size(); i++) tabList.get(i).position = i;
	}
	
	/**
	 * Remove a tab from the list of tabs.
	 * @param tab the configuration information for the tab to remove.
	 */
	public void addTab(TabConfiguration tab)
	{
		tab.panel = GuiUtils.createBoxPanel(BoxLayout.Y_AXIS);
		if (tab.position < 0)
		{
			tabbedPane.addTab(tab.name, tab.panel);
			tabList.add(tab);
		}
		else
		{
			tabbedPane.insertTab(tab.name, null, tab.panel, null, tab.position);
			tabList.add(tab.position, tab);
		}
		tabMap.put(tab.name, tab);
		for (int i=0; i<tabList.size(); i++) tabList.get(i).position = i;
		tab.panel.updateUI();
	}
	
	/**
	 * Remove a specified configuration from the list of active configurations.
	 * @param tab the configuration information for the tab containing the chart.
	 * @param config the configuration to remove.
	 */
	public void removeChart(TabConfiguration tab, ChartConfiguration config)
	{
		tab.configs.remove(config);
		JPanel panel = tab.panel;
		panel.remove(config.chartPanel);
		panel.updateUI();
	}
	
	/**
	 * Update the data displayed in the charts.
	 * @param event holds the new stats values.
	 */
	public void dataUpdated(StatsHandlerEvent event)
	{
		for (TabConfiguration tab: tabMap.values())
		{
			for (final ChartConfiguration config: tab.configs)
			{
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						handlerMap.get(config.type).updateDataset(config);
					}
				});
			}
		}
	}

	/**
	 * Get the tabbed pane in which each pane contains user-defined charts.
	 * @return a <code>JTabbedPane</code> instance.
	 */
	public JTabbedPane getTabbedPane()
	{
		return tabbedPane;
	}

	/**
	 * Get the list of active tabs in this chart builder.
	 * @return a list of tabs names.
	 */
	public List<TabConfiguration> getTabList()
	{
		return tabList;
	}

	/**
	 * Get the tab chartPanel with the specified name.
	 * @param tabName the name of the tab to lookup.
	 * @return a <code>JPanel</code> instance.
	 */
	public JPanel getTabPanel(String tabName)
	{
		return tabMap.get(tabName).panel;
	}

	/**
	 * Create a set of default charts if none is defined.
	 */
	public void createInitialCharts()
	{
		loadChartConfigurations();
		if (tabList.isEmpty()) createDefaultCharts();
	}

	/**
	 * Create a set of default charts if none is defined.
	 */
	public void createDefaultCharts()
	{
		TabConfiguration network = new TabConfiguration("Network", 0);
		addTab(network);
		TabConfiguration plot = new TabConfiguration("Plot Charts", 1);
		addTab(plot);
		TabConfiguration bar = new TabConfiguration("Bar Charts", 2);
		addTab(bar);

		String[] fields = new String[] { AVG_EXECUTION_TIME, LATEST_EXECUTION_TIME, AVG_NODE_EXECUTION_TIME, LATEST_NODE_EXECUTION_TIME };
		addChart(network, new ChartConfiguration("Execution time", CHART_PLOTXY, "ms", 2, fields));
		fields = new String[] { AVG_EXECUTION_TIME, LATEST_EXECUTION_TIME, MAX_EXECUTION_TIME };
		addChart(bar, new ChartConfiguration("Execution time (bar chart)", CHART_3DBAR, "ms", 2, fields));
		addChart(plot, new ChartConfiguration("Execution time", CHART_PLOTXY, "ms", 2, fields));
		fields = new String[] { AVG_QUEUE_TIME, LATEST_QUEUE_TIME, MAX_QUEUE_TIME };
		addChart(bar, new ChartConfiguration("Queue time (bar chart)", CHART_3DBAR, "ms", 2, fields));
		addChart(plot, new ChartConfiguration("Queue time", CHART_PLOTXY, "ms", 2, fields));
		fields = new String[] { QUEUE_SIZE, MAX_QUEUE_SIZE };
		addChart(bar, new ChartConfiguration("Queue size (bar chart)", CHART_3DBAR, null, 0, fields));
		addChart(plot, new ChartConfiguration("Queue size", CHART_PLOTXY, null, 0, fields));
	}
	
	/**
	 * Add a chart to a tab.
	 * @param tab the tab to add a chart to.
	 * @param config the chart to add.
	 */
	public void addChart(TabConfiguration tab, ChartConfiguration config)
	{
		createChart(config, false);
		if (config.position < 0)
		{
			config.position = tab.configs.size();
			tab.configs.add(config);
			tab.panel.add(config.chartPanel);
		}
		else
		{
			tab.configs.add(config.position, config);
			tab.panel.add(config.chartPanel, config.position);
		}
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
				return new Integer(o1.position).compareTo(o2.position);
			}
		});

		for (TabConfiguration tab: tabs)
		{
			addTab(tab);
			for (ChartConfiguration config : tab.configs)
			{
				createChart(config, false);
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
				return new Integer(o1.position).compareTo(o2.position);
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
		config.fields = fields.split("\\|");
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
	 * Save all tabs and charts configurations in the user preferences.
	 */
	public void saveAll()
	{
		removeAllSaved();
		//CHART_CONFIG_PREFERENCES
		int cnt = 0;
		for (TabConfiguration tab: tabList)
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
		try
		{
			pref.flush();
		}
		catch(BackingStoreException e)
		{
			log.error(e.getMessage(), e);
		}
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
			sb.append(config.fields[i]);
		}
		pref.put("fields", sb.toString());
		pref.putInt("position", config.position);
		try
		{
			pref.flush();
		}
		catch(BackingStoreException e)
		{
			log.error(e.getMessage(), e);
		}
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
			CHART_CONFIG_PREFERENCES.flush();
		}
		catch(BackingStoreException e)
		{
			log.error(e.getMessage(), e);
		}
	}
}
