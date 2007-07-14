/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jppf.node.screensaver;

import java.awt.*;
import java.io.*;
import java.text.NumberFormat;

import javax.swing.*;

import org.jppf.utils.*;

/**
 * This class enables launching a JPPF node as an applet, from a web browser.
 * @author Laurent Cohen
 */
public class NodePanel extends JPanel
{
	/**
	 * Path to the images to display in the UI.
	 */
	public static final String IMAGE_PATH = "/org/jppf/node";
	/**
	 * Image dispalying a bright green traffic light.
	 */
	static final ImageIcon BRIGHT_GREEN = loadImage(IMAGE_PATH + "/" + "active_greenlight.gif");
	/**
	 * Image dispalying a dark green traffic light.
	 */
	static final ImageIcon DARK_GREEN = loadImage(IMAGE_PATH + "/" + "inactive_greenlight.gif");
	/**
	 * Image dispalying a bright red traffic light.
	 */
	static final ImageIcon BRIGHT_RED = loadImage(IMAGE_PATH + "/" + "active_redlight.gif");
	/**
	 * Image dispalying a dark red traffic light.
	 */
	static final ImageIcon DARK_RED = loadImage(IMAGE_PATH + "/" + "inactive_redlight.gif");
	/**
	 * Holds the states of all nodes.
	 */
	public NodeState nodeState = null;
	/**
	 * 
	 */
	private boolean activate = true;

	/**
	 * Initialize this UI.
	 * @param activate .
	 */
	public NodePanel(boolean activate)
	{
		this.activate = activate;
		init();
	}

	/**
	 * Initialize this applet.
	 * This method get the applet parameters for the JPPF config file and the log4j config file,
	 * then creates the UI components, then starts the node in a separate thred, so that the
	 * applet is not stuck in the <code>init()</code> method.
	 * @see java.applet.Applet#init()
	 */
	public void init()
	{
		try
		{
			String log4jCfg = "log4j-node.properties";
			System.setProperty("log4j.configuration", log4jCfg);
			//Log4jInitializer.configureFromClasspath(log4jCfg);
			TypedProperties props = JPPFConfiguration.getProperties();
			props.remove("jppf.policy.file"); 
			createUI();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Initialize the user interface for this applet.
	 */
	private void createUI()
	{
		GridBagLayout g = new GridBagLayout();
		setLayout(g);
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
		setBackground(Color.BLACK);
		ImageIcon logo = loadImage(IMAGE_PATH + "/" + "jppf-at-home.gif");
		JLabel logoLabel = new JLabel(logo);
		addLayoutComp(this, g, c, logoLabel);
		addLayoutComp(this, g, c, Box.createVerticalStrut(10));
		addLayoutComp(this, g, c, createNodePanel());
		addLayoutComp(this, g, c, Box.createVerticalStrut(5));
		if (activate) nodeState.startNode();
	}

	/**
	 * Create a panel showing the activity of a node.
	 * @return a panel with some node information about is activity. 
	 */
	private JPanel createNodePanel()
	{
		JPanel panel = new JPanel();
		GridBagLayout g = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    c.gridy = 0;
		panel.setLayout(g);
		panel.setBackground(Color.BLACK);
		nodeState = new NodeState();

		addLayoutComp(panel, g, c, Box.createHorizontalStrut(25));
		JLabel label = new JLabel("JPPF Node");
		label.setBackground(Color.BLACK);
		label.setForeground(Color.WHITE);
		addLayoutComp(panel, g, c, label);
		addLayoutComp(panel, g, c, Box.createHorizontalStrut(50));
		addLayoutComp(panel, g, c, nodeState.timeLabel);
		addLayoutComp(panel, g, c, Box.createHorizontalStrut(25));
		addLayoutComp(panel, g, c, makeStatusPanel(0, "connection"));
		addLayoutComp(panel, g, c, Box.createHorizontalStrut(15));
		addLayoutComp(panel, g, c, makeStatusPanel(1, "execution"));
		addLayoutComp(panel, g, c, Box.createHorizontalStrut(15));
		label = new JLabel("tasks");
		label.setBackground(Color.BLACK);
		label.setForeground(Color.WHITE);
		addLayoutComp(panel, g, c, label);
		panel.add(Box.createHorizontalStrut(5));
		nodeState.countLabel.setPreferredSize(new Dimension(60, 20));
		addLayoutComp(panel, g, c, nodeState.countLabel);

		return panel;
	}
	
	/**
	 * Add a component to a panel with the specified constaints.
	 * @param panel the panel to add the component to.
	 * @param g the <code>GridBagLayout</code> set on the panel.
	 * @param c the constraints to apply to the component.
	 * @param comp the component to add.
	 */
	private void addLayoutComp(JPanel panel, GridBagLayout g, GridBagConstraints c, Component comp)
	{
		g.setConstraints(comp, c);
    panel.add(comp);
	}
	
	/**
	 * Generate a panel display the status of the connection or execution.
	 * @param statusIdx index of the status to display: 0 for connection, 1 for execution.
	 * @param text the text to display on the left of the status lights.
	 * @return a <code>JPanel</code> instance.
	 */
	private JPanel makeStatusPanel(int statusIdx, String text)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.setBackground(Color.BLACK);

		JLabel label = new JLabel(text);
		label.setBackground(Color.BLACK);
		label.setForeground(Color.WHITE);
		JPanel labelPanel = new JPanel();
		labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.X_AXIS));
		labelPanel.add(label);
		labelPanel.setBackground(Color.BLACK);

