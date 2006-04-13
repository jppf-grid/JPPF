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
package org.jppf.node;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import org.jppf.node.event.*;
import org.jppf.utils.*;

/**
 * This class enables launching a JPPF node as an applet, from a web browser.
 * @author Laurent Cohen
 */
public class NodeApplet extends JApplet
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 7299446296348222737L;
	/**
	 * Path to the images to display in the UI.
	 */
	private static final String IMAGE_PATH = "/org/jppf/node";
	/**
	 * Image dispalying a bright green traffic light.
	 */
	private static final ImageIcon BRIGHT_GREEN = loadImage("active_greenlight.gif");
	/**
	 * Image dispalying a dark green traffic light.
	 */
	private static final ImageIcon DARK_GREEN = loadImage("inactive_greenlight.gif");
	/**
	 * Image dispalying a bright red traffic light.
	 */
	private static final ImageIcon BRIGHT_RED = loadImage("active_redlight.gif");
	/**
	 * Image dispalying a dark red traffic light.
	 */
	private static final ImageIcon DARK_RED = loadImage("inactive_redlight.gif");
	/**
	 * Path to the images to display in the UI.
	 */
	private static final int MAX_NODES = 2;
	/**
	 * Holds the states of all nodes.
	 */
	private NodeState[] nodeState = new NodeState[MAX_NODES];

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
			String cfg = getParameter(JPPFConfiguration.CONFIG_PROPERTY);
			System.out.println("JPPF configuration file: "+cfg);
			//System.setProperty(JPPFConfiguration.CONFIG_PROPERTY, cfg);
			System.setProperty(JPPFConfiguration.CONFIG_PROPERTY, "jppf-node.properties");
			String log4jCfg = getParameter("log4j.configuration");
			System.setProperty("log4j.configuration", log4jCfg);
			Log4jInitializer.configureFromClasspath(log4jCfg);
			SwingUtilities.invokeAndWait(new Runnable()
			{
				public void run()
				{
					createUI();
				}
			});
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
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		for (int i=0; i<MAX_NODES; i++) panel.add(createNodePanel(i));
		getContentPane().add(panel);
	}

	/**
	 * Create a panel showing the activity of a node.
	 * @param n the unique index of the node.
	 * @return a panel with some node information about is activity. 
	 */
	private JPanel createNodePanel(int n)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		nodeState[n] = new NodeState();
		panel.add(new JLabel("Node " + (n+1)));
		panel.add(Box.createHorizontalStrut(5));

		JPanel tmpPanel = new JPanel();
		tmpPanel.setLayout(new BoxLayout(tmpPanel, BoxLayout.Y_AXIS));
		tmpPanel.add(nodeState[n].statusLabels[0][0]);
		panel.add(Box.createVerticalStrut(4));
		tmpPanel.add(nodeState[n].statusLabels[0][1]);
		panel.add(tmpPanel);
		panel.add(Box.createHorizontalStrut(5));

		tmpPanel = new JPanel();
		tmpPanel.setLayout(new BoxLayout(tmpPanel, BoxLayout.Y_AXIS));
		tmpPanel.add(nodeState[n].statusLabels[1][0]);
		panel.add(Box.createVerticalStrut(4));
		tmpPanel.add(nodeState[n].statusLabels[1][1]);
		panel.add(tmpPanel);
		panel.add(Box.createHorizontalStrut(5));

		panel.add(new JLabel("tasks"));
		panel.add(Box.createHorizontalStrut(5));
		panel.add(nodeState[n].countLabel);
		panel.add(Box.createHorizontalStrut(5));
		panel.add(nodeState[n].btn[0]);
		panel.add(nodeState[n].btn[1]);
		panel.add(Box.createHorizontalGlue());

		return panel;
	}

	/**
	 * Load an icon from the specified path.
	 * @param file the file to get the icon from.
	 * @return an <code>ImageIcon</code> instance.
	 */
	protected static ImageIcon loadImage(String file)
	{
		String path = IMAGE_PATH + "/" + file;
		int MAX_IMAGE_SIZE = 2400;
		int count = 0;
		InputStream is = NodeApplet.class.getResourceAsStream(path);
		if (is == null)
		{
			System.err.println("Couldn't find file: " + path);
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
			System.err.println("Couldn't read stream from file: " + path);
			ioe.printStackTrace();
			return null;
		}
		if (count <= 0)
		{
			System.err.println("Empty file: " + path);
			return null;
		}
		return new ImageIcon(Toolkit.getDefaultToolkit().createImage(buf));
	}
	
	/**
	 * Instances of this class represent information about a node.
	 */
	public class NodeState implements NodeListener
	{
		/**
		 * Contains the threads in which the nodes run.
		 */
		public NodeThread nodeThread = null;
		/**
		 * Number of tasks executed by the node.
		 */
		public int taskCount = 0;
		/**
		 * Holds the statuses for the node connection and tasks execution.
		 */
		public boolean[][] status = new boolean[2][2];
		/**
		 * These labels contain the status icons for the nodes connection and task execution activity.
		 * Each status is represented by a green light and a red light, each light dark or bright depending on the node status.
		 */
		public JLabel[][] statusLabels = new JLabel[2][2];
		/**
		 * Labels uswed to display the number of tasks executed by each node.
		 */
		public JLabel countLabel = null;
		/**
		 * Buttons used to start and stop the node.
		 */
		public JButton[] btn = new JButton[2];
		/**
		 * Determine whether the node has already been started at least once.
		 */
		boolean startedOnce = false;
		
		/**
		 * Initialize this node state.
		 */
		public NodeState()
		{
			for (int i=0; i<statusLabels.length; i++)
			{
				statusLabels[i][0] = new JLabel(DARK_GREEN);
				statusLabels[i][1] = new JLabel(DARK_RED);
			}
			Dimension d = new Dimension(8, 8);
			for (int i=0; i<statusLabels.length; i++)
			{
				for (int j=0; j<statusLabels[i].length; j++)
				{
					statusLabels[i][j].setMinimumSize(d);
					statusLabels[i][j].setMaximumSize(d);
				}
			}
			countLabel = new JLabel(""+taskCount);
			d = new Dimension(40, 20);
			countLabel.setMinimumSize(d);
			countLabel.setMaximumSize(d);
			nodeThread = new NodeThread(this);
			btn[0] = new JButton("Start");
			btn[0].addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent event)
				{
					btn[0].setEnabled(false);
					btn[1].setEnabled(true);
					if (!startedOnce)
					{
						startedOnce = true;
						nodeThread.start();
					}
					else nodeThread.startNode();
				}
			});

			btn[1] = new JButton("Stop");
			btn[1].addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent event)
				{
					btn[0].setEnabled(true);
					btn[1].setEnabled(false);
					nodeThread.stopNode();
				}
			});
			btn[1].setEnabled(false);
		}

		/**
		 * Called when the underlying node sends an event notification.
		 * @param event the event that triggered the call to this method.
		 * @see org.jppf.node.event.NodeListener#eventOccurred(org.jppf.node.event.NodeEvent)
		 */
		public void eventOccurred(NodeEvent event)
		{
			String type = event.getType();
			if (NodeEvent.START_CONNECT.equals(type))
			{
				statusLabels[0][0].setIcon(DARK_GREEN);
				statusLabels[0][1].setIcon(BRIGHT_RED);
			}
			else if (NodeEvent.END_CONNECT.equals(type))
			{
				statusLabels[0][0].setIcon(BRIGHT_GREEN);
				statusLabels[0][1].setIcon(DARK_RED);
			}
			else if (NodeEvent.DISCONNECTED.equals(type))
			{
				statusLabels[0][0].setIcon(DARK_GREEN);
				statusLabels[0][1].setIcon(DARK_RED);
				statusLabels[1][0].setIcon(DARK_GREEN);
				statusLabels[1][1].setIcon(DARK_RED);
			}
			else if (NodeEvent.START_EXEC.equals(type))
			{
				statusLabels[1][0].setIcon(DARK_GREEN);
				statusLabels[1][1].setIcon(BRIGHT_RED);
			}
			else if (NodeEvent.END_EXEC.equals(type))
			{
				statusLabels[1][0].setIcon(BRIGHT_GREEN);
				statusLabels[1][1].setIcon(DARK_RED);
				taskCount++;
				countLabel.setText(""+taskCount);
			}
		}
	}
}
