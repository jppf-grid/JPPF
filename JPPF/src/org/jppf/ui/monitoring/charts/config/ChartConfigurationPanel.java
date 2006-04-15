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
package org.jppf.ui.monitoring.charts.config;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.event.*;
import org.jppf.ui.monitoring.GuiUtils;

/**
 * The chartPanel used to create and manage chart configurations.
 * @author Laurent Cohen
 */
public class ChartConfigurationPanel extends JPanel
{
	/**
	 * Holds the active chart configurations.
	 */
	private JPPFChartBuilder builder = null;
	/**
	 * The component holding the list of existing chart tabs.
	 */
	private JList tabsJList = null;
	/**
	 * The component holding the list of existing charts in the currently select tab.
	 */
	private JList chartsJList = null;
	/**
	 * The currently selected tab configuration.
	 */
	private TabConfiguration currentTab = null;
	/**
	 * The currently selected configuration.
	 */
	private ChartConfiguration currentConfig = null;
	/**
	 * The panel that contains the chart configuration parameter fields.
	 */
	private FieldsPanel fieldsPanel = null;
	/**
	 * The actions for the buttons associated with the tabs list.
	 */
	private ButtonAction[] tabActions = null;
	/**
	 * The actions for the buttons associated with the charts list.
	 */
	private ButtonAction[] chartActions = null;
	
	/**
	 * Initialize this chart configuration chartPanel with the specified chart builder.
	 * @param builder the builder used to create and manage charts.
	 */
	public ChartConfigurationPanel(JPPFChartBuilder builder)
	{
		this.builder = builder;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(createListPanel());
		add(Box.createVerticalStrut(10));
		fieldsPanel = new FieldsPanel(this);
		add(fieldsPanel);
	}

	/**
	 * Create the chartPanel containing the list enabling the navigation in the tabs and charts. 
	 * @return a <code>JPanel</code> instance.
	 */
	private JPanel createListPanel()
	{
		JPanel panel = GuiUtils.createBoxPanel(BoxLayout.X_AXIS);
		panel.add(createTabListPanel());
		panel.add(Box.createHorizontalStrut(10));
		panel.add(createChartListPanel());
		return panel;
	}
	
