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
package org.jppf.ui.monitoring.node.actions;

import java.awt.event.*;
import java.util.List;

import javax.swing.*;

import org.jppf.management.*;
import org.jppf.ui.monitoring.node.*;
import org.jppf.utils.StringUtils;

/**
 * This action displays the driver or node environment information in a separate frame.
 */
public class SystemInformationAction extends AbstractTopologyAction
{
	/**
	 * Initialize this action.
	 */
	public SystemInformationAction()
	{
		setupIcon("/org/jppf/ui/resources/info.gif");
		setupNameAndTooltip("show.information");
	}

	/**
	 * Update this action's enabled state based on a list of selected elements.
	 * @param selectedElements a list of objects.
	 * @see org.jppf.ui.actions.AbstractUpdatableAction#updateState(java.util.List)
	 */
	@Override
	public void updateState(final List<Object> selectedElements)
	{
		this.selectedElements = selectedElements;
		dataArray = new TopologyData[selectedElements.size()];
		int count = 0;
		for (Object o: selectedElements) dataArray[count++] = (TopologyData) o;
		setEnabled(dataArray.length > 0);
	}

	/**
	 * Perform the action.
	 * @param event not used.
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(final ActionEvent event)
	{
		String s = null;
		try
		{
			JMXConnectionWrapper connection = dataArray[0].getJmxWrapper();
			JPPFSystemInformation info = connection.systemInformation();
			boolean isNode = dataArray[0].getType().equals(TopologyDataType.NODE);
			PropertiesTableFormat format = new HTMLPropertiesTableFormat("information for " + (isNode ? "node " : "driver ") + connection.getId());
			format.start();
			format.formatTable(info.getUuid(), "UUID");
			format.formatTable(info.getSystem(), "System Properties");
			format.formatTable(info.getEnv(), "Environment Variables");
			format.formatTable(info.getRuntime(), "Runtime Information");
			format.formatTable(info.getJppf(), "JPPF configuration");
			format.formatTable(info.getNetwork(), "Network configuration");
			format.formatTable(info.getStorage(), "Storage Information");
			format.end();
			s = format.getText();
		}
		catch(Exception e)
		{
			s = StringUtils.getStackTrace(e).replace("\n", "<br>");
		}
		final JFrame frame = new JFrame("System Information");
		frame.setIconImage(((ImageIcon) getValue(SMALL_ICON)).getImage());
		frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(final WindowEvent e)
			{
				frame.dispose();
			}
		});
		JEditorPane textArea = new JEditorPane("text/html", s);
		AbstractButton btn = (AbstractButton) event.getSource();
		if (btn.isShowing()) location = btn.getLocationOnScreen();
		textArea.setEditable(false);
		textArea.setOpaque(false);
		frame.getContentPane().add(new JScrollPane(textArea));
		frame.setLocationRelativeTo(null);
		frame.setLocation(location);
		frame.setSize(400, 400);
		frame.setVisible(true);
	}
}
