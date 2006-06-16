/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jppf.ui.options.factory;

import java.awt.GridBagConstraints;
import java.util.*;
import javax.swing.*;
import org.jppf.ui.monitoring.charts.ChartType;
import org.jppf.ui.monitoring.charts.config.*;
import org.jppf.ui.monitoring.data.*;
import org.jppf.ui.options.*;
import org.jppf.utils.CollectionUtils;

/**
 * Actions and listeners for the charts configuration panel.
 * @author Laurent Cohen
 */
public class ChartConfigActions extends AbstractActionsHolder
{
	/**
	 * Initialize this action holder by populating the adequate fields.
	 * @param page an <code>OptionsPage</code> used for bbotstrapping.
	 */
	public void initialize(OptionsPage page)
	{
		page.setEventsEnabled(false);
		option = (Option) page.findFirstWithName("ChartType");
		List values = CollectionUtils.list(ChartType.values());
		((ComboBoxOption) option).setItems(values);
		option = (Option) page.findFirstWithName("FieldsList");
		values = CollectionUtils.list(StatsConstants.ALL_FIELDS);
		((ListOption) option).setItems(values);
		populateTabsList(null);
		populateChartsList(null, null);
		populateTabsCombo(null);
		page.setEventsEnabled(true);
	}

	/**
	 * Initialize the mapping of an option name to the method to invoke when the option's value changes.
	 * @see org.jppf.ui.options.factory.AbstractActionsHolder#initializeMethodMap()
	 */
	protected void initializeMethodMap()
	{
		addMapping("TabsList", "tabSelectionChanged");
		addMapping("ChartsList", "chartSelectionChanged");
		addMapping("ChartType", "fieldsOrChartTypeSelectionChanged");
		addMapping("FieldsList", "fieldsOrChartTypeSelectionChanged");
		addMapping("TabNew", "newTabPressed");
		addMapping("TabRemove", "removeTabPressed");
		addMapping("TabUp", "tabMoved");
		addMapping("TabDown", "tabMoved");
		addMapping("ChartRemove", "removeChartPressed");
		addMapping("ChartUp", "chartMoved");
		addMapping("ChartDown", "chartMoved");
		addMapping("SaveNewChart", "newChartPressed");
		addMapping("UpdateChart", "updateChartPressed");
	}

	/**
	 * Invoked when the tab selection has changed.
	 */
	public void tabSelectionChanged()
	{
		option.getRoot().setEventsEnabled(false);
		List values = (List) option.getValue();
		if ((values == null) || (values.isEmpty())) return;
		populateChartsList(getTabConfig(), null);
		resetAllEnabledStates();
		option.getRoot().setEventsEnabled(true);
	}

	/**
	 * Called when the chart selection has changed.
	 */
	public void chartSelectionChanged()
	{
		option.getRoot().setEventsEnabled(false);
		List values = (List) option.getValue();
		if ((values == null) || (values.isEmpty())) return;
		ChartConfiguration config = (ChartConfiguration) values.get(0);
		populateFields(getTabConfig(), config);
		changePreview(config);
		resetAllEnabledStates();
		option.getRoot().setEventsEnabled(true);
	}

	/**
	 * Invoked when the fields list selection has changed.
	 */
	public void fieldsOrChartTypeSelectionChanged()
	{
		changePreview(getPopulatedConfiguration());
	}

	/**
	 * Add a new tab to the list of tabs.
	 */
	public void removeTabPressed()
	{
		option.getRoot().setEventsEnabled(false);
		TabConfiguration tab = getTabConfig();
		JPPFChartBuilder.getInstance().removeTab(tab);
		populateTabsList(null);
		populateTabsCombo(null);
		populateChartsList(null, null);
		option.getRoot().setEventsEnabled(true);
	}

	/**
	 * When the remove tab button has been pressed.
	 */
	public void newTabPressed()
	{
		option.getRoot().setEventsEnabled(false);
		String s = JOptionPane.showInputDialog(option.getUIComponent(), "Enter a name for the new tab", "new tab");
		if (s != null)
		{
			TabConfiguration tab = new TabConfiguration(s, -1);
			JPPFChartBuilder.getInstance().addTab(tab);
			populateTabsList(tab);
			populateTabsCombo(tab);
			populateChartsList(tab, null);
		}
		option.getRoot().setEventsEnabled(true);
	}

	/**
	 * Called when a tab is moved up or down in the tabs list.
	 */
	public void tabMoved()
	{
		option.getRoot().setEventsEnabled(false);
		TabConfiguration tab = getTabConfig();
		int increment = "TabUp".equals(option.getName()) ? -1 : 1;
		JPPFChartBuilder builder = JPPFChartBuilder.getInstance();
		builder.removeTab(tab);
		tab.position += increment;
		builder.addTab(tab);
		populateTabsList(tab);
		populateTabsCombo(tab);
		option.getRoot().setEventsEnabled(true);
	}

