/*
 * JPPF.
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
package org.jppf.ui.monitoring.node.actions;

import java.awt.Point;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;

import org.apache.commons.logging.*;
import org.jppf.management.JMXNodeConnectionWrapper;
import org.jppf.ui.options.*;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.ui.utils.GuiUtils;
import org.jppf.utils.TypedProperties;

/**
 * This action displays an input panel for the user to type a new
 * thread pool size for a node, and updates the node with it.
 */
public class NodeConfigurationAction extends AbstractTopologyAction
{
	/**
	 * Logger for this class.
	 */
	protected static Log log = LogFactory.getLog(NodeConfigurationAction.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	protected static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Determines whether the "OK" button was pressed.
	 */
	private boolean isOk = false;
	/**
	 * Panel containing the dialog for entering the number of threads and their priority.
	 */
	private OptionElement panel = null;
	/**
	 * Location at which to display the entry dialog. 
	 */
	private Point location = null;

	/**
	 * Initialize this action.
	 */
	public NodeConfigurationAction()
	{
		setupIcon("/org/jppf/ui/resources/update.gif");
		putValue(NAME, "Update the configuration properties");
	}

	/**
	 * Update this action's enabled state based on a list of selected elements.
	 * @param selectedElements - a list of objects.
	 * @see org.jppf.ui.actions.AbstractUpdatableAction#updateState(java.util.List)
	 */
	public void updateState(List<Object> selectedElements)
	{
		super.updateState(selectedElements);
		setEnabled(nodeDataArray.length > 0);
	}

	/**
	 * Perform the action.
	 * @param event - not used.
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent event)
	{
		try
		{
			panel = OptionsHandler.loadPageFromXml("org/jppf/ui/options/xml/JPPFConfigurationPanel.xml");
			TextAreaOption textArea = (TextAreaOption) panel.findFirstWithName("configProperties");
			textArea.setValue(getPropertiesAsString());

			JButton okBtn = (JButton) panel.findFirstWithName("/nodeThreadsOK").getUIComponent();
			JButton cancelBtn = (JButton) panel.findFirstWithName("/nodeThreadsCancel").getUIComponent();
			final JFrame frame = new JFrame("Update the JPPF configuration");
			frame.setIconImage(GuiUtils.loadIcon("/org/jppf/ui/resources/update.gif").getImage());
			okBtn.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent event)
				{
					frame.setVisible(false);
					frame.dispose();
					doOK();
				}
			});
			cancelBtn.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent event)
				{
					frame.setVisible(false);
					frame.dispose();
				}
			});
			frame.getContentPane().add(panel.getUIComponent());
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setLocation(location);
			frame.setVisible(true);
		}
		catch(Exception e)
		{
			if (debugEnabled) log.debug(e.getMessage(), e);
		}
	}

	/**
	 * Perform the action.
	 */
	private void doOK()
	{
		TextAreaOption textArea = (TextAreaOption) panel.findFirstWithName("configProperties");
		final Map<String, String> map = getPropertiesAsMap((String) textArea.getValue());
		final Boolean b = (Boolean) ((BooleanOption) panel.findFirstWithName("forceReconnect")).getValue();
		Runnable r = new Runnable()
		{
			public void run()
			{
				try
				{
					((JMXNodeConnectionWrapper) nodeDataArray[0].getJmxWrapper()).updateConfiguration(map, b);
				}
				catch(Exception e)
				{
					log.error(e.getMessage(), e);
				}
			}
		};
		new Thread(r).start();
	}

	/**
	 * Obtain the JPPF ocnfiguration as a string, one property per line.
	 * @return the properties as a string.
	 * @throws Exception if an error occurs whileattempting to obtain the JPPF properties.
	 */
	private String getPropertiesAsString() throws Exception
	{
		StringBuilder sb = new StringBuilder();
		TypedProperties props = ((JMXNodeConnectionWrapper) nodeDataArray[0].getJmxWrapper()).systemInformation().getJppf();
		Set<String> keys = new TreeSet<String>();
		for (Object o: props.keySet()) keys.add((String) o);
		for (String s: keys) sb.append(s).append(" = ").append(props.get(s)).append("\n");
		return sb.toString();
	}

	/**
	 * Get the properties defined in the text area as a map.
	 * @param source - the text from which to read the properties.
	 * @return a map of string keys to string values.
	 */
	private Map<String, String> getPropertiesAsMap(String source)
	{
		try
		{
			Map<String, String> map = new HashMap<String, String>();
			BufferedReader reader = new BufferedReader(new StringReader(source));
			while (true)
			{
				String s = reader.readLine();
				if (s == null) break;
				int idx = s.indexOf('=');
				if (idx < 0) idx = s.indexOf(' ');
				if (idx < 0) continue;
				String key = s.substring(0, idx).trim();
				String value = s.substring(idx+1).trim();
				map.put(key, value);
			}
			reader.close();
			return map;
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
		return null;
	}
}
