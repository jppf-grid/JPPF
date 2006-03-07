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
import javax.swing.*;
import org.jppf.server.protocol.AdminRequestHeader;
import org.jppf.ui.monitoring.data.StatsHandler;

/**
 * Options chartPanel for the monitor.
 * @author Laurent Cohen
 */
public class AdminPanel extends JPanel
{
	/**
	 * The field containing the delay before server shutdown.
	 */
	private JFormattedTextField shutdownField = new JFormattedTextField();
	/**
	 * The field containing the delay before server shutdown.
	 */
	private JFormattedTextField restartField = new JFormattedTextField();
	/**
	 * Determines whether the server should be retsarted after shutdown.
	 */
	private JCheckBox restartBox = null;
	/**
	 * The stats formatter that provides the data.
	 */
	private StatsHandler statsFormatter = null;

	/**
	 * Default contructor.
	 * @param statsFormatter the monitoring chartPanel to which the options apply.
	 */
	public AdminPanel(StatsHandler statsFormatter)
	{
		this.statsFormatter = statsFormatter;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(createShutdownRestartPanel());
		add(Box.createVerticalGlue());
	}
	
	/**
	 * Creates a chartPanel with a field to enter the shutdown delay, a field to enter the restart delay,
	 * and a button to send the shutdown/restart request to the server.
	 * @return a <code>JPanel</code> instance.
	 */
	private JPanel createShutdownRestartPanel()
	{
		JPanel panel = GuiUtils.createBoxPanel(BoxLayout.Y_AXIS);
		panel.setBorder(BorderFactory.createTitledBorder("Server shutdown/restart"));

		JPanel shutdownPanel = createShtudownPanel();
		JPanel restartPanel = createRestartPanel();
		JPanel btnPanel = createBtnPanel();
		JPanel checkboxPanel = createCheckboxPanel();

		panel.add(Box.createVerticalStrut(5));
		panel.add(btnPanel);
		panel.add(Box.createVerticalStrut(5));
		panel.add(shutdownPanel);
		panel.add(Box.createVerticalStrut(5));
		panel.add(checkboxPanel);
		panel.add(Box.createVerticalStrut(5));
		panel.add(restartPanel);
		panel.add(Box.createVerticalGlue());
		return panel;
	}
	
	/**
	 * Create the shutdown delay field.
	 * @return a <code>JPanel</code> instance.
	 */
	private JPanel createShtudownPanel()
	{
		JPanel shutdownPanel = GuiUtils.createBoxPanel(BoxLayout.X_AXIS);
		shutdownField.setValue(3000L); 
		shutdownField.setHorizontalAlignment(JTextField.RIGHT);
		shutdownField.setPreferredSize(new Dimension(100, 20));
		shutdownField.setMinimumSize(new Dimension(100, 20));
		shutdownField.setMaximumSize(new Dimension(100, 20));
		shutdownPanel.add(Box.createHorizontalStrut(5));
		shutdownPanel.add(new JLabel("Shutdown delay (ms)")); 
		shutdownPanel.add(Box.createHorizontalStrut(5));
		shutdownPanel.add(shutdownField);
		shutdownPanel.add(Box.createHorizontalGlue());
		shutdownPanel.setPreferredSize(new Dimension(25, 300));
		return shutdownPanel;
	}
	
	/**
	 * Create the restart delay field.
	 * @return a <code>JPanel</code> instance.
	 */
	private JPanel createRestartPanel()
	{
		JPanel restartPanel = GuiUtils.createBoxPanel(BoxLayout.X_AXIS);
		restartField.setValue(3000L); 
		restartField.setHorizontalAlignment(JTextField.RIGHT);
		restartField.setPreferredSize(new Dimension(100, 20));
		restartField.setMinimumSize(new Dimension(100, 20));
		restartField.setMaximumSize(new Dimension(100, 20));
		restartPanel.add(Box.createHorizontalStrut(25));
		restartPanel.add(new JLabel("Restart delay (ms)")); 
		restartPanel.add(Box.createHorizontalStrut(5));
		restartPanel.add(restartField);
		restartPanel.add(Box.createHorizontalGlue());
		restartPanel.setPreferredSize(new Dimension(50, 300));
		return restartPanel;
	}
	
	/**
	 * Create the button to send the request.
	 * @return a <code>JPanel</code> instance.
	 */
	private JPanel createBtnPanel()
	{
		JButton btn = new JButton("Perform now");
		btn.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				long shutdownDelay = (Long) shutdownField.getValue();
				long restartDelay = 0L;
				String command = null;
				if (restartBox.isSelected())
				{
					restartDelay = (Long) restartField.getValue();
					command = AdminRequestHeader.ADMIN_SHUTDOWN_RESTART;
				}
				else command = AdminRequestHeader.ADMIN_SHUTDOWN;
				statsFormatter.requestShutdownRestart(command, shutdownDelay, restartDelay);
			}
		});
		btn.setPreferredSize(new Dimension(100, 20));
		JPanel btnPanel = GuiUtils.createBoxPanel(BoxLayout.X_AXIS);
		btnPanel.add(Box.createHorizontalStrut(5));
		btnPanel.add(btn);
		btnPanel.add(Box.createHorizontalGlue());
		btnPanel.setPreferredSize(new Dimension(25, 300));
		return btnPanel;
	}
	
	/**
	 * Create the checkbox that determines whether a restart should be performed after the shutdown.
	 * @return a <code>JPanel</code> instance.
	 */
	private JPanel createCheckboxPanel()
	{
		JPanel checkboxPanel = GuiUtils.createBoxPanel(BoxLayout.X_AXIS);
		restartBox = new JCheckBox("Restart", true);
		restartBox.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				restartField.setEnabled(restartBox.isSelected());
			}
		});
		checkboxPanel.add(Box.createHorizontalStrut(5));
		checkboxPanel.add(restartBox);
		checkboxPanel.add(Box.createHorizontalGlue());
		restartBox.setPreferredSize(new Dimension(100, 20));
		restartBox.setMinimumSize(new Dimension(100, 20));
		restartBox.setMaximumSize(new Dimension(100, 20));
		return checkboxPanel;
	}
}
