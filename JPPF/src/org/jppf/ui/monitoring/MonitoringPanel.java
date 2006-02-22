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
package org.jppf.ui.monitoring;

import java.awt.Dimension;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import org.apache.log4j.Logger;
import org.jppf.ui.monitoring.event.*;
import org.jvnet.substance.SubstanceLookAndFeel;

/**
 * This class provides a graphical interface for monitoring the status and health 
 * of the JPPF server.<br>
 * It also provides a few customization options, such as setting the interval between 2 server refreshes,
 * and switching the color scheme (skin) fot the whole UI.
 * @author Laurent Cohen
 */
public class MonitoringPanel extends JPanel implements StatsFormatterListener, StatsConstants
{
	/**
	 * Log4j logger for this class.
	 */
	static Logger log = Logger.getLogger(MonitoringPanel.class);
	/**
	 * Association of value names to the corresponding <code>JLabel</code> components.
	 */
	private Map<String, JLabel> nameLabels = new HashMap<String, JLabel>();
	/**
	 * Association of values to the corresponding <code>JLabel</code> components.
	 */
	private Map<String, JLabel> valueLabels = new HashMap<String, JLabel>();
	/**
	 * The stats formatter that provides the data.
	 */
	private StatsFormatter statsFormatter = null;

	/**
	 * Default contructor.
	 * @param statsFormatter the stats formatter that provides the data.
	 */
	public MonitoringPanel(StatsFormatter statsFormatter)
	{
		this.statsFormatter = statsFormatter;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(Box.createVerticalStrut(5));
		add(makeRefreshPanel());
		add(Box.createVerticalStrut(5));
		add(makeValuesPanel(EXECUTION_PROPS, "Execution"));
		add(Box.createVerticalStrut(5));
		add(makeValuesPanel(QUEUE_PROPS, "Queue"));
		add(Box.createVerticalStrut(5));
		add(makeValuesPanel(CONNECTION_PROPS, "Connections"));
		add(Box.createVerticalStrut(5));
	}
	
	/**
	 * Called when new stats have been received from the server.
	 * @param event holds the new stats values.
	 */
	public void dataUpdated(StatsFormatterEvent event)
	{
		final StatsFormatter sf = event.getStatsFormatter();
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				for (String name: nameLabels.keySet())
				{
					valueLabels.get(name).setText(sf.getStringValueMap().get(name));
				}
			}
		});
	}
	
	/**
	 * Create a panel with a &quot;refresh now&quote; button.
	 * @return a <code>JPanel</code> instance.
	 */
	private JPanel makeRefreshPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		JButton btn = new JButton("Refresh Now");
		btn.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				statsFormatter.requestUpdate();
			}
		});
		btn.setPreferredSize(new Dimension(100, 20));
		panel.add(Box.createHorizontalStrut(5));
		panel.add(btn);
		panel.add(Box.createHorizontalGlue());
		return panel;
	}

	/**
	 * Create a panel displaying a group of values.
	 * @param props the names of the values to display.
	 * @param title the title of the panel.
	 * @return a <code>JPanel</code> instance.
	 */
	private JPanel makeValuesPanel(String[] props, String title)
	{
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createTitledBorder(title));
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		for (String name: props)
		{
			String value = statsFormatter.getStringValueMap().get(name);
			panel.add(makeValueLine(name, value));
		}
		return panel;
	}

	/**
	 * Create a panel displaying a value and its name.
	 * @param name the name of the value to display.
	 * @param value the value to display.
	 * @return a <code>JPanel</code> instance.
	 */
	private JPanel makeValueLine(String name, String value)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		JLabel nameLabel = new JLabel(name);
		nameLabel.setPreferredSize(new Dimension(150, 20));
		JLabel valueLabel = new JLabel(value);
		valueLabel.setPreferredSize(new Dimension(200, 20));
		valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);

		nameLabels.put(name, nameLabel);
		valueLabels.put(name, valueLabel);

		panel.add(Box.createHorizontalStrut(5));
		panel.add(nameLabel);
		panel.add(Box.createHorizontalStrut(5));
		panel.add(valueLabel);
		panel.add(Box.createHorizontalGlue());
		return panel;
	}

	/**
	 * Start this UI.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			UIManager.setLookAndFeel(new SubstanceLookAndFeel());
			SubstanceLookAndFeel.setCurrentWatermark(new TiledImageWatermark("org/jppf/ui/resources/GridWatermark.gif"));
			SubstanceLookAndFeel.setCurrentTheme(new JPPFTheme(new JPPFColorScheme(), "JPPF", true));
			StatsFormatter statsFormatter = new StatsFormatter();
			JFrame frame = new JFrame("Test");
			JTabbedPane tabbedPane = new JTabbedPane();
			MonitoringPanel monitor = new MonitoringPanel(statsFormatter);
			AdminPanel admin = new AdminPanel(statsFormatter);
			BarChartsPanel chartsPanel = new BarChartsPanel(statsFormatter);
			PlotChartsPanel plotPanel = new PlotChartsPanel(statsFormatter);
			statsFormatter.addStatsFormatterListener(monitor);
			statsFormatter.addStatsFormatterListener(chartsPanel);
			statsFormatter.addStatsFormatterListener(plotPanel);
			tabbedPane.addTab("Server Stats", monitor);
			tabbedPane.addTab("Admin", admin);
			tabbedPane.addTab("Snapshot Charts", chartsPanel);
			tabbedPane.addTab("Plot Charts", plotPanel);
			tabbedPane.addTab("Options", new OptionsPanel(statsFormatter));
			frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			frame.addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent e)
				{
					System.exit(0);
				}
			});
			frame.add(tabbedPane);
			frame.setSize(400, 600);
			frame.setVisible(true);
			statsFormatter.startRefreshTimer();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			log.error(e.getMessage(), e);
			System.exit(1);
		}
	}
}
