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

import java.awt.*;
import java.awt.Dimension;
import java.awt.event.*;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.JPanel;
import javax.swing.event.*;
import javax.swing.event.ListSelectionListener;
import org.jppf.ui.monitoring.GuiUtils;
import org.jppf.ui.monitoring.charts.ChartType;
import org.jppf.ui.monitoring.data.StatsConstants;

/**
 * 
 * @author Laurent Cohen
 */
public class FieldsPanel extends JPanel
{
	/**
	 * Holds the active chart configurations.
	 */
	private JPPFChartBuilder builder = null;
	/**
	 * Text field containing the unit to display in the legend or item labels.
	 */
	private JTextField unitField = null;
	/**
	 * Text field containing the name of the chart.
	 */
	private JTextField nameField = null;
	/**
	 * Text field containing the name of the tab the chart belongs.
	 */
	private JComboBox tabNameCombo = null;
	/**
	 * Spinner used to set the precision.
	 */
	private JSpinner precisionSpinner = null;
	/**
	 * Combo box used to select the type of chart.
	 */
	private JComboBox chartTypeCombo = null;
	/**
	 * JList containing the list of fields to display in the chart.
	 */
	private JList fieldsJList = null;
	/**
	 * Panel used to preview the chart.
	 */
	private JPanel previewPanel = null;
	/**
	 * The selection listener for the JList containing the field names to display in a chart.
	 */
	private ListSelectionListener fieldsSelectionListener = null;
	/**
	 * The action listener for the combo box containing the the chart types.
	 */
	private ActionListener chartTypeListener = null;
	/**
	 * Indicates if the fields listeners are currently set onto their respective components.
	 */
	private boolean fieldListenersActive = false;
	/**
	 * The configuration panel this panel is a part of.
	 */
	private ChartConfigurationPanel configPanel = null;

	/**
	 * Initialize this chart configuration chartPanel with the specified chart builder.
	 * @param configPanel the configuration panel this panel is a part of.
	 */
	public FieldsPanel(ChartConfigurationPanel configPanel)
	{
		this.configPanel = configPanel;
		this.builder = configPanel.getBuilder();
		init();
	}

