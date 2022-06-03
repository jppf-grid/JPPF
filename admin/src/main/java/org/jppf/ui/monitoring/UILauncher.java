/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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
package org.jppf.ui.monitoring;

import java.awt.Frame;

import javax.swing.*;

import org.jppf.ui.options.OptionElement;
import org.jppf.ui.options.event.WindowClosingListener;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.ui.utils.*;
import org.jppf.utils.JPPFConfiguration;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * This class provides a graphical interface for monitoring the status and health of the JPPF server.<br>
 * It also provides a few customization options, such as setting the interval between 2 server refreshes,
 * and switching the color scheme (skin) for the whole UI.
 * @author Laurent Cohen
 */
public class UILauncher {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(UILauncher.class);
  /**
   * The unique instance of the embedded admin console.
   */
  private static JComponent consoleComponent = null;
  /**
   * The splash screen window.
   */
  private static JPPFSplash splash = null;
  /**
   * Whether the admin console is embedded within another application.
   * @since 5.0
   */
  private static boolean embedded = false;

  /**
   * Start this UI.
   * @param args not used.
   */
  public static void main(final String...args) {
    try {
      if ((args  == null) || (args.length < 2)) throw new IllegalArgumentException("Usage: UILauncher page_location location_source");
      final String[] laf = { "com.jgoodies.looks.windows.WindowsLookAndFeel", "com.jgoodies.looks.plastic.PlasticLookAndFeel",
          "com.jgoodies.looks.plastic.Plastic3DLookAndFeel", "com.jgoodies.looks.plastic.PlasticXPLookAndFeel" };
      //if (args.length > 2) adminConsole = false;
      final int n = 3;
      boolean success = false;
      final String s = System.getProperty("swing.defaultlaf");
      if (!success && (s != null)) {
        try {
          UIManager.setLookAndFeel(s);
          success = true;
        } catch(final Throwable t) {
          log.error("could not set specified look and feel '" + s + "' : " + t.getMessage());
          System.getProperties().remove("swing.defaultlaf");
        }
      }
      if (!success) {
        try {
          UIManager.setLookAndFeel(laf[n]);
        } catch(final Throwable t) {
          log.error("could not set look and feel '" + laf[n] + "' : " + t.getMessage());
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
      }
      final boolean showSplash = JPPFConfiguration.get(JPPFProperties.UI_SPLASH);
      if (showSplash) (splash = new JPPFSplash(JPPFConfiguration.get(JPPFProperties.UI_SPLASH_MESSAGE))).start();
      loadUI(args[0], args[1]);
      if (showSplash) splash.stop();
      final Frame[] frames = Frame.getFrames();
      for (final Frame f: frames) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            f.setVisible(true);
          }
        });
      }
    } catch(final Exception e) {
      e.printStackTrace();
      log.error(e.getMessage(), e);
      System.exit(1);
    }
  }

  /**
   * Load the UI from the specified path to an XML descriptor.
   * @param src the path tot he XML docuemnt to load
   * @param type the type of the path: url or file.
   * @return the root {@link JComponent} of the admin console.
   * @since 5.0
   */
  private synchronized static JComponent loadUI(final String src, final String type) {
    if (consoleComponent == null) {
      OptionElement elt = null;
      try {
        final Frame frame = new JFrame();
        OptionsHandler.setMainWindow(frame);
        frame.addWindowListener(new WindowClosingListener());
        if ("url".equalsIgnoreCase(type)) elt = OptionsHandler.addPageFromURL(src, null);
        else elt = OptionsHandler.addPageFromXml(src);
        frame.add(elt.getUIComponent());
        OptionsHandler.loadPreferences();
        frame.setTitle(elt.getLabel());
        final String iconPath = elt.getIconPath();
        frame.setIconImage(GuiUtils.loadIcon(iconPath != null ? iconPath : GuiUtils.JPPF_ICON).getImage());
        OptionsHandler.loadMainWindowAttributes(OptionsHandler.getPreferences().node(elt.getName()));
        OptionsHandler.getBuilder().triggerInitialEvents(elt);
      } catch (final Exception e) {
        e.printStackTrace();
        log.error(e.getMessage(), e);
      }
      consoleComponent = (elt == null) ? null : elt.getUIComponent();
    }
    return consoleComponent;
  }

  /**
   * Determine whether the admin conosle is embedded within another application.
   * @return {@code true} if the console is embedded, {@code false} otherwise.
   * @since 5.0
   */
  public static boolean isEmbedded() {
    return embedded;
  }
}
