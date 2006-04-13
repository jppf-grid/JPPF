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
package org.jppf.ui.monitoring.admin;

import java.awt.Dimension;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import org.jppf.server.protocol.AdminRequest;
import org.jppf.ui.monitoring.GuiUtils;
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
	 * Spinner used to set the task bundle size on the server.
	 */
	private JSpinner bundleSizeSpinner = null;
	/**
	 * Determines whether the server should be retsarted after shutdown.
	 */
	private JCheckBox restartBox = null;
	/**
	 * The stats formatter that provides the data.
	 */
	private StatsHandler statsHandler = null;
	/**
	 * The field containing the admin password.
	 */
	private JPasswordField passwordField = new JPasswordField(15);
	/**
	 * The field containing the new admin password.
	 */
	private JPasswordField newPasswordField = new JPasswordField(15);
	/**
	 * The field containing the confirmation for the new admin password.
	 */
	private JPasswordField confirmPasswordField = new JPasswordField(15);
	/**
	 * Text area that contains messages in response to admin requests.
	 */
	private JTextArea messagesArea = new JTextArea();

	/**
	 * Default contructor.
	 * @param statsHandler the monitoring chartPanel to which the options apply.
	 */
	public AdminPanel(StatsHandler statsHandler)
	{
		this.statsHandler = statsHandler;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(createShutdownRestartPanel());
		add(Box.createVerticalStrut(5));
		add(createConfigurationPanel());
		add(Box.createVerticalStrut(5));
		add(createPasswordPanel());
		add(Box.createVerticalStrut(5));
		add(createMessagesPanel());
		add(Box.createVerticalGlue());
	}
	
	/**
	 * Creates a chartPanel with a field to enter the shutdown delay, a field to enter the restart delay,
	 * and a button to send the shutdown/restart request to the server.
	 * @return a <code>JPanel</code> instance.
	 */
	private JPanel createShutdownRestartPanel()
	{
		JPanel mainPanel = GuiUtils.createBoxPanel(BoxLayout.Y_AXIS);
		mainPanel.setBorder(BorderFactory.createTitledBorder("Server Shutdown / Restart"));
		JPanel panel = new JPanel();

		JPanel btnPanel = createBtnPanel();
		JPanel shutdownPanel = createShtudownPanel();
		JPanel checkboxPanel = createCheckboxPanel();
		JPanel restartPanel = createRestartPanel();
		
		Dimension d = new Dimension(250, 20);
		btnPanel.setPreferredSize(d);
		shutdownPanel.setPreferredSize(d);
		checkboxPanel.setPreferredSize(d);
		restartPanel.setPreferredSize(d);

		SpringLayout layout = new SpringLayout();
		panel.setLayout(layout);
		panel.add(btnPanel);
		panel.add(shutdownPanel);
		panel.add(checkboxPanel);
		panel.add(restartPanel);

		layout.putConstraint(SpringLayout.NORTH, btnPanel, 10, SpringLayout.NORTH, panel);
		layout.putConstraint(SpringLayout.NORTH, shutdownPanel, 10, SpringLayout.SOUTH, btnPanel);
		layout.putConstraint(SpringLayout.NORTH, checkboxPanel, 10, SpringLayout.SOUTH, shutdownPanel);
		layout.putConstraint(SpringLayout.NORTH, restartPanel, 10, SpringLayout.SOUTH, checkboxPanel);
		layout.putConstraint(SpringLayout.WEST, btnPanel, 10, SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.WEST, shutdownPanel, 10, SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.WEST, checkboxPanel, 10, SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.WEST, restartPanel, 10, SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.EAST, panel, 10, SpringLayout.EAST, btnPanel);

		mainPanel.add(panel);
		
		return mainPanel;
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
		shutdownPanel.setPreferredSize(new Dimension(300, 25));
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
		restartPanel.setPreferredSize(new Dimension(300, 50));
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
					command = AdminRequest.SHUTDOWN_RESTART;
				}
				else command = AdminRequest.SHUTDOWN;
				String pwd = new String(passwordField.getPassword());
				String msg = statsHandler.requestShutdownRestart(pwd, command, shutdownDelay, restartDelay);
				messagesArea.setText(msg);
			}
		});
		btn.setPreferredSize(new Dimension(100, 20));
		JPanel btnPanel = GuiUtils.createBoxPanel(BoxLayout.X_AXIS);
		btnPanel.add(Box.createHorizontalStrut(5));
		btnPanel.add(btn);
		btnPanel.add(Box.createHorizontalGlue());
		btnPanel.setPreferredSize(new Dimension(300, 25));
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

	/**
	 * Creates a chartPanel with a field to enter the shutdown delay, a field to enter the restart delay,
	 * and a button to send the shutdown/restart request to the server.
	 * @return a <code>JPanel</code> instance.
	 */
	private JPanel createPasswordPanel()
	{
		JPanel mainPanel = GuiUtils.createBoxPanel(BoxLayout.Y_AXIS);
		mainPanel.setBorder(BorderFactory.createTitledBorder("Administrator password"));
		JPanel panel = new JPanel();

		JPanel btnPanel = createPasswordBtnPanel();
		JPanel passwordPanel = createPasswordRowPanel(passwordField, "Password");
		JPanel newPasswordPanel = createPasswordRowPanel(newPasswordField, "New password");
		JPanel confirmPasswordPanel = createPasswordRowPanel(confirmPasswordField, "Confirm new password");
		
		Dimension d = new Dimension(250, 20);
		btnPanel.setPreferredSize(d);
		passwordPanel.setPreferredSize(d);
		newPasswordPanel.setPreferredSize(d);
		confirmPasswordPanel.setPreferredSize(d);

		SpringLayout layout = new SpringLayout();
		panel.setLayout(layout);
		panel.add(btnPanel);
		panel.add(passwordPanel);
		panel.add(newPasswordPanel);
		panel.add(confirmPasswordPanel);

		layout.putConstraint(SpringLayout.NORTH, btnPanel, 10, SpringLayout.NORTH, panel);
		layout.putConstraint(SpringLayout.NORTH, passwordPanel, 10, SpringLayout.SOUTH, btnPanel);
		layout.putConstraint(SpringLayout.NORTH, newPasswordPanel, 10, SpringLayout.SOUTH, passwordPanel);
		layout.putConstraint(SpringLayout.NORTH, confirmPasswordPanel, 10, SpringLayout.SOUTH, newPasswordPanel);
		layout.putConstraint(SpringLayout.WEST, btnPanel, 10, SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.WEST, passwordPanel, 10, SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.WEST, newPasswordPanel, 10, SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.WEST, confirmPasswordPanel, 10, SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.EAST, panel, 10, SpringLayout.EAST, btnPanel);

		mainPanel.add(panel);
		
		return mainPanel;
	}

	/**
	 * Create a panel containing a password field with a corresponding label in a row. 
	 * @param field the password field.
	 * @param labelText the label corresponding to the field.
	 * @return a <code>JPanel</code> instance.
	 */
	private JPanel createPasswordRowPanel(JPasswordField field, String labelText)
	{
		JPanel panel = GuiUtils.createBoxPanel(BoxLayout.X_AXIS);
		field.setHorizontalAlignment(JTextField.LEFT);
		JLabel label = new JLabel(labelText);
		label.setPreferredSize(new Dimension(120, 20));
		panel.add(Box.createHorizontalStrut(5));
		panel.add(label); 
		panel.add(Box.createHorizontalStrut(5));
		panel.add(field);
		panel.add(Box.createHorizontalGlue());
		panel.setPreferredSize(new Dimension(25, 250));
		return panel;
	}

	/**
	 * Create the button to send the request for changing the admin password.
	 * @return a <code>JPanel</code> instance.
	 */
	private JPanel createPasswordBtnPanel()
	{
		JButton btn = new JButton("Change password");
		btn.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				String pwd = new String(passwordField.getPassword());
				String newPwd = new String(newPasswordField.getPassword());
				String confirmPwd = new String(confirmPasswordField.getPassword());
				if (validateNewPassword(newPwd, confirmPwd))
				{
					String msg = statsHandler.changeAdminPassword(pwd, newPwd);
					messagesArea.setText(msg);
				}
			}
		});
		btn.setPreferredSize(new Dimension(120, 20));
		JPanel btnPanel = GuiUtils.createBoxPanel(BoxLayout.X_AXIS);
		btnPanel.add(Box.createHorizontalStrut(5));
		btnPanel.add(btn);
		btnPanel.add(Box.createHorizontalGlue());
		btnPanel.setPreferredSize(new Dimension(25, 300));
		return btnPanel;
	}

	/**
	 * Creates a chartPanel with a field to enter the shutdown delay, a field to enter the restart delay,
	 * and a button to send the shutdown/restart request to the server.
	 * @return a <code>JPanel</code> instance.
	 */
	private JPanel createMessagesPanel()
	{
		JPanel mainPanel = GuiUtils.createBoxPanel(BoxLayout.Y_AXIS);
		mainPanel.setBorder(BorderFactory.createTitledBorder("Server messages"));
		messagesArea.setPreferredSize(new Dimension(200, 100));
		messagesArea.setEditable(false);
		messagesArea.setOpaque(false);
		//messagesArea.setText("Server messages");
		JScrollPane scrollPane = new JScrollPane(messagesArea);
		mainPanel.add(scrollPane);
		return mainPanel;
	}
	
	/**
	 * Perform a validation of the new password before a password change.
	 * @param newPwd the new admin password to set.
	 * @param confirmPwd a confirmation of the new password.
	 * @return true if the new password is valid, false otherwise.
	 */
	private boolean validateNewPassword(String newPwd, String confirmPwd)
	{
		String msg = null;
		if ((newPwd == null) || "".equals(newPwd.trim()))
		{
			msg = "The new password must not be empty, with at least one non-space character";
		}
		else if (!newPwd.equals(confirmPwd))
		{
			msg = "The new password and the confirmation do not match";
		}
		if (msg != null) messagesArea.setText(msg + "\nPlease try again");
		return msg == null;
	}

	/**
	 * Creates a panel to enables changes some of the server settings.
	 * @return a <code>JPanel</code> instance.
	 */
	private JPanel createConfigurationPanel()
	{
		JPanel mainPanel = GuiUtils.createBoxPanel(BoxLayout.Y_AXIS);
		mainPanel.setBorder(BorderFactory.createTitledBorder("Server Configuration"));
		JPanel panel = new JPanel();

		JPanel btnPanel = createConfigBtnPanel();
		bundleSizeSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 10000, 1));
		JPanel bundleSizePanel = createConfigRowPanel(bundleSizeSpinner, "Task bundle size");
		
		Dimension d = new Dimension(250, 20);
		btnPanel.setPreferredSize(d);
		bundleSizePanel.setPreferredSize(d);

		SpringLayout layout = new SpringLayout();
		panel.setLayout(layout);
		panel.add(btnPanel);
		panel.add(bundleSizePanel);

		layout.putConstraint(SpringLayout.NORTH, btnPanel, 10, SpringLayout.NORTH, panel);
		layout.putConstraint(SpringLayout.NORTH, bundleSizePanel, 10, SpringLayout.SOUTH, btnPanel);
		layout.putConstraint(SpringLayout.WEST, btnPanel, 10, SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.WEST, bundleSizePanel, 10, SpringLayout.WEST, panel);
		layout.putConstraint(SpringLayout.EAST, panel, 10, SpringLayout.EAST, btnPanel);

		mainPanel.add(panel);
		
		return mainPanel;
	}

	/**
	 * Create a panel containing a component with a corresponding label in a row. 
	 * @param field the component whose value can be changed.
	 * @param labelText the label corresponding to the field.
	 * @return a <code>JPanel</code> instance.
	 */
	private JPanel createConfigRowPanel(JComponent field, String labelText)
	{
		JPanel panel = GuiUtils.createBoxPanel(BoxLayout.X_AXIS);
		JLabel label = new JLabel(labelText);
		label.setPreferredSize(new Dimension(120, 20));
		panel.add(Box.createHorizontalStrut(5));
		panel.add(label); 
		panel.add(Box.createHorizontalStrut(5));
		panel.add(field);
		panel.add(Box.createHorizontalGlue());
		panel.setPreferredSize(new Dimension(25, 250));
		return panel;
	}

	/**
	 * Create the button to send the request for changing the admin password.
	 * @return a <code>JPanel</code> instance.
	 */
	private JPanel createConfigBtnPanel()
	{
		JButton requestBtn = new JButton("Refresh settings");
		requestBtn.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				
				int n = statsHandler.getLatestStats().bundleSize;
				if (n > 0)
				{
					SpinnerNumberModel model = (SpinnerNumberModel) bundleSizeSpinner.getModel();
					model.setValue(n);
				}
			}
		});
		requestBtn.setPreferredSize(new Dimension(120, 20));
		JButton changeBtn = new JButton("Change settings");
		changeBtn.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				String pwd = new String(passwordField.getPassword());
				Map<String, Object> params = new HashMap<String, Object>();
				SpinnerNumberModel model = (SpinnerNumberModel) bundleSizeSpinner.getModel();
				params.put(AdminRequest.BUNDLE_SIZE_PARAM, model.getValue());
				String msg = statsHandler.changeSettings(pwd, params);
				if (msg != null) messagesArea.setText(msg);
			}
		});
		changeBtn.setPreferredSize(new Dimension(120, 20));
		JPanel btnPanel = GuiUtils.createBoxPanel(BoxLayout.X_AXIS);
		btnPanel.add(Box.createHorizontalStrut(5));
		btnPanel.add(requestBtn);
		btnPanel.add(Box.createHorizontalStrut(5));
		btnPanel.add(changeBtn);
		btnPanel.add(Box.createHorizontalGlue());
		btnPanel.setPreferredSize(new Dimension(25, 300));
		return btnPanel;
	}
}
