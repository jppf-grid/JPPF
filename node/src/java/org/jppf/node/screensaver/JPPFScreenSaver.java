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
import java.awt.event.*;
import java.util.*;
import java.util.Timer;
import javax.swing.*;
import org.jdesktop.jdic.screensaver.*;
import org.jppf.node.NodePanel;
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
	 * Flag to determine whether to handle collisions between logos.
	 */
	private boolean collisions = false;
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
	private Timer timer = null;
	/**
	 * Width of the logo.
	 */
	int imgw = 0;
	/**
	 * Height of the logo.
	 */
	int imgh = 0;
	/**
	 * The image object for the flying logos.
	 */
	private Image logoImg = null;

	/**
	 * Default constructor.
	 */
	public JPPFScreenSaver()
	{
	}
	
	/**
	 * Initialize the UI components.
	 * @see org.jdesktop.jdic.screensaver.ScreensaverBase#init()
	 */
	public void init()
	{
		try
		{
			try
			{
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				for (Frame frame: Frame.getFrames())
				{
					SwingUtilities.updateComponentTreeUI(frame);
					frame.addWindowListener(new WindowAdapter()
					{
						public void windowClosing(WindowEvent e)
						{
							destroy();
							System.exit(0);
						}
					});
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			initializeSettings();
			data = new ImageData[nbLogos];
			for (int i=0; i<nbLogos; i++) data[i] = new ImageData();
			parent = (Container) getContext().getComponent();
			parent.setBackground(Color.BLACK);
			if (node == null) node = new NodePanel();
			node.setDoubleBuffered(true);
			parent.add(node);
			initializeFlyingLogos();
	
			Dimension dim = parent.getSize();
			Random rand = new Random(System.currentTimeMillis());
			for (int i=0; i<nbLogos; i++)
			{
				int n = dim.width - imgw;
				if (n <= 0) n = imgw;
				data[i].x = rand.nextInt(n);
				data[i].prevx = data[i].x;
				data[i].stepX *= 2 * rand.nextInt(2) - 1; 
				n = dim.height - imgh;
				if (n <= 0) n = imgh;
				data[i].y = rand.nextInt(n);
				data[i].prevy = data[i].y;
				data[i].stepY *= 2 * rand.nextInt(2) - 1; 
			}
			setDoubledBuffering(node);
			if (timer == null)
			{
				timer = new Timer();
				timer.schedule(new LogoUpdateTask(), 100, 25 + 5 * (11 - speed));
				// 25 frames/sec = 40ms/frame
				timer.schedule(new LogoDisplayTask(), 500, 33);
				TimerTask task = new TimerTask()
				{
					public void run()
					{
						
						String s = NodePanel.toStringDuration(System.currentTimeMillis() - node.nodeState.startedAt);
						node.nodeState.timeLabel.setText("Active for: "+s);
					}
				};
				timer.scheduleAtFixedRate(task, 1000, 1000);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Initialize the parameters of the screensaver.
	 */
	private void initializeSettings()
	{
		ScreensaverSettings settings = getContext().getSettings();
		System.setProperty(JPPFConfiguration.CONFIG_PROPERTY, "jppf-node.properties");

		TypedProperties props = JPPFConfiguration.getProperties();
		props.setProperty("jppf.server.host", settings.getProperty("host"));
		props.setProperty("class.server.port", settings.getProperty("classServerPort"));
		props.setProperty("node.server.port", settings.getProperty("nodeServerPort"));
		props.setProperty("processing.threads", settings.getProperty("nbThreads"));

		collisions = settings.getProperty("collisions") != null;
		nbLogos = getIntSetting("nbLogos", 10);
		speed = getIntSetting("speed", 5);
	}

	/**
	 * Read the logos image file and initialize the graphics objects
	 * required to render them.
	 */
	private void initializeFlyingLogos()
	{
		logo = NodePanel.loadImage(NodePanel.IMAGE_PATH + "/" + "logo-small.gif");
		logoImg = logo.getImage();
		imgw = logo.getIconWidth();
		imgh = logo.getIconHeight();
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
		if (node != null)
		{
			node.cleanup();
			parent.remove(node);
			node = null;
		}
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
	 * Timer task to display the logos at a rate of 25 frames/sec.
	 */
	public class LogoDisplayTask  extends TimerTask
	{
		/**
		 * The task that renders the flying logos.
		 */
		Runnable task = null;

		/**
		 * Initialize the task that renders the flying logos.
		 */
		public LogoDisplayTask()
		{
			task = new Runnable()
			{
				public void run()
				{
					updateLogos();
				}
			};
		}

		/**
		 * Update the position and direction of the flying logos.
		 * @see java.util.TimerTask#run()
		 */
		public void run()
		{
			SwingUtilities.invokeLater(task);
		}
	}

	/**
	 * Timer task to update the position and direction of the flying logos.
	 */
	private class LogoUpdateTask extends TimerTask
	{
		/**
		 * Update the position and direction of the flying logos.
		 * @see java.util.TimerTask#run()
		 */
		public void run()
		{
			Dimension dim = parent.getSize();
			for (int i=0; i<data.length; i++)
			{
				ImageData d = data[i];
				if (collisions)
				{
					for (int j=i+1; j<data.length; j++)
					{
						ImageData d2 = data[j];
						checkColliding(d, d2);
					}
				}
				synchronized(d)
				{
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
			}
		}

		/**
		 * Determine whether two logos are colliding.
		 * @param d1 the position and speed vector data for the first logo.
		 * @param d2 the position and speed vector data for the second logo.
		 * @return true if the two logos are colliding, false otherwise.
		 */
		public boolean checkColliding(ImageData d1, ImageData d2)
		{
			int x1 = d1.x + d1.stepX;
			int x2 = d2.x + d2.stepX;
			int y1 = d1.y + d1.stepY;
			int y2 = d2.y + d2.stepY;
			boolean b = false;
			if (isIn(x1, y1, x2, y2))
			{
				if (d1.x >= d2.x + imgw)
				{
					d1.stepX  = -d1.stepX;
					d2.stepX  = -d2.stepX;
				}
				if (d1.y >= d2.y + imgh)
				{
					d1.stepY  = -d1.stepY;
					d2.stepY  = -d2.stepY;
				}
				return true;
			}
			if (isIn(x1 + imgw, y1, x2, y2))
			{
				if (d1.x + imgw <= d2.x)
				{
					d1.stepX  = -d1.stepX;
					d2.stepX  = -d2.stepX;
				}
				if (d1.y >= d2.y + imgh)
				{
					d1.stepY  = -d1.stepY;
					d2.stepY  = -d2.stepY;
				}
				return true;
			}
			if (isIn(x1, y1 + imgh, x2, y2))
			{
				if (d1.x >= d2.x + imgw)
				{
					d1.stepX  = -d1.stepX;
					d2.stepX  = -d2.stepX;
				}
				if (d1.y + imgh <= d2.y)
				{
					d1.stepY  = -d1.stepY;
					d2.stepY  = -d2.stepY;
				}
				return true;
			}
			if (isIn(x1 + imgw, y1 + imgh, x2, y2))
			{
				if (d1.x + imgw <= d2.x)
				{
					d1.stepX  = -d1.stepX;
					d2.stepX  = -d2.stepX;
				}
				if (d1.y + imgh <= d2.y)
				{
					d1.stepY  = -d1.stepY;
					d2.stepY  = -d2.stepY;
				}
				return true;
			}
			return false;
		}

		/**
		 * .
		 * @param x1 .
		 * @param y1 .
		 * @param x2 .
		 * @param y2 .
		 * @return .
		 */
		public boolean isIn(int x1, int y1, int x2, int y2)
		{
			return ((x1 >= x2) && (x1 <= x2 + imgw)) && ((y1 >= y2) && (y1 <= y2 + imgh));
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
			synchronized(d)
			{
				g.setClip(d.prevx, d.prevy, imgw, imgh);
				parent.paint(g);
				node.paint(g);
				g.drawImage(logoImg, d.x, d.y, node);
				d.prevx = d.x;
				d.prevy = d.y;
			}
		}
		g.setClip(clip);
	}
}