	/**
	 * Create the panel containing the list of tabs and the associated buttons.
	 * @return a <code>JPanel</code> instance.
	 */
	private JPanel createTabListPanel()
	{
		JPanel panel = GuiUtils.createBoxPanel(BoxLayout.X_AXIS);
		JPanel btnPanel = GuiUtils.createBoxPanel(BoxLayout.Y_AXIS);
		defineTabActions();
		for (int i=0; i<tabActions.length; i++)
		{
			JButton btn = new JButton(tabActions[i]);
			btn.setMinimumSize(new Dimension(80, 20));
			btn.setMaximumSize(new Dimension(80, 20));
			btnPanel.add(Box.createVerticalStrut(5));
			btnPanel.add(btn);
		}
		btnPanel.add(Box.createVerticalGlue());
		tabsJList = new JList();
		tabsJList.setPreferredSize(new Dimension(150, 100));
		tabsJList.setOpaque(false);
		tabsJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tabsJList.setModel(new DefaultListModel());
		DefaultListModel model = (DefaultListModel) tabsJList.getModel();
		for (TabConfiguration tab: builder.getTabList()) model.addElement(tab);
		tabsJList.addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				TabConfiguration tab = (TabConfiguration) tabsJList.getSelectedValue();
				tabSelected(tab);
			}
		});
		JScrollPane tabScrollPane = new JScrollPane(tabsJList);
		tabScrollPane.setPreferredSize(new Dimension(150, 100));
		tabScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		tabScrollPane.setBorder(BorderFactory.createTitledBorder("Tabs"));
		panel.add(btnPanel);
		btnPanel.add(Box.createHorizontalStrut(5));
		panel.add(tabScrollPane);
		return panel;
	}

	/**
	 * Instanciate and define the actions for the list of tabs.
	 */
	private void defineTabActions()
	{
		tabActions = new ButtonAction[4];
		tabActions[0] = new ButtonAction("New")
		{
			public void resetEnabledState()
			{// this action is always enabled
			}
			public void perform()
			{
				String s = JOptionPane.showInputDialog(ChartConfigurationPanel.this, "Enter a name for the new tab", "new tab");
				if (s != null) addNewTab(s);
			}
		};
		tabActions[1] = new ButtonAction("Remove")
		{
			public void resetEnabledState()
			{
				setEnabled(tabsJList.getSelectedValue() != null);
			}
			public void perform()
			{
				removeTab(currentTab);
				currentTab = null;
			}
		};
		tabActions[2] = new ButtonAction("Up")
		{
			public void resetEnabledState()
			{
				setEnabled(tabsJList.getSelectedIndex() > 0);
			}
			public void perform()
			{
				moveTab(currentTab, -1);
			}
		};
		tabActions[3] = new ButtonAction("Down")
		{
			public void resetEnabledState()
			{
				DefaultListModel model = (DefaultListModel) tabsJList.getModel();
				setEnabled(tabsJList.getSelectedIndex() < model.getSize() - 1);
			}
			public void perform()
			{
				moveTab(currentTab, 1);
			}
		};
	}

	/**
	 * Add a new tab to the list of tabs.
	 * @param name the name of the tab to create.
	 */
	private void addNewTab(String name)
	{
		TabConfiguration tab = new TabConfiguration(name, -1);
		builder.addTab(tab);
		populateTabsList(tab);
		fieldsPanel.populateTabsCombo(tab);
	}

	/**
	 * Remove a tab from the list of tabs.
	 * @param tab the tab to remove.
	 */
	private void removeTab(TabConfiguration tab)
	{
		builder.removeTab(tab);
		populateTabsList(null);
		fieldsPanel.populateTabsCombo(null);
	}

	/**
	 * Move a tab up or down a specified increment in the list of tabs.
	 * @param tab the tab to move.
	 * @param increment the distance and direction in which to move the tab.
	 */
	private void moveTab(TabConfiguration tab, int increment)
	{
		builder.removeTab(tab);
		tab.position += increment;
		builder.addTab(tab);
		fieldsPanel.removeFieldsListeners();
		populateTabsList(tab);
		fieldsPanel.populateTabsCombo(tab);
		fieldsPanel.addFieldsListeners();
	}

	/**
	 * Create the panel containing the list of charts and the associated buttons.
	 * @return a <code>JPanel</code> instance.
	 */
	private JPanel createChartListPanel()
	{
		JPanel panel = GuiUtils.createBoxPanel(BoxLayout.X_AXIS);
		JPanel btnPanel = GuiUtils.createBoxPanel(BoxLayout.Y_AXIS);
		defineChartActions();
		for (int i=0; i<chartActions.length; i++)
		{
			JButton btn = new JButton(chartActions[i]);
			btn.setMinimumSize(new Dimension(80, 20));
			btn.setMaximumSize(new Dimension(80, 20));
			btnPanel.add(Box.createVerticalStrut(5));
			btnPanel.add(btn);
		}
		btnPanel.add(Box.createVerticalGlue());
		chartsJList = new JList();
		chartsJList.setOpaque(false);
		chartsJList.setModel(new DefaultListModel());
		chartsJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		chartsJList.addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				ChartConfiguration config = (ChartConfiguration) chartsJList.getSelectedValue();
				chartSelected(config);
			}
		});
		JScrollPane chartScrollPane = new JScrollPane(chartsJList);
		chartScrollPane.setPreferredSize(new Dimension(150, 100));
		chartScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		chartScrollPane.setBorder(BorderFactory.createTitledBorder("Charts"));
		panel.add(btnPanel);
		btnPanel.add(Box.createHorizontalStrut(5));
		panel.add(chartScrollPane);
		return panel;
	}

	/**
	 * Instanciate and define the actions for the list of charts.
	 */
	private void defineChartActions()
	{
		chartActions = new ButtonAction[3];
		/*chartActions[0] = new ButtonAction("New")
		{
			public void resetEnabledState()
			{// this action is always enabled
			}
			public void perform()
			{
				String s = JOptionPane.showInputDialog(ChartConfigurationPanel.this, "Enter a name for the new tab", "new tab");
				if (s != null) addNewTab(s);
			}
		};*/
		chartActions[0] = new ButtonAction("Remove")
		{
			public void resetEnabledState()
			{
				setEnabled(chartsJList.getSelectedValue() != null);
			}
			public void perform()
			{
				doRemove();
			}
		};
		chartActions[1] = new ButtonAction("Up")
		{
			public void resetEnabledState()
			{
				setEnabled(chartsJList.getSelectedIndex() > 0);
			}
			public void perform()
			{
				moveChart(currentConfig, -1);
			}
		};
		chartActions[2] = new ButtonAction("Down")
		{
			public void resetEnabledState()
			{
				DefaultListModel model = (DefaultListModel) chartsJList.getModel();
				setEnabled((chartsJList.getSelectedIndex() >= 0) && (chartsJList.getSelectedIndex() < model.getSize() - 1));
			}
			public void perform()
			{
				moveChart(currentConfig, 1);
			}
		};
	}

	/**
	 * Move a chart up in the list of charts.
	 * @param config the tab to move.
	 * @param increment the distance and direction in which to move the tab.
	 */
	private void moveChart(ChartConfiguration config, int increment)
	{
		builder.removeChart(currentTab, config);
		config.position += increment;
		builder.addChart(currentTab, config);
		fieldsPanel.removeFieldsListeners();
		resetChartsList(currentTab);
		chartsJList.setSelectedValue(config, true);
		fieldsPanel.addFieldsListeners();
	}

	/**
	 * Repopulate the lists of tabs and charts.
	 */
	private void resetTabsList()
	{
		fieldsPanel.removeFieldsListeners();
		populateTabsList(null);
		resetChartsList(null);
		fieldsPanel.addFieldsListeners();
	}
	
	/**
	 * Refresh the content of the list of tabs.
	 * @param tab if not null, determines which tab should be selected.
	 */
	private void populateTabsList(TabConfiguration tab)
	{
		DefaultListModel model = (DefaultListModel) tabsJList.getModel();
		model.removeAllElements();
		for (TabConfiguration t: builder.getTabList()) model.addElement(t);
		if (tab != null) tabsJList.setSelectedValue(tab, true);
	}
	
	/**
	 * Repopulate the lists of tabs and charts.
	 * @param tab the tab name.
	 */
	private void resetChartsList(TabConfiguration tab)
	{
		fieldsPanel.removeFieldsListeners();
		chartsJList.clearSelection();
		DefaultListModel model = (DefaultListModel) chartsJList.getModel();
		model.removeAllElements();
		if (tab != null)
		{
			for (ChartConfiguration config : tab.configs) model.addElement(config);
		}
		fieldsPanel.addFieldsListeners();
	}
	
	/**
	 * Called when the tab selection has changed.
	 * @param tab the name of the selected tab, or null if the selectionis empty.
	 */
	private void tabSelected(TabConfiguration tab)
	{
		fieldsPanel.removeFieldsListeners();
		currentTab = tab;
		chartsJList.clearSelection();
		currentConfig = null;
		DefaultListModel model = (DefaultListModel) chartsJList.getModel();
		model.removeAllElements();
		if (tab == null) return;
		for (ChartConfiguration config: tab.configs) model.addElement(config);
		resetAllEnabledStates();
		fieldsPanel.addFieldsListeners();
	}

	/**
	 * Called when the chart selection has changed.
	 * @param config the chart configuration that was selected.
	 */
	private void chartSelected(ChartConfiguration config)
	{
		currentConfig = config;
		boolean oldActive = fieldsPanel.isFieldListenersActive();
		if (oldActive) fieldsPanel.removeFieldsListeners();
		populateFields(config);
		if (oldActive) fieldsPanel.addFieldsListeners();
		resetAllEnabledStates();
		fieldsPanel.changePreview(fieldsPanel.getConfiguration());
	}
	
	/**
	 * Action called when the &quot;Saves as new&quot; button is pressed.
	 */
	public void doSaveAsNew()
	{
		fieldsPanel.removeFieldsListeners();
		ChartConfiguration config = fieldsPanel.getConfiguration();
		builder.addChart(currentTab, config);
		resetTabsList();
		resetChartsList(currentTab);
		tabsJList.setSelectedValue(currentTab, true);
		chartsJList.setSelectedValue(config, true);
		fieldsPanel.addFieldsListeners();
	}
	
	/**
	 * Action called when the &quot;Update&quot; button is pressed.
	 * @param newTab the tab in which the update occurs.
	 */
	public void doUpdate(TabConfiguration newTab)
	{
		fieldsPanel.removeFieldsListeners();
		ChartConfiguration oldConfig = currentConfig;
		ChartConfiguration newConfig = fieldsPanel.getConfiguration();

		if (newTab == currentTab) newConfig.position = currentTab.configs.indexOf(oldConfig);
		builder.removeChart(currentTab, oldConfig);
		builder.addChart(newTab, newConfig);
		resetTabsList();
		resetChartsList(newTab);
		tabsJList.setSelectedValue(newTab, true);
		chartsJList.setSelectedValue(newConfig, true);
		fieldsPanel.addFieldsListeners();
	}
	
	/**
	 * Action called when the &quot;Remove&quot; button is pressed.
	 */
	public void doRemove()
	{
		if (currentConfig == null) return;
		fieldsPanel.removeFieldsListeners();
		builder.removeChart(currentTab, currentConfig);
		resetTabsList();
		resetChartsList(currentTab);
		tabsJList.setSelectedValue(currentTab, true);
		chartsJList.setSelectedValue(currentConfig, true);
		fieldsPanel.addFieldsListeners();
	}
	
	/**
	 * Set the values in the configuration fields.
	 * @param config the chart configurationfrom which the values are taken.
	 */
	private void populateFields(ChartConfiguration config)
	{
		if (currentTab == null) return;
		if (config == null) return;
		fieldsPanel.populateFields(currentTab, config);
	}
	
	/**
	 * Get the chart builder for this configuration panel.
	 * @return a <code>JPPFChartBuilder</code> instance.
	 */
	public JPPFChartBuilder getBuilder()
	{
		return builder;
	}

	/**
	 * Referesh the enabled states of all buttons for the tabs and charts lists.
	 */
	private void resetAllEnabledStates()
	{
		for (int i=0; i<tabActions.length; i++) tabActions[i].resetEnabledState();
		for (int i=0; i<chartActions.length; i++) chartActions[i].resetEnabledState();
	}

	/**
	 * Common superclass for all buttons associated witht he tabs and charts lists.
	 */
	public abstract class ButtonAction extends AbstractAction
	{
		/**
		 * Initialize this action with a specified name.
		 * @param name the name of this action, displayed as a button text.
		 */
		public ButtonAction(String name)
		{
			super(name);
		}

		/**
		 * Perform the action.
		 * @param e the event that triggered this action.
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		public final void actionPerformed(ActionEvent e)
		{
			perform();
			resetAllEnabledStates();
		}

		/**
		 * The actual action to perform(). Subclasses must implement this method.
		 */
		public abstract void perform();

		/**
		 * Determine the enable states of this action.
		 */
		public abstract void resetEnabledState();
	}
}
