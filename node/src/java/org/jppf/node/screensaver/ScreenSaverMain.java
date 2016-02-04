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

package org.jppf.node.screensaver;

import static org.jppf.utils.configuration.JPPFProperties.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.*;

import org.jppf.node.initialization.InitializationHook;
import org.jppf.node.screensaver.impl.JPPFScreenSaverImpl;
import org.jppf.utils.*;

/**
 * Main entry point for starting the screen saver.
 * @author Laurent Cohen
 * @since 4.0
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
      Thread.setDefaultUncaughtExceptionHandler(new JPPFDefaultUncaughtExceptionHandler());
      new ScreenSaverMain().startScreenSaver(JPPFConfiguration.getProperties());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void initializing(final UnmodifiableTypedProperties initialConfiguration) {
    try {
      if (instance != null) return;
      if (initialConfiguration.get(SCREENSAVER_ENABLED)) startScreenSaver(initialConfiguration);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Start the screen saver using the specified JPPF configuration.
   * @param config the configuration to use.
   * @throws Exception if any error occurs.
   */
  private void startScreenSaver(final TypedProperties config) throws Exception {
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
    boolean fullscreenRequested = config.get(SCREENSAVER_FULLSCREEN);
    final GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
    final GraphicsDevice device = env.getDefaultScreenDevice();
    final GraphicsDevice[] devices = env.getScreenDevices();
    boolean fullscreenSupported = true;
    Rectangle2D result = new Rectangle2D.Double();
    for (GraphicsDevice gd : devices) {
      fullscreenSupported &= gd.isFullScreenSupported();
      for (GraphicsConfiguration graphicsConfiguration : gd.getConfigurations()) {
        Rectangle2D.union(result, graphicsConfiguration.getBounds(), result);
      }
    }

    if (fullscreenRequested && !fullscreenSupported) System.err.println("Full screen is not supported by the current graphics device");
    String title = config.get(SCREENSAVER_TITLE);
    final JFrame frame = new FocusedJFrame(title);
    ImageIcon icon = loadImage(config.get(SCREENSAVER_ICON));
    if (icon == null) icon = loadImage(SCREENSAVER_ICON.getDefaultValue());
    frame.setIconImage(icon.getImage());
    if (fullscreenRequested && fullscreenSupported) {
      frame.setUndecorated(true);
      frame.setSize((int) result.getWidth(), (int) result.getHeight());
      frame.setResizable(false);
      // hide the mouse cursor over the JFrame
      BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB); // Transparent 16 x 16 pixel cursor image.
      Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(0, 0), "blank cursor"); // Create a new blank cursor.
      frame.getContentPane().setCursor(blankCursor); // Set the blank cursor to the JFrame.
    } else {
      Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
      int w = JPPFConfiguration.get(SCREENSAVER_WIDTH);
      int h = JPPFConfiguration.get(SCREENSAVER_HEIGHT);
      if ((w <= 0) || (w > d.width)) w = 1000;
      if ((h <= 0) || (h > d.height)) h = 800;
      frame.setSize(w, h);
    }
    configureFrameListeners(frame, fullscreenRequested && fullscreenSupported);
    frame.setBackground(Color.BLACK);
    frame.getContentPane().setBackground(Color.BLACK);
    createScreenSaver();
    frame.add(screensaver.getComponent());
    screensaver.getComponent().setSize(frame.getSize());
    int screenX = config.get(SCREENSAVER_LOCATION_X);
    int screenY = config.get(SCREENSAVER_LOCATION_Y);
    frame.setLocation(screenX, screenY);
    screensaver.init(config, fullscreenRequested && fullscreenSupported);
    frame.setVisible(true);
    java.awt.EventQueue.invokeLater(new Runnable() {
      @Override public void run() {
        frame.toFront();
      }
    });
  }

  /**
   * Create a screen saver object based on the configuration.
   * @return a {@link JPPFScreenSaver} instance, or <code>null</code> if no screen saver could be created.
   * @throws Exception if any error occurs.
   */
  private JPPFScreenSaver createScreenSaver() throws Exception {
    try {
      String name = JPPFConfiguration.get(SCREENSAVER_CLASS);
      Class<?> clazz = Class.forName(name);
      screensaver = (JPPFScreenSaver) clazz.newInstance();
    } catch(Exception e) {
      screensaver = new JPPFScreenSaverImpl();
    }
    return screensaver;
  }

  /**
   * Configure the mouse and keyboard listener for the frame.
   * @param frame the frame to configure.
   * @param fullscreen whether the frame is displayed in full screen mode.
   */
  private void configureFrameListeners(final JFrame frame, final boolean fullscreen) {
    if (fullscreen) {
      frame.addKeyListener(new KeyAdapter() {
        @Override public void keyPressed(final KeyEvent e) {
          doOnclose();
        }
      });
      frame.addMouseListener(new MouseAdapter() {
        @Override public void mousePressed(final MouseEvent e) {
          doOnclose();
        }
      });
      if (config.get(SCREENSAVER_MOUSE_MOTION_CLOSE)) {
        final long mouseMotionDelay = config.get(SCREENSAVER_MOUSE_MOTION_DELAY);
        final long start = System.nanoTime();
        frame.addMouseMotionListener(new MouseAdapter() {
          @Override
          public void mouseMoved(final MouseEvent e) {
            if (DateTimeUtils.elapsedFrom(start) > mouseMotionDelay) doOnclose();
          }
        });
      }
    }
    frame.addWindowListener(new WindowAdapter() {
      @Override public void windowClosing(final WindowEvent e) {
        doOnclose();
      }
    });
  }

  /**
   * Get the screensaver implementation.
   * @return a {@link JPPFScreenSaver} instance.
   */
  JPPFScreenSaver getScreenSaver() {
    return screensaver;
  }

  /**
   * Get the singleton instance of this class.
   * @return a {@link ScreenSaverMain} instance.
   */
  static ScreenSaverMain getInstance() {
    return instance;
  }

  /**
   * Get the JPPF configuration.
   * @return a {@link TypedProperties} instance.
   */
  TypedProperties getConfig() {
    return config;
  }

  /**
   * Action performed when the frame is closed, or upon key pressed, mouse click
   * or mouse motion events in full screen.
   */
  private void doOnclose() {
    if (screensaver != null) screensaver.destroy();
    System.exit(0);
  }

  /**
   * Load an icon from the specified path.
   * @param file the file to get the icon from.
   * @return an <code>ImageIcon</code> instance.
   */
  public static ImageIcon loadImage(final String file) {
    byte[] buf = null;
    try {
      buf = FileUtils.getPathAsByte(file);
    } catch (Exception e) {
      System.err.println("Could not load image '" + file + "' : " + ExceptionUtils.getStackTrace(e));
    }
    return (buf == null) ? null : new ImageIcon(Toolkit.getDefaultToolkit().createImage(buf));
  }
}
