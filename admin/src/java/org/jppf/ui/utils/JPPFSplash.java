/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Timer;

import javax.swing.*;

import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

import net.miginfocom.swing.MigLayout;

/**
 * This class handles the splash screen displayed while starting the admin console.
 * @author Laurent Cohen
 */
public class JPPFSplash extends Window {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFSplash.class);
  /**
   * The default list of images to use if none is specified in the configuration.
   */
  private static final List<String> DEFAULT_IMAGES = new ArrayList<String>() {{
    for (int i=1; i<=4; i++) add("/org/jppf/ui/resources/splash" + i + ".gif");
  }};
  /**
   * The default text color.
   */
  private static final Color DEFAULT_FOREGROUND = new Color(64, 64, 128);
  /**
   * Contains the images displayed by the splash screen.
   */
  private final List<ImageIcon> images = new ArrayList<>();
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
  private final long delay = JPPFConfiguration.get(JPPFProperties.UI_SPLASH_DELAY);

  /**
   * Initialize this window with the specified owner.
   * @param message the message to display.
   */
  public JPPFSplash(final String message) {
    super(null);
    setBackground(Color.WHITE);
    List<String> pathList = StringUtils.parseStrings(JPPFConfiguration.get(JPPFProperties.UI_SPLASH_IMAGES), "\\|");
    for (String path: pathList) {
      ImageIcon icon = GuiUtils.loadIcon(path, false);
      if (icon != null) images.add(icon);
    }
    if (images.isEmpty()) {
      for (String path: DEFAULT_IMAGES) images.add(GuiUtils.loadIcon(path, false));
    }
    label = new JLabel(images.get(0));
    label.setBorder(BorderFactory.createLineBorder(new Color(192, 192, 192), 1, true));
    label.setHorizontalTextPosition(SwingConstants.CENTER);
    label.setVerticalTextPosition(SwingConstants.CENTER);
    label.setText(message);
    Color color = DEFAULT_FOREGROUND;
    String s = JPPFConfiguration.get(JPPFProperties.UI_SPLASH_MESSAGE_COLOR);
    if ((s != null) && !s.trim().isEmpty()) {
      String[] comps = s.split(",");
      if ((comps != null) && (comps.length >= 3)) {
        int rgb[] = new int[comps.length];
        try {
          for (int i=0; i<comps.length; i++) rgb[i] = Integer.valueOf(comps[i].trim());
          color = (rgb.length == 3) ? new Color(rgb[0], rgb[1], rgb[2]) : new Color(rgb[0], rgb[1], rgb[2], rgb[3]);
        } catch(Exception e) {
          log.debug(e.getMessage(), e);
        }
      }
    }
    label.setForeground(color);
    Font tmp = label.getFont();
    label.setFont(new Font(tmp.getFamily(), Font.BOLD, 24));
    setLayout(new MigLayout("fill, ins 4 4 4 4"));
    add(label, "grow, push");
    pack();
    setVisible(true);
  }

  /**
   * Start the animation of this splash screen.
   */
  public void start() {
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
  public void stop() {
    Runnable r = new Runnable() {
      @Override
      public void run() {
        /* try { Thread.sleep(2000); }
         * catch(InterruptedException e) {} */
        setVisible(false);
        task.cancel();
        timer.purge();
        dispose();
      }
    };
    new Thread(r).start();
  }

  /**
   * Task that scrolls the images at regular intervals.
   */
  public class ScrollTask extends TimerTask {
    /**
     * Position in the array of images.
     */
    private int pos = 0;

    /**
     * Execute this task.
     * @see java.util.TimerTask#run()
     */
    @Override
    public void run() {
      pos = (pos + 1) % images.size();
      label.setIcon(images.get(pos));
    }
  }
}
