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
import java.util.*;
import java.util.Timer;

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
	 * The icon holding the flying logo image.
	 */
	private ImageIcon logo = null;
	/**
	 * The number of flying logos;
	 */
	private int nbLogos = 10;
	/**
	 * The speed of the flying logos;
	 */
	private int speed = 10;
	/**
	 * Array of ImageData instances holding the position and speed of the logos.
	 */
	private ImageData[] data = null;
	/**
	 * The main UI component.
	 */
	private Container parent = null;
	/**
	 * Timer used to update the position of the flying logos at regular intervals.
	 */
	private Timer timer = new Timer();
	/**
	 * Width of the logo.
	 */
	int imgw = 0;
	/**
	 * Height of the logo.
	 */
	int imgh = 0;
	
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

		int n = getIntSetting("nbThreads", 1);
		props.put("processing.threads", ""+n);

		nbLogos = getIntSetting("nbLogos", 10);
		if (nbLogos < 1) nbLogos = 1;
		if (nbLogos > 10) nbLogos = 10;

		speed = getIntSetting("speed", 10);
		if (speed < 1) speed = 1;
		if (speed > 10) speed = 10;

		data = new ImageData[nbLogos];
		for (int i=0; i<nbLogos; i++)
		{
			data[i] = new ImageData();
		}
		parent = (Container) getContext().getComponent();
		logo = NodePanel.loadImage("logo-small.gif");
		imgw = logo.getIconWidth();
		imgh = logo.getIconHeight();

		//parent.setDoubleBuffered(true);
		Dimension dim = parent.getSize();
		Random rand = new Random(System.currentTimeMillis());
		for (int i=0; i<nbLogos; i++)
		{
			data[i].x = rand.nextInt(dim.width - imgw);
			data[i].stepX *= 2 * rand.nextInt(2) - 1; 
			data[i].y = rand.nextInt(dim.height - imgh);
			data[i].stepY *= 2 * rand.nextInt(2) - 1; 
		}
		parent.setBackground(Color.BLACK);
		node = new NodePanel();
		node.setDoubleBuffered(true);
		parent.add(node);
		setDoubledBuffering(node);
		timer.schedule(new LogoTask(), 500, 10 * (11 - speed));
	}
	
	/**
	 * Set a hierarchy of Swing components as double buffered.
	 * @param comp the root of the components hierarchy.
	 */
	private void setDoubledBuffering(JComponent comp)
	{
		comp.setDoubleBuffered(true);
		for (int i=0; i<comp.getComponentCount(); i++)
		{
			Component c = comp.getComponent(i);
			if (c instanceof JComponent) setDoubledBuffering((JComponent) c);
		}
	}
	
	/**
	 * Get a screensaver setting as an int value.
	 * @param name the name of the setting.
	 * @param defValue the default value to use if the setting is not defined.
	 * @return the setting as an int value.
	 */
	private int getIntSetting(String name, int defValue)
	{
		int result = defValue;
		try
		{
			ScreensaverSettings settings = getContext().getSettings();
			String s = settings.getProperty(name);
			result = Integer.parseInt(s);
		}
		catch(NumberFormatException e)
		{
		}
		return result;
	}
	
	/**
	 * Called at regular intervals to render the next frame in the screen saver. 
	 * @param g the graphics on which to paint the frame.
	 * @see org.jdesktop.jdic.screensaver.SimpleScreensaver#paint(java.awt.Graphics)
	 */
	public void paint(Graphics g)
	{
	}

	/**
	 * Invoked whne the screen saver terminates, to free the resources used by the node.
	 * @see org.jdesktop.jdic.screensaver.ScreensaverBase#destroy()
	 */
	protected void destroy()
	{
		timer.cancel();
		node.cleanup();
	}
	
	/**
	 * Data structure holding the position and direction of a flying logo.
	 */
	private class ImageData
	{
		/**
		 * The previous position on the x axis.
		 */
		public int prevx = 0;
		/**
		 * The previous position on the y axis.
		 */
		public int prevy = 0;
		/**
		 * The position on the x axis.
		 */
		public int x = 0;
		/**
		 * The position on the y axis.
		 */
		public int y = 0;
		/**
		 * The direction on the x axis.
		 */
		public int stepX = 1;
		/**
		 * The direction on the y axis.
		 */
		public int stepY = 1;
	}
	
	/**
	 * Timer task to update the position and direction of the flying logos.
	 */
	private class LogoTask extends TimerTask
	{
		/**
		 * Update the position and direction of the flying logos.
		 * @see java.util.TimerTask#run()
		 */
		public void run()
		{
			Dimension dim = parent.getSize();
			for (ImageData d: data)
			{
				d.prevx = d.x;
				d.prevy = d.y;
				if ((d.x + d.stepX < 0) || (d.x + d.stepX + imgw > dim.width))
				{
					d.stepX = -d.stepX;
				}
				if ((d.y + d.stepY < 0) || (d.y + d.stepY + imgh > dim.height))
				{
					d.stepY = -d.stepY;
				}
				d.x += d.stepX;
				d.y += d.stepY;
			}
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					updateLogos();
				}
			});
		}
	}

	/**
	 * Peforms the repainting of the flying logo images, as well as that of the areas they were
	 * occupying within the underlying components.
	 */
	public void updateLogos()
	{
		Graphics g = parent.getGraphics();
		Shape clip = g.getClip();
		for (ImageData d: data)
		{
			int x1 = Math.min(d.x, d.prevx); 
			int y1 = Math.min(d.y, d.prevy);
			Rectangle r = new Rectangle(x1, y1, imgw + 2, imgh + 2);
			g.setClip(r);
			parent.paint(g);
			node.paint(g);
			g.setClip(clip);
			g.drawImage(logo.getImage(), d.x, d.y, node);
		}
	}
}
