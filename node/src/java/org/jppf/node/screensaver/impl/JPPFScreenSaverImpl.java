/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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
import org.jppf.utils.*;

/**
 * A panel that serves as a GUI on top of a JPPF node, displayed as a screen saver.
 * @author Laurent Cohen
 * @author nissalia
 */
public class JPPFScreenSaverImpl extends JPanel implements JPPFScreenSaver {
  /**
   * 
   */
  private static final int MAX_SPEED = 100;
  /**
   * The node UI used in the screen saver.
   */
  private NodePanel nodePanel = null;
  /**
   * The icon holding the flying logo image.
   */
  private ImageIcon logo = null;
  /**
   * The number of flying logos;
   */
  private int nbLogos = 10;
  /**
   * The image object for the flying logos.
   */
  private Image logoImg = null;
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
   * Timer used to update the position of the flying logos at regular intervals.
   */
  private Timer timer = null;
  /**
   * Full screen flag.
   */
  private boolean fullscreen = false;

  /**
   * Default constructor.
   */
  public JPPFScreenSaverImpl() {
    setLayout(new BorderLayout());
    setBackground(Color.BLACK);
  }

  @Override
  public JComponent getComponent() {
    return this;
  }

  @Override
  public void init(final boolean fullscreen) {
    this.fullscreen = fullscreen;
    initializeSettings();
    if (nodePanel == null) nodePanel = new NodePanel();
    nodePanel.setDoubleBuffered(true);
    this.add(nodePanel, BorderLayout.CENTER);

    data = new ImageData[nbLogos];
    for (int i=0; i<nbLogos; i++) data[i] = new ImageData(logo);
    Dimension dim = this.getSize();
    System.out.println("window dimension = " + dim);
    Random rand = new Random(System.currentTimeMillis());
    for (int i=0; i<nbLogos; i++) {
      int n = dim.width - logo.getIconWidth();
      if (n <= 0) n = logo.getIconWidth();
      data[i].x = rand.nextInt(n);
      data[i].prevx = data[i].x;
      data[i].stepX *= 2 * rand.nextInt(2) - 1;
      n = dim.height - logo.getIconHeight();
      if (n <= 0) n = logo.getIconHeight();
      data[i].y = rand.nextInt(n);
      data[i].prevy = data[i].y;
      data[i].stepY *= 2 * rand.nextInt(2) - 1;
    }
    setDoubleBuffering(this);
    if (timer == null) {
      timer = new Timer();
      //timer.schedule(new LogoUpdateTask(), 100, 25L + 5L * (11L - speed));
      timer.schedule(new LogoUpdateTask(), 100, 1000 / speed);
      timer.schedule(new LogoPaintTask(), 500L, 25L);
      TimerTask task = new TimerTask() {
        @Override
        public void run() {
          if (nodePanel != null) {
            String s = NodePanel.toStringDuration(System.currentTimeMillis() - nodePanel.startedAt);
            nodePanel.timeLabel.setText("Active for: "+s);
          }
        }
      };
      timer.scheduleAtFixedRate(task, 1000L, 1000L);
    }
  }

  /**
   * Initialize the parameters of the screensaver.
   */
  private void initializeSettings() {
    TypedProperties config = JPPFConfiguration.getProperties();
    collisions = config.getBoolean("jppf.screensaver.handle.collisions", true);
    nbLogos = config.getInt("jppf.screensaver.logos", 10);
    speed = config.getInt("jppf.screensaver.speed", 10);
    if (speed < 1) speed = 1;
    if (speed > MAX_SPEED) speed = MAX_SPEED;
    String path = config.getString("jppf.screensaver.logo.path", NodePanel.IMAGE_PATH + '/' + "jppf_group_small.gif");
    logo = NodePanel.loadImage(path);
    logoImg = logo.getImage();
  }

  /**
   * Set a hierarchy of Swing components as double buffered.
   * @param comp the root of the components hierarchy.
   */
  public static void setDoubleBuffering(final JComponent comp) {
    comp.setDoubleBuffered(true);
    for (int i=0; i<comp.getComponentCount(); i++) {
      Component c = comp.getComponent(i);
      if (c instanceof JComponent) setDoubleBuffering((JComponent) c);
    }
  }

  @Override
  public void destroy() {
    timer.cancel();
    if (nodePanel != null) {
      nodePanel.cleanup();
      nodePanel = null;
    }
  }

  /**
   * Timer task to display the logos at a rate of 25 frames/sec.
   */
  public class LogoPaintTask  extends TimerTask {
    /**
     * The task that renders the flying logos.
     */
    Runnable task = null;

    /**
     * Initialize the task that renders the flying logos.
     */
    public LogoPaintTask() {
      task = new Runnable() {
        @Override
        public void run() {
          paintLogos();
        }
      };
    }

    /**
     * Update the position and direction of the flying logos.
     * @see java.util.TimerTask#run()
     */
    @Override
    public void run() {
      SwingUtilities.invokeLater(task);
    }
  }

  /**
   * Timer task to update the position and direction of the flying logos.
   */
  private class LogoUpdateTask extends TimerTask {
    /**
     * Update the position and direction of the flying logos.
     */
    @Override
    public void run() {
      Dimension dim = JPPFScreenSaverImpl.this.getSize();
      for (int i=0; i<data.length; i++) {
        ImageData d = data[i];
        if (collisions) {
          for (int j=i+1; j<data.length; j++) d.checkColliding(data[j]);
        }
        d.update(dim);
      }
    }
  }

  /**
   * Performs the repainting of the flying logo images, as well as that of the areas they were
   * occupying within the underlying components.
   */
  public void paintLogos() {
    Graphics g = getGraphics();
    Image buffer = createVolatileImage(getWidth(), getHeight());
    Graphics bufferGraphics = buffer.getGraphics();
    try {
      for (ImageData d: data) {
        synchronized(d) {
          int minx = Math.min(d.prevx, d.x);
          int maxx = Math.max(d.prevx, d.x);
          int miny = Math.min(d.prevy, d.y);
          int maxy = Math.max(d.prevy, d.y);
          int w = maxx - minx + logo.getIconWidth();
          int h = maxy - miny + logo.getIconHeight();
          bufferGraphics.setClip(minx, miny, w, h);
          paint(bufferGraphics);
          bufferGraphics.drawImage(logoImg, d.x, d.y, null);
          d.prevx = d.x;
          d.prevy = d.y;
          g.drawImage(buffer, minx, miny, minx + w, miny + h, minx, miny, minx + w, miny + h, null);
        }
      }
    } finally {
      bufferGraphics.dispose();
      g.dispose();
    }
  }

  /**
   * Get the node panel.
   * @return a {@link NodePanel} instance.
   */
  public NodePanel getNodePanel()
  {
    return nodePanel;
  }
}
