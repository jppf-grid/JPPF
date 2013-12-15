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

import org.jppf.node.screensaver.*;
import org.jppf.utils.TypedProperties;

/**
 * A built-in screen saver implementation.
 * @author Laurent Cohen
 * @author nissalia
 * @since 4.0
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
   * The number of flying logos;
   */
  private int nbLogos = 10;
  /**
   * The icon holding the flying logo image.
   */
  private ImageIcon[] logos = null;
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
   * The horizontal alignment of the node status panel.
   */
  private int alignment = 1;
  /**
   * The JPPF configuration.
   */
  private TypedProperties config;

  /**
   * Default constructor.
   */
  public JPPFScreenSaverImpl() {
    super(true);
  }

  @Override
  public JComponent getComponent() {
    return this;
  }

  @Override
  public void init(final TypedProperties config, final boolean fullscreen) {
    this.config = new TypedProperties(config);
    configure();
    data = new ImageData[nbLogos];
    for (int i=0; i<nbLogos; i++) data[i] = new ImageData(logos[i % logos.length]);
    Dimension dim = this.getSize();
    for (ImageData d: data) d.init(dim);

    if (nodePanel == null) nodePanel = createNodePanel();
    SpringLayout layout = new SpringLayout();
    setLayout(layout);
    this.add(nodePanel);
    setBackground(Color.BLACK);
    Dimension dim2 = nodePanel.getPreferredSize();
    int hmargin = (getWidth() - dim2.width) / 2;
    int vmargin = (getHeight() - dim2.height) / 2;
    layout.putConstraint(SpringLayout.WEST, nodePanel, alignment * hmargin, SpringLayout.WEST, this);
    layout.putConstraint(SpringLayout.NORTH, nodePanel, vmargin, SpringLayout.NORTH, this);
    if (timer == null) {
      timer = new Timer("JPPFScreenSaverTimer");
      timer.schedule(new LogoUpdateTask(), 100L, 1000L / speed);
      timer.schedule(new LogoPaintTask(), 100L, 20L);
      TimerTask task = new TimerTask() {
        @Override
        public void run() {
          if (nodePanel != null) nodePanel.updateTimeLabel();
        }
      };
      timer.scheduleAtFixedRate(task, 1000L, 1000L);
    }
  }

  /**
   * Initialize the parameters of the screensaver.
   */
  private void configure() {
    collisions = config.getBoolean("jppf.screensaver.handle.collisions", true);
    nbLogos = config.getInt("jppf.screensaver.logos", 10);
    speed = config.getInt("jppf.screensaver.speed", 10);
    if (speed < 1) speed = 1;
    if (speed > MAX_SPEED) speed = MAX_SPEED;
    String defaultPath = NodePanel.IMAGE_PATH + '/' + "jppf_group_small.gif";
    String paths = config.getString("jppf.screensaver.logo.path", defaultPath);
    String[] tokens = paths.split("\\|");
    java.util.List<ImageIcon> list = new LinkedList<>();
    for (String s: tokens) {
      ImageIcon icon = ScreenSaverMain.loadImage(s.trim());
      if (icon != null) list.add(icon);
    }
    if (list.isEmpty()) list.add(ScreenSaverMain.loadImage(defaultPath));
    logos = new ImageIcon[list.size()];
    Random rnd = new Random(System.nanoTime());
    int count = 0;
    while (!list.isEmpty()) {
      int n = rnd.nextInt(list.size());
      logos[count++] = list.remove(n);
    }
    //logos = list.toArray(new ImageIcon[list.size()]);
    String s = config.getString("jppf.screensaver.status.panel.alignment", "center").trim().toLowerCase();
    if (s.startsWith("l")) alignment = 0;
    else if (s.startsWith("r")) alignment = 2;
    else alignment = 1;
  }

  @Override
  public void destroy() {
    if (timer != null) {
      timer.cancel();
      timer = null;
    }
    if (nodePanel != null) nodePanel = null;
  }

  /**
   * Timer task to display the logos at a rate of 25 frames/sec.
   */
  private class LogoPaintTask  extends TimerTask {
    /**
     * Request the painting of the flying logos.
     */
    @Override
    public void run() {
      JPPFScreenSaverImpl.this.repaint();
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
      if (data == null) return;
      try {
        Dimension dim = JPPFScreenSaverImpl.this.getSize();
        for (int i=0; i<data.length; i++) {
          ImageData d = data[i];
          d.update(dim, collisions ? data : null, i+1);
        }
      } catch(Throwable t) {
        t.printStackTrace();
      }
    }
  }

  /**
   * Performs the repainting of the flying logo images, as well as that of the areas they were
   * occupying within the underlying components.
   * @param g the graphics object to use for painting.
   */
  private void paintLogos(final Graphics g) {
    if (data == null) return;
    Rectangle r = g.getClipBounds();
    try {
      for (ImageData d: data) {
        synchronized(d) {
          /*
          g.setClip(d.prevx, d.prevy, d.imgw, d.imgh);
          g.setColor(Color.BLACK);
          g.fillRect(d.prevx, d.prevy, d.imgw, d.imgh);
          */

          g.setClip(d.x, d.y, d.imgw, d.imgh);
          g.drawImage(d.img, d.x, d.y, null);
          d.prevx = d.x;
          d.prevy = d.y;
        }
      }
    } catch(Throwable t) {
      t.printStackTrace();
    } finally {
      g.setClip(r);
    }
  }

  /**
   * Create the node panel. This methods is provided so subclasses can override it
   * and eventually create as subclass of {@link NodePanel}.
   * @return an instance of {@link NodePanel} or one of its subclasses.
   */
  protected NodePanel createNodePanel() {
    return new NodePanel();
  }

  /**
   * Get the node panel.
   * @return a {@link NodePanel} instance.
   */
  public NodePanel getNodePanel() {
    return nodePanel;
  }

  @Override
  public void paint(final Graphics g) {
    super.paint(g);
    paintLogos(g);
  }

  /**
   * Get the Timer used to update GUI.
   * @return a Timer instance.
   */
  public Timer getTimer() {
    return timer;
  }
}
