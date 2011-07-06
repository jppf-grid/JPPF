/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

import java.util.*;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

import net.miginfocom.swing.MigLayout;

import org.jppf.ui.monitoring.data.*;
import org.jppf.ui.monitoring.event.*;
import org.jppf.utils.LocalizationUtils;
import org.slf4j.*;

/**
 * This class provides a graphical interface for monitoring the status and health 
 * of the JPPF server.<br>
 * It also provides a few customization options, such as setting the interval between 2 server refreshes,
 * and switching the color scheme (skin) fot the whole UI.
 * @author Laurent Cohen
 */
public class MonitoringPanel extends JPanel implements StatsHandlerListener, StatsConstants
{
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
	 * Holds a list of table models to update wwhen new stats are received.
	 */
	private List<MonitorTableModel> tableModels = new ArrayList<MonitorTableModel>();

	/**
	 * Default contructor.
	 */
	public MonitoringPanel()
	{
		this.statsHandler = StatsHandler.getInstance();
		setLayout(new MigLayout("fill, flowy"));
		addTablePanel(EXECUTION_PROPS, "ExecutionTable.label");
		addTablePanel(NODE_EXECUTION_PROPS, "NodeExecutionTable.label");
		addTablePanel(TRANSPORT_PROPS, "NetworkOverheadTable.label");
		addTablePanel(QUEUE_PROPS, "QueueTable.label");
		addTablePanel(CONNECTION_PROPS, "ConnectionsTable.label");
		statsHandler.addStatsHandlerListener(this);
	}

	/**
	 * Add a table panel to this panel.
	 * @param fields the fields displayed in the table.
	 * @param label the reference to the localized title of the table.
	 */
	private void addTablePanel(Fields[] fields, String label)
	{
		add(makeTablePanel(fields, LocalizationUtils.getLocalized(BASE, label)), "grow");
	}
	
	/**
	 * Called when new stats have been received from the server.
	 * @param event holds the new stats values.
	 */
	public void dataUpdated(StatsHandlerEvent event)
	{
		for (final MonitorTableModel model: tableModels)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
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
	private JComponent makeTablePanel(Fields[] props, String title)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("fill"));
		panel.setBorder(BorderFactory.createTitledBorder(title));
		JTable table = new JTable()
		{
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		};
		MonitorTableModel model = new MonitorTableModel(props);
		table.setModel(model);
		table.setOpaque(true);
		DefaultTableCellRenderer rend1 = new DefaultTableCellRenderer();
		rend1.setHorizontalAlignment(JLabel.RIGHT);
		rend1.setOpaque(true);
		table.getColumnModel().getColumn(1).setCellRenderer(rend1);
		DefaultTableCellRenderer rend0 = new DefaultTableCellRenderer();
		rend0.setHorizontalAlignment(JLabel.LEFT);
		rend0.setOpaque(true);
		table.getColumnModel().getColumn(0).setCellRenderer(rend0);
	  for (int i=0; i<model.getColumnCount(); i++) table.sizeColumnsToFit(i);
		tableModels.add(model);
		panel.add(table, "growx, pushx");
		table.setShowGrid(false);
		return panel;
	}
}
