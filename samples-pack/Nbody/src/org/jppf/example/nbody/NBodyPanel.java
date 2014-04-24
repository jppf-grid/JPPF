/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

//******************************************************************************
// This Java source file is copyright (C) 2008 by Alan Kaminsky. All rights
// reserved. For further information, contact the author, Alan Kaminsky, at
// ark@cs.rit.edu.
//
// This Java source file is part of the Parallel Java Library ("PJ"). PJ is free
// software; you can redistribute it and/or modify it under the terms of the GNU
// General Public License as published by the Free Software Foundation; either
// version 3 of the License, or (at your option) any later version.
//
// PJ is distributed in the hope that it will be useful, but WITHOUT ANY
// WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
// A PARTICULAR PURPOSE. See the GNU General Public License for more details.
//
// A copy of the GNU General Public License is provided in the file gpl.txt. You
// may also obtain a copy of the GNU General Public License on the World Wide
// Web at http://www.gnu.org/licenses/gpl.html.
//
//******************************************************************************

package org.jppf.example.nbody;

import java.awt.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JPanel;

import org.jppf.utils.JPPFThreadFactory;

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
  private ExecutorService executor = Executors.newFixedThreadPool(1, new JPPFThreadFactory("PanelUpdater"));
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
   * determines whether the display is being updated.
   */
  private AtomicBoolean updating = new AtomicBoolean(false);

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
  @Override
  protected void paintComponent(final Graphics g)
  {
    //lock.lock();
    try
    {
      updating.set(true);
      super.paintComponent(g);
      if (positions != null) drawBodies(g, positions, BACKGROUND);
      if (newPositions != null)
      {
        positions = newPositions;
        newPositions = null;
      }
      if (positions != null) drawBodies(g, positions, FOREGROUND);
    }
    catch(Throwable e)
    {
      e.printStackTrace();
    }
    finally
    {
      updating.set(false);
      //lock.unlock();
    }
  }

  /**
   * Draw all the bodies in the specified color.
   * @param g the graphics for this panel.
   * @param pos the positions of the bodies to draw.
   * @param c the color in which to draw the bodies.
   */
  protected void drawBodies(final Graphics g, final Vector2d[] pos, final Color c)
  {
    Color tmp = g.getColor();
    Graphics2D g2 = (Graphics2D) g;
    g.setColor(c);
    for (Vector2d v: pos) g2.fillRect((int) v.x, (int) v.y, 3, 3);
    //for (Vector2d v: pos) g2.fillOval((int) v.x, (int) v.y, 3, 3);
    g.setColor(tmp);
  }

  /**
   * Add an update request.
   * @param pos the new positions to display.
   */
  public void updatePositions(final Vector2d[] pos)
  {
    if (!isUpdating()) executor.submit(new UpdateRequest(pos));
  }

  /**
   * Determine whether this panel is currently being updated.
   * @return true if this panel is being updated, false otherwise.
   */
  public boolean isUpdating()
  {
    return updating.get();
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
    public UpdateRequest(final Vector2d[] pos)
    {
      this.pos = pos;
    }

    /**
     * Do the update request on the component.
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run()
    {
      if (updating.get()) return;
      //lock.lock();
      try
      {
        newPositions = pos;
        NBodyPanel.this.repaint();
      }
      finally
      {
        //lock.unlock();
      }
    }
  }
}
