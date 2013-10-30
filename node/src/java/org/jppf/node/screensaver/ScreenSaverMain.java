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

package org.jppf.node.screensaver;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.jppf.node.initialization.InitializationHook;
import org.jppf.utils.*;

/**
 * 
 * @author Laurent Cohen
 */
public class ScreenSaverMain implements InitializationHook
{
  /**
   * The singleton instance of this class.
   */
  private static ScreenSaverMain instance = null;
  /**
   * The JPPF configuration.
   */
  private TypedProperties config;
  /**
   * The screensaver implementation.
   */
  private JPPFScreenSaver screensaver = null;

  /**
   * Test the screen saver stadalone (not part of a node).
   * @param args not used.
   */
  public static void main(final String[] args) {
    try {
      new ScreenSaverMain().startScreenSaver(JPPFConfiguration.getProperties());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void initializing(final UnmodifiableTypedProperties initialConfiguration) {
    try {
      if (instance != null) return;
      if (initialConfiguration.getBoolean("jppf.screensaver.enabled")) startScreenSaver(initialConfiguration);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Start the screen saver using the specified JPPF configuration.
   * @param config the configuration to use.
   * @throws Exception if any error occurs.
   */
  public void startScreenSaver(final TypedProperties config) throws Exception {
    instance = this;
    this.config = config;
    createUI();
  }

  /**
   * Create and initialize the UI and graphics.
   * @throws Exception if any error occurs.
   */
  private void createUI() throws Exception {
    if (GraphicsEnvironment.isHeadless()) {
      System.err.println("This is a headless graphics environment - cannot run in full screen");
      return;
    }
    boolean fullscreen = config.getBoolean("jppf.screensaver.fullscreen", false);
    GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
    final GraphicsDevice device = env.getDefaultScreenDevice();
    final boolean fullscreenSupported = device.isFullScreenSupported();
    if (fullscreen && !fullscreenSupported) System.err.println("Full screen is not supported by the current graphics device");
    JFrame frame = new JFrame("JPPF screensaver");
    if (fullscreen && fullscreenSupported) {
      frame.setUndecorated(true);
      frame.setResizable(false);
    } else {
      frame.setVisible(true);
      Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
      int w = JPPFConfiguration.getProperties().getInt("jppf.screensaver.width", -1);
      int h = JPPFConfiguration.getProperties().getInt("jppf.screensaver.height", -1);
      if (w <= 0) w = d.width;
      if (h <= 0) h = d.height;
      frame.setSize(w, h);
    }
    frame.setBackground(Color.BLACK);
    frame.getContentPane().setBackground(Color.BLACK);
    if (fullscreen && fullscreenSupported) device.setFullScreenWindow(frame);
    final JPPFScreenSaver screensaver = createScreenSaver();
    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        screensaver.destroy();
        device.setFullScreenWindow(null);
        System.exit(0);
      }
    });
    JComponent comp = screensaver.getComponent();
    frame.getContentPane().add(comp);
    if (fullscreen && fullscreenSupported) comp.setSize(frame.getSize());
    screensaver.init(fullscreen && fullscreenSupported);
    frame.setVisible(true);
  }

  /**
   * Create a screen saver object based on the configuration.
   * @return a {@link JPPFScreenSaver} instance, or <code>null</code> if no screen saver could be created.
   * @throws Exception if any error occurs.
   */
  private JPPFScreenSaver createScreenSaver() throws Exception {
    String name = JPPFConfiguration.getProperties().getString("jppf.screensaver.class");
    Class<?> clazz = Class.forName(name);
    screensaver = (JPPFScreenSaver) clazz.newInstance();
    return screensaver;
  }


  /**
   * Get the screensaver implementation.
   * @return a {@link JPPFScreenSaver} instance.
   */
  public JPPFScreenSaver getScreenSaver() {
    return screensaver;
  }

  /**
   * Get the singleton instance of this class.
   * @return a {@link ScreenSaverMain} instance.
   */
  public static ScreenSaverMain getInstance() {
    return instance;
  }

  /**
   * Get the JPPF configuration.
   * @return a {@link TypedProperties} instance.
   */
  public TypedProperties getConfig() {
    return config;
  }
}
