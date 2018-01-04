/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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
package org.jppf.node.screensaver.impl;

import java.awt.*;
import java.util.*;
import java.util.Timer;

import javax.swing.*;

import org.jppf.node.screensaver.JPPFScreenSaver;
import org.jppf.utils.TypedProperties;

/**
 * A built-in simple screen saver implementation.
 * It draws 500 discs at random locations with a random color 25 times / second.
 * Every 5 seconds, the screen is emptied (repainted in black).
 * @author Laurent Cohen
 * @since 4.0
 */
public class SimpleScreenSaver extends JPanel implements JPPFScreenSaver {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The pseudo-random number generator.
   */
  private Random rand = new Random(System.nanoTime());
  /**
   * Timer used to draw additional shapes efery 40 ms.
   */
  private Timer timer = null;
  /**
   * Set to <code>true</code> every 5s to indicate the screen should be emptied.
   */
  private volatile boolean reset = false;

  /**
   * Default constructor.
   */
  public SimpleScreenSaver() {
    super(true);
  }

  @Override
  public JComponent getComponent() {
    return this;
  }

  @Override
  public void init(final TypedProperties config, final boolean fullscreen) {
    setBackground(Color.BLACK);
    timer = new Timer("JPPFScreenSaverTimer");
    // executes approximately every 40 ms
    timer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        repaint();
      }
    }, 40L, 40L);
    // executes approximately every 5 s
    timer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        reset = true;
      }
    }, 5000L, 5000L);
  }

  @Override
  public void destroy() {
    if (timer != null) timer.cancel();
  }

  @Override
  public void paintComponent(final Graphics g) {
    // we do not call super.paintComponent(g) because we do not want to have
    // the backrground repainted each time, which would erase the shapes we draw.
    final int w = getWidth();
    final int h = getHeight();
    if (reset) {
      reset = false;
      g.setColor(Color.BLACK);
      g.fillRect(0, 0, w, h);
    } else {
      final int n = 5;
      for (int i=0; i<500; i++) {
        final int x = rand.nextInt(w-(n-1));
        final int y = rand.nextInt(h-(n-1));
        g.setColor(new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256)));
        g.fillOval(x, y, n, n);
      }
    }
  }
}
