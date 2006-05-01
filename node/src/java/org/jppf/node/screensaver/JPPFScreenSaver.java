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
package org.jppf.node.screensaver;

import java.awt.*;

import javax.swing.*;

import org.jdesktop.jdic.screensaver.*;
import org.jppf.node.*;
import org.jppf.utils.*;

/**
 * A panel that serves as a GUI on top of a JPPF node, displayed as a screen saver.
 * @author Laurent Cohen
 */
public class JPPFScreenSaver extends SimpleScreensaver
{
	/**
	 * The node UI used in the screen saver.
	 */
	private NodePanel node = null; 

	/**
	 * Initialize the UI components.
	 * @see org.jdesktop.jdic.screensaver.ScreensaverBase#init()
	 */
	public void init()
	{
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			for (Frame frame: Frame.getFrames()) SwingUtilities.updateComponentTreeUI(frame);
		}
		catch(Exception e)
		{
		}
		ScreensaverSettings settings = getContext().getSettings();
		System.setProperty(JPPFConfiguration.CONFIG_PROPERTY, "jppf-node.properties");
		String s = settings.getProperty("host");
		if (s == null) s = "localhost";
		TypedProperties props = JPPFConfiguration.getProperties();
		props.put("jppf.server.host", s);
		s = settings.getProperty("nbThreads");
		int n = 1;
		try
		{
			n = Integer.parseInt(s);
			props.put("processing.threads", ""+n);
		}
		catch(NumberFormatException e)
		{
		}
		if (node == null)
		{
			Container c = (Container) getContext().getComponent();
			//c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
			c.setBackground(Color.BLACK);
			c.setForeground(Color.BLACK);
			node = new NodePanel();
			c.add(node);
		}
	}

	/**
	 * Called at regular intervals to render the next frame in the screen saver. 
	 * @param g the graphics on which to paint the frame.
	 * @see org.jdesktop.jdic.screensaver.SimpleScreensaver#paint(java.awt.Graphics)
	 */
	public void paint(Graphics g)
	{
		node.paintAll(g);
	}

	/**
	 * Invoked whne the screen saver terminates, to free the resources used by the node.
	 * @see org.jdesktop.jdic.screensaver.ScreensaverBase#destroy()
	 */
	protected void destroy()
	{
		node.cleanup();
	}
}