	/**
	 * Create the chartPanel where the configuration parameters are displayed and set.
	 */
	private void init()
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		nameField = new JTextField();
		tabNameCombo = new JComboBox();
		tabNameCombo.setEditable(false);
		populateTabsCombo(null);
		chartTypeCombo = new JComboBox(ChartType.values());
		precisionSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 18, 1));
		unitField = new JTextField();
		fieldsJList = new JList();
		fieldsJList.setOpaque(false);
		DefaultListModel listModel = new DefaultListModel();
		fieldsJList.setModel(listModel);
		for (String field: StatsConstants.ALL_FIELDS) listModel.addElement(field);
		fieldsJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		JScrollPane fieldScrollPane = new JScrollPane(fieldsJList);
		fieldScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		fieldScrollPane.setBorder(BorderFactory.createTitledBorder("Fields"));

		JPanel leftPanel = GuiUtils.createBoxPanel(BoxLayout.Y_AXIS);
		leftPanel.setBorder(BorderFactory.createTitledBorder("Configuration"));
		leftPanel.add(createLabelPanel("Tab name:", tabNameCombo));
		leftPanel.add(createLabelPanel("Chart name:", nameField));
		leftPanel.add(createLabelPanel("Chart type:", chartTypeCombo));
		leftPanel.add(createLabelPanel("Precision:", precisionSpinner));
		leftPanel.add(createLabelPanel("Unit:", unitField));
		leftPanel.add(Box.createVerticalGlue());

		JPanel rightPanel = GuiUtils.createBoxPanel(BoxLayout.Y_AXIS);
		rightPanel.add(fieldScrollPane);
		JPanel topPanel = GuiUtils.createBoxPanel(BoxLayout.X_AXIS);
		topPanel.add(leftPanel);
		topPanel.add(Box.createHorizontalStrut(10));
		topPanel.add(rightPanel);
		topPanel.add(Box.createHorizontalStrut(10));
		topPanel.add(createConfigButtonsPanel());
		previewPanel = GuiUtils.createBoxPanel(BoxLayout.Y_AXIS);
		previewPanel.setPreferredSize(new Dimension(150, 100));
		add(topPanel);
		topPanel.add(Box.createVerticalStrut(5));
		add(previewPanel);

		defineFieldsListeners();
		addFieldsListeners();
	}
	
	/**
	 * Refresh the list combo box that holds the list of available tabs.
	 * @param tab the tab to set as selected, may be null.
	 */
	public void populateTabsCombo(TabConfiguration tab)
	{
		tabNameCombo.removeAllItems();
		for (TabConfiguration t: builder.getTabList()) tabNameCombo.addItem(t);
		if (tab != null) tabNameCombo.setSelectedItem(tab);
	}

	/**
	 * Create a chartPanel associating a label with a component.
	 * @param text the text of the label.
	 * @param comp the component associated with the label.
	 * @return a <code>JPanel</code> instance.
	 */
	private JPanel createLabelPanel(String text, JComponent comp)
	{
		JPanel panel = new JPanel();
		JLabel label = new JLabel(text);
		panel.add(BorderLayout.EAST, label);
		panel.add(BorderLayout.CENTER, comp);
		label.setPreferredSize(new Dimension(100, 20));
		comp.setPreferredSize(new Dimension(100, 20));
		panel.setPreferredSize(new Dimension(100, 25));
		return panel;
	}

	/**
	 * Create the buttons chartPanel to allow saving configuration changes.
	 * @return a <code>JPanel</code> instance.
	 */
	private JPanel createConfigButtonsPanel()
	{
		JPanel panel = GuiUtils.createBoxPanel(BoxLayout.Y_AXIS);
		panel.setBorder(BorderFactory.createTitledBorder("Actions"));
		JButton[] btn = new JButton[] { new JButton("Save as new"), new JButton("Update"), new JButton("Remove") };
		btn[0].addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				configPanel.doSaveAsNew();
			}
		});
		btn[1].addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				TabConfiguration newTab = (TabConfiguration) tabNameCombo.getSelectedItem();
				configPanel.doUpdate(newTab);
			}
		});
		btn[2].addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				configPanel.doRemove();
			}
		});

		Dimension d = new Dimension(90, 20);
		for (int i=0; i<btn.length; i++)
		{
			btn[i].setMinimumSize(d);
			btn[i].setMaximumSize(d);
			panel.add(Box.createVerticalStrut(10));
			btn[i].setAlignmentX(0.5f);
			panel.add(btn[i]);
		}
		panel.add(Box.createVerticalGlue());
		return panel;
	}

	/**
	 * Create the listeners set onto the configuration fields to automatically update the preview panel.
	 */
	private void defineFieldsListeners()
	{
		fieldsSelectionListener = new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				changePreview(getConfiguration());
			}
		};
		chartTypeListener = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				changePreview(getConfiguration());
			}
		};
	}
	
	/**
	 * Add all the listeners to the appropriate configuration components.
	 */
	public void addFieldsListeners()
	{
		fieldsJList.addListSelectionListener(fieldsSelectionListener);
		chartTypeCombo.addActionListener(chartTypeListener);
		fieldListenersActive = true;
	}
	
	/**
	 * Remove all the listeners from the appropriate configuration components.
	 */
	public void removeFieldsListeners()
	{
		fieldListenersActive = false;
		fieldsJList.removeListSelectionListener(fieldsSelectionListener);
		chartTypeCombo.removeActionListener(chartTypeListener);
	}

	/**
	 * Update the preview chartPanel according to a specified chart configuration.
	 * @param config the configuration of the chart to dispaly in the preview chartPanel.
	 */
	public void changePreview(final ChartConfiguration config)
	{
		if (config == null) return;
		builder.createChart(config, true);
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				previewPanel.removeAll();
				previewPanel.add(config.chartPanel);
				config.chart.setBackgroundPaint(previewPanel.getBackground());
				previewPanel.updateUI();
			}
		});
	}

	/**
	 * Create a chart configuration from the configuration fields.
	 * @return the chart configuration created form the configuration fields.
	 */
	public ChartConfiguration getConfiguration()
	{
		ChartConfiguration config = new ChartConfiguration();
		config.name = nameField.getText();
		config.unit = unitField.getText();
		if ("".equals(config.unit)) config.unit = null;
		SpinnerNumberModel spinnerModel = (SpinnerNumberModel) precisionSpinner.getModel();
		config.precision = ((Number) spinnerModel.getValue()).intValue();
		config.type = (ChartType) chartTypeCombo.getSelectedItem();
		Object[] sel = fieldsJList.getSelectedValues();
		String[] fields = new String[sel.length];
		for (int i=0; i<sel.length; i++) fields[i] = (String) sel[i];
		config.fields = fields;
		return config;
	}

	/**
	 * Set the values in the configuration fields.
	 * @param tab the tab to which the field belongs.
	 * @param config the chart configurationfrom which the values are taken.
	 */
	public void populateFields(TabConfiguration tab, ChartConfiguration config)
	{
		removeFieldsListeners();
		nameField.setText(config.name);
		tabNameCombo.setSelectedItem(tab);
		unitField.setText(config.unit == null ? "" : config.unit);
		SpinnerNumberModel spinnerModel = (SpinnerNumberModel) precisionSpinner.getModel();
		spinnerModel.setValue(config.precision);
		chartTypeCombo.setSelectedItem(config.type);
		fieldsJList.clearSelection();
		DefaultListModel listModel = (DefaultListModel) fieldsJList.getModel();
		int[] indices = new int[config.fields.length];
		for (int i=0; i<indices.length; i++)
		{
			int n = listModel.indexOf(config.fields[i]);
			indices[i] = n;
		}
		fieldsJList.setSelectedIndices(indices);
		addFieldsListeners();
	}

	/**
	 * Indicates if the fields listeners are currently set onto their respective components.
	 * @return true if the listeners are set, false otherwise.
	 */
	public boolean isFieldListenersActive()
	{
		return fieldListenersActive;
	}

	/**
	 * Set the active state of the fields listeners.
	 * @param fieldListenersActive true to activate the listeners , false to deactivate them.
	 */
	public void setFieldListenersActive(boolean fieldListenersActive)
	{
		this.fieldListenersActive = fieldListenersActive;
	}
}