		JPanel statusPanel = new JPanel();
		statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
		statusPanel.setBackground(Color.BLACK);
		statusPanel.add(nodeState.statusLabels[statusIdx][0]);
		statusPanel.add(Box.createVerticalStrut(4));
		statusPanel.add(nodeState.statusLabels[statusIdx][1]);

		labelPanel.setPreferredSize(new Dimension(60, 20));
		panel.add(labelPanel);
		panel.add(Box.createHorizontalStrut(5));
		statusPanel.setPreferredSize(new Dimension(8, 20));
		panel.add(statusPanel);
		panel.setPreferredSize(new Dimension(73, 20));
		return panel;
	}

	/**
	 * Load an icon from the specified path.
	 * @param file the file to get the icon from.
	 * @return an <code>ImageIcon</code> instance.
	 */
	public static ImageIcon loadImage(String file)
	{
		int MAX_IMAGE_SIZE = 15000;
		int count = 0;
		InputStream is = NodePanel.class.getResourceAsStream(file);
		if (is == null)
		{
			System.err.println("Couldn't find file: " + file);
			return null;
		}
		BufferedInputStream bis = new BufferedInputStream(is);
		byte buf[] = new byte[MAX_IMAGE_SIZE];
		try
		{
			count = bis.read(buf);
			bis.close();
		}
		catch (IOException ioe)
		{
			System.err.println("Couldn't read stream from file: " + file);
			ioe.printStackTrace();
			return null;
		}
		if (count <= 0)
		{
			System.err.println("Empty file: " + file);
			return null;
		}
		return new ImageIcon(Toolkit.getDefaultToolkit().createImage(buf));
	}
	
	/**
	 * Free resources used by the nodes.
	 */
	public void cleanup()
	{
		try
		{
			if (activate) nodeState.stopNode();
		}
		catch (Throwable t)
		{
		}
	}
	
	/**
	 * Used to nicely format integer values.
	 */
	private static NumberFormat integerFormatter = null;
	/**
	 * Tranform a duration in milliseconds into a string with hours, minutes, seconds and milliseconds..
	 * @param elapsed the duration to transform, expressed in milliseconds.
	 * @return a string specifiying the duration in terms of hours, minutes, seconds and milliseconds.
	 */
	public static String toStringDuration(long elapsed)
	{
		if (integerFormatter == null)
		{
			integerFormatter = NumberFormat.getInstance();
			integerFormatter.setGroupingUsed(false);
			integerFormatter.setMinimumFractionDigits(0);
			integerFormatter.setMaximumFractionDigits(0);
			integerFormatter.setMinimumIntegerDigits(2);
		}

		StringBuilder sb = new StringBuilder();
		sb.append(integerFormatter.format(elapsed / 3600000L)).append(" hrs ");
		elapsed = elapsed % 3600000L;
		sb.append(integerFormatter.format(elapsed / 60000L)).append(" mn ");
		elapsed = elapsed % 60000L;
		sb.append(integerFormatter.format(elapsed / 1000L));
		sb.append(" sec");
		return sb.toString();
	}
}