	/**
	 * When the remove chart button has been pressed.
	 */
	public void removeChartPressed()
	{
		option.findElement("/").setEventsEnabled(false);
		TabConfiguration tabConfig = getTabConfig();
		ChartConfiguration chartConfig = getChartConfig();
		JPPFChartBuilder.getInstance().removeChart(tabConfig, chartConfig);
		populateChartsList(tabConfig, chartConfig);
		option.getRoot().setEventsEnabled(true);
	}

	/**
	 * Called when a tab is moved up or down in the tabs list.
	 */
	public void chartMoved()
	{
		option.getRoot().setEventsEnabled(false);
		ChartConfiguration chartConfig = getChartConfig();
		TabConfiguration tabConfig = getTabConfig();
		int increment = "ChartUp".equals(option.getName()) ? -1 : 1;
		JPPFChartBuilder builder = JPPFChartBuilder.getInstance();
		builder.removeChart(tabConfig, chartConfig);
		chartConfig.position += increment;
		builder.addChart(tabConfig, chartConfig);
		populateChartsList(tabConfig, chartConfig);
		option.getRoot().setEventsEnabled(true);
	}

	/**
	 * Action called when the &quot;Saves as new&quot; button is pressed.
	 */
	public void newChartPressed()
	{
		option.getRoot().setEventsEnabled(false);
		ChartConfiguration config = getPopulatedConfiguration();
		TabConfiguration tabConfig = getTabConfig();
		JPPFChartBuilder.getInstance().addChart(tabConfig, config);
		populateTabsList(tabConfig);
		populateChartsList(tabConfig, config);
		option.getRoot().setEventsEnabled(true);
	}

	/**
	 * Action called when the &quot;Update&quot; button is pressed.
	 */
	public void updateChartPressed()
	{
		TabConfiguration newTab = (TabConfiguration) ((Option) option.findElement("/TabsList")).getValue();
		TabConfiguration currentTab = getTabConfig();
		option.getRoot().setEventsEnabled(false);
		ChartConfiguration oldConfig = getChartConfig();
		ChartConfiguration newConfig = getPopulatedConfiguration();

		if (newTab == currentTab) newConfig.position = currentTab.configs.indexOf(oldConfig);
		JPPFChartBuilder builder = JPPFChartBuilder.getInstance();
		builder.removeChart(currentTab, oldConfig);
		builder.addChart(newTab, newConfig);
		populateTabsList(newTab);
		populateChartsList(newTab, newConfig);
		option.getRoot().setEventsEnabled(true);
	}
	
	/**
	 * Repopulate the list of charts.
	 * @param tabConfig the tab the charts belong to.
	 * @param chartConfig the currently selected chart.
	 */
	public void populateChartsList(TabConfiguration tabConfig, ChartConfiguration chartConfig)
	{
		ListOption listOption = (ListOption) option.findFirstWithName("/ChartsList");
		if (tabConfig == null)
		{
			listOption.setItems(new ArrayList());
			listOption.setValue(null);
		}
		else
		{
			listOption.setItems(tabConfig.configs);
			List<Object> values = new ArrayList<Object>();
			if (chartConfig != null) values.add(chartConfig);
			listOption.setValue(values);
		}
	}
	
	/**
	 * Refresh the content of the list of tabs.
	 * @param tab if not null, determines which tab should be selected.
	 */
	public void populateTabsList(TabConfiguration tab)
	{
		ListOption listOption = (ListOption) option.findFirstWithName("/TabsList");
		listOption.setItems(JPPFChartBuilder.getInstance().getTabList());
		if (tab != null)
		{
			List<Object> value = new ArrayList<Object>();
			value.add(tab);
			listOption.setValue(value);
		}
	}

	/**
	 * Refresh the combo box that holds the list of available tabs.
	 * @param tab the tab to set as selected, may be null.
	 */
	public void populateTabsCombo(TabConfiguration tab)
	{
		ComboBoxOption comboOption = (ComboBoxOption) option.findFirstWithName("/TabName");
		comboOption.setItems(JPPFChartBuilder.getInstance().getTabList());
		if (tab != null) comboOption.setValue(tab);
	}

	/**
	 * Set the values in the configuration fields.
	 * @param tab the tab to which the field belongs.
	 * @param config the chart configurationfrom which the values are taken.
	 */
	public void populateFields(TabConfiguration tab, ChartConfiguration config)
	{
		AbstractOption o = (AbstractOption) option.findFirstWithName("/ChartName");
		o.setValue(config.name);
		o = (AbstractOption) option.findFirstWithName("/TabName");
		o.setValue(tab);
		o = (AbstractOption) option.findFirstWithName("/Unit");
		o.setValue(config.unit == null ? "" : config.unit);
		o = (AbstractOption) option.findFirstWithName("/Precision");
		o.setValue(config.precision);
		o = (AbstractOption) option.findFirstWithName("/ChartType");
		o.setValue(config.type);
		o = (AbstractOption) option.findFirstWithName("/FieldsList");
		o.setValue(CollectionUtils.list(config.fields));
	}

