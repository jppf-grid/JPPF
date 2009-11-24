/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

package org.jppf.ui.utils;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Window;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.jppf.utils.JPPFConfiguration;

import net.miginfocom.swing.MigLayout;

/**
 * 
 * @author Laurent Cohen
 */
public class JPPFSplash extends Window
{
	/**
	 * Contains the images displayed by the splash screen.
	 */
	private ImageIcon[] images = null;
	/**
	 * Component used to display the text and images.
	 */
	private JLabel label = null;
	/**
	 * The timer task that scrolls through images.
	 */
	private ScrollTask task = null;
	/**
	 * The timer that runs the task.
	 */
	private Timer timer = null;
	/**
	 * Delay between images scrolling.
	 */
	private long delay = JPPFConfiguration.getProperties().getLong("jppf.ui.splash.delay", 500L);

	/**
	 * Initialize this window with the specified owner.
	 * @param message - the message to display.
	 */
	public JPPFSplash(String message)
	{
		super(new JFrame());
		images = new ImageIcon[4];
		for (int i=1; i<=4; i++) images[i-1] = GuiUtils.loadIcon("/org/jppf/ui/resources/splash" + i + ".gif", false);
		label = new JLabel(images[0]);
		label.setHorizontalTextPosition(SwingConstants.CENTER);
		label.setVerticalTextPosition(SwingConstants.CENTER);
		label.setText(message);
		label.setForeground(new Color(64, 64, 128));
		Font tmp = label.getFont();
		label.setFont(new Font(tmp.getFamily(), Font.BOLD, 24));
		setLayout(new MigLayout("fill, ins 0 0 0 0"));
		add(label, "grow, push");
		pack();
		getOwner().setVisible(true);
	}

	/**
	 * Start the animation of this splash screen.
	 */
	public void start()
	{
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension d2 = label.getSize();
		int x = (d.width - d2.width) / 2;
		int y = (d.height - d2.height) / 2;
		setLocation(x, y);
		task = new ScrollTask();
		setVisible(true);
		timer = new Timer();
		timer.schedule(task, delay, delay);
	}

	/**
	 * Stop the animation of this splash screen.
	 */
	public void stop()
	{
		Runnable r = new Runnable()
		{
			public void run()
			{
				try { Thread.sleep(2000); }
				catch(InterruptedException e) {}
				setVisible(false);
				task.cancel();
				timer.purge();
				dispose();
				getOwner().setVisible(true);
				getOwner().dispose();
			}
		};
		new Thread(r).start();
	}

	/**
	 * Task that scrolls the images at regular intervals.
	 */
	public class ScrollTask extends TimerTask
	{
		/**
		 * Position in the array of images.
		 */
		private int pos = 0;

		/**
		 * Execute this task.
		 * @see java.util.TimerTask#run()
		 */
		public void run()
		{
			pos = (pos + 1) % images.length;
			label.setIcon(images[pos]);
		}
	}
}
