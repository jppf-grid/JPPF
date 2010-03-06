/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

package sample.nbody;

import java.awt.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JPanel;

/**
 * 
 * @author Laurent Cohen
 */
public class NBodyPanel extends JPanel
{
	/**
	 * Foreground color for this panel.
	 */
	private static final Color FOREGROUND = Color.red;
	/**
	 * Background color for this panel.
	 */
	private static final Color BACKGROUND = Color.white;
	/**
	 * Thread pool used to generate paint requests.
	 */
	private ExecutorService executor = Executors.newFixedThreadPool(1);
	/**
	 * Used to synchronize access to the current positions array.
	 */
	private ReentrantLock lock = new ReentrantLock();
	/**
	 * The positions currently displayed.
	 */
	private Vector2d[] positions = null;
	/**
	 * The new positions to display.
	 */
	private Vector2d[] newPositions = null;
	/**
	 * determines wheher the display is being updated.
	 */
	private boolean updating = false;

	/**
	 * Default constructor.
	 */
	public NBodyPanel()
	{
		setOpaque(true);
		setBackground(BACKGROUND);
	}

	/**
	 * Repaint this panel. 
	 * @param g the graphics object associated with this panel.
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	protected void paintComponent(Graphics g)
	{
		lock.lock();
		try
		{
			updating = true;
			super.paintComponent(g);
			if (positions != null) drawBodies(g, positions, BACKGROUND);
			if (newPositions != null)
			{
				positions = newPositions;
				newPositions = null;
			}
			if (positions != null) drawBodies(g, positions, FOREGROUND);
		}
		finally
		{
			updating = false;
			lock.unlock();
		}
	}

	/**
	 * Draw all the bodies in the specified color.
	 * @param g the graphics for this panel.
	 * @param pos the positions of the bodies to draw.
	 * @param c the color in which to draw the bodies.
	 */
	protected void drawBodies(Graphics g, Vector2d[] pos, Color c)
	{
		Color tmp = g.getColor();
		Graphics2D g2 = (Graphics2D) g;
		g.setColor(c);
		for (Vector2d v: pos) g2.fillRect((int) v.x, (int) v.y, 3, 3);
		g.setColor(tmp);
	}

	/**
	 * Add an update request.
	 * @param pos the new positions to display.
	 */
	public void updatePositions(Vector2d[] pos)
	{
		executor.submit(new UpdateRequest(pos));
	}

	/**
	 * Update request.
	 */
	public class UpdateRequest implements Runnable
	{
		/**
		 * The new positions to display.
		 */
		private Vector2d[] pos = null;
		/**
		 * Add an update request.
		 * @param pos the new positions to display.
		 */
		public UpdateRequest(Vector2d[] pos)
		{
			this.pos = pos;
		}

		/**
		 * Do the update request on the component.
		 * @see java.lang.Runnable#run()
		 */
		public void run()
		{
			if (updating) return;
			lock.lock();
			try
			{
				newPositions = pos;
				NBodyPanel.this.repaint();
			}
			finally
			{
				lock.unlock();
			}
		}
	}
}