	/**
	 * Create a chart configuration from the configuration fields.
	 * @return a <code>ChartConfiguration</code> instance.
	 */
	public ChartConfiguration getPopulatedConfiguration()
	{
		ChartConfiguration config = new ChartConfiguration();
		config.name = (String) ((Option) option.findFirstWithName("/ChartName")).getValue();
		config.unit = (String) ((Option) option.findFirstWithName("/Unit")).getValue();
		if ("".equals(config.unit)) config.unit = null;
		config.precision = ((Number) ((Option) option.findFirstWithName("/Precision")).getValue()).intValue();
		config.type = (ChartType) ((Option) option.findFirstWithName("/ChartType")).getValue();

		List list = (List) ((Option) option.findFirstWithName("/FieldsList")).getValue();
		Fields[] fields = new Fields[list.size()];
		for (int i=0; i<fields.length; i++) fields[i] = (Fields) list.get(i);
		config.fields = fields;
		return config;
	}

	/**
	 * Get the currently selected tab configuration.
	 * @return a <code>TabConfiguration</code> instance.
	 */
	public TabConfiguration getTabConfig()
	{
		List values = getListValues("TabsList");
		return values.isEmpty() ? null : (TabConfiguration) values.get(0);
	}

	/**
	 * Get the currently selected chart configuration.
	 * @return a <code>ChartConfiguration</code> instance.
	 */
	public ChartConfiguration getChartConfig()
	{
		List values = getListValues("ChartsList");
		return values.isEmpty() ? null : (ChartConfiguration) values.get(0);
	}
	
	/**
	 * Get the values selected in a list option.
	 * @param optionName the name of the option to get the values from.
	 * @return a list of object values.
	 */
	public List getListValues(String optionName)
	{
		if (option == null) return new ArrayList<Object>();
		Option listOption = (Option) option.findFirstWithName("/" + optionName);
		List values = (List) listOption.getValue();
		return values == null ? new ArrayList<Object>() : values;
	}

	/**
	 * Get the items in a list option.
	 * @param optionName the name of the option to get the items from.
	 * @return a list of object values.
	 */
	public List getListItems(String optionName)
	{
		if (option == null) return new ArrayList<Object>();
		Option listOption = (Option) option.findFirstWithName("/" + optionName);
		List item = ((ListOption) listOption).getItems();
		return item == null ? new ArrayList<Object>() : item;
	}

	/**
	 * Update the preview chartPanel according to a specified chart configuration.
	 * @param config the configuration of the chart to dispaly in the preview chartPanel.
	 */
	public void changePreview(final ChartConfiguration config)
	{
		if (config == null) return;
		if (option == null) return;
		final Option option = this.option;
		JPPFChartBuilder.getInstance().createChart(config, true);
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				JComponent comp = option.findFirstWithName("/ChartPreview").getUIComponent();
				comp.removeAll();
				GridBagConstraints c = new GridBagConstraints();
				c.weightx = 1.0;
				c.weighty = 1.0;
				c.gridheight = GridBagConstraints.REMAINDER;
				c.gridwidth = GridBagConstraints.REMAINDER;
				c.fill = GridBagConstraints.BOTH;
				comp.add(config.chartPanel, c);
				config.chart.setBackgroundPaint(comp.getBackground());
				comp.updateUI();
			}
		});
	}

	/**
	 * Reset the enabled status of all buttons in the panel.
	 */
	protected void resetAllEnabledStates()
	{
		resetListEnabledStates("TabsList", "TabRemove", "TabUp", "TabDown");
		resetListEnabledStates("ChartsList", "ChartRemove", "ChartUp", "ChartDown");
	}

	/**
	 * Reset the enabled status of all buttons associated with a list.
	 * @param listName name of the list option.
	 * @param btnName the names of the associated buttons.
	 */
	protected void resetListEnabledStates(String listName, String...btnName)
	{
		List list = getListValues(listName);
		Object o = list.isEmpty() ? null : list.get(0);
		List items = getListItems(listName);
		int idx = (o == null) ? -1 : items.indexOf(o);
		option.findFirstWithName("/" + btnName[0]).setEnabled(o != null);
		option.findFirstWithName("/" + btnName[1]).setEnabled(idx > 0);
		option.findFirstWithName("/" + btnName[2]).setEnabled((idx >= 0) && (idx < items.size() - 1));
	}
}
