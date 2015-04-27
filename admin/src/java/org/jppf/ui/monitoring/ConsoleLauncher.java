/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.jppf.ui.monitoring.data.StatsHandler;
import org.jppf.ui.options.*;
import org.jppf.ui.options.docking.DockingManager;
import org.jppf.ui.options.event.WindowClosingListener;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.ui.utils.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class provides a graphical interface for monitoring the status and health of the JPPF server.<br>
 * It also provides a few customization options, such as setting the interval between 2 server refreshes,
 * and switching the color scheme (skin) for the whole UI.
 * @author Laurent Cohen
 */
public class ConsoleLauncher {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(ConsoleLauncher.class);
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
   * .
   * @since 5.0
   */
  private static boolean adminConsole = true;

  /**
   * Start this UI.
   * @param args not used.
   */
  public static void main(final String...args) {
    try {
      Thread.setDefaultUncaughtExceptionHandler(new JPPFDefaultUncaughtExceptionHandler());
      if ((args  == null) || (args.length < 2)) throw new IllegalArgumentException("Usage: UILauncher page_location location_source");
      String[] laf = { "com.jgoodies.looks.windows.WindowsLookAndFeel", "com.jgoodies.looks.plastic.PlasticLookAndFeel",
          "com.jgoodies.looks.plastic.Plastic3DLookAndFeel", "com.jgoodies.looks.plastic.PlasticXPLookAndFeel" };
      //if (args.length > 2) adminConsole = false;
      int n = 3;
      boolean success = false;
      String s = System.getProperty("swing.defaultlaf");
      if (!success && (s != null)) {
        try {
          UIManager.setLookAndFeel(s);
          success = true;
        } catch(Throwable t) {
          log.error("could not set specified look and feel '" + s + "' : " + t.getMessage());
          System.getProperties().remove("swing.defaultlaf");
        }
      }
      if (!success) {
        try {
          UIManager.setLookAndFeel(laf[n]);
        } catch(Throwable t) {
          log.error("could not set look and feel '" + laf[n] + "' : " + t.getMessage());
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
      }
      boolean showSplash = JPPFConfiguration.getProperties().getBoolean("jppf.ui.splash", true);
      if (showSplash) (splash = new JPPFSplash("The management console is starting ...")).start();
      loadUI(args[0], args[1], true, -1);
      if (showSplash) splash.stop();
      Frame[] frames = Frame.getFrames();
      for (final Frame f: frames) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            f.setVisible(true);
          }
        });
      }
    } catch(Exception e) {
      e.printStackTrace();
      log.error(e.getMessage(), e);
      System.exit(1);
    }
  }

  /**
   * Load the UI from the default XML descriptor.
   * @return the root {@link JComponent} of the admin console.
   * @since 5.0
   */
  public static JComponent loadAdminConsole() {
    embedded = true;
    return loadUI("org/jppf/ui/options/xml/JPPFAdminTool.xml", "file", false, -1);
  }

  /**
   * Reload the UI to take new preferences into account.
   * @return the root {@link JComponent} of the admin console.
   * @since 5.0
   */
  public synchronized static JComponent reloadUI() {
    int idx = -1;
    if (consoleComponent == null) {
      Frame frame = OptionsHandler.getMainWindow();
      if (frame != null) {
        for (int i=0; i<frame.getComponentCount(); i++) {
          if (frame.getComponent(i) == consoleComponent) {
            idx = i;
            break;
          }
        }
        frame.remove(consoleComponent);
        consoleComponent = null;
      }
    }
    return loadUI("org/jppf/ui/options/xml/JPPFAdminTool.xml", "file", !embedded, idx);
  }

  /**
   * Load the UI from the specified path to an XML descriptor.
   * @param src the path tot he XML docuemnt to load
   * @param type the type of the path: url or file.
   * @param createFrame determines whether the main application frame should alsd be created and initialized.
   * @param idx the index at which to insert the admin console in the main frame. A negative value measn insert at the end.
   * @return the root {@link JComponent} of the admin console.
   * @since 5.0
   */
  private synchronized static JComponent loadUI(final String src, final String type, final boolean createFrame, final int idx) {
    if (consoleComponent == null) {
      OptionElement elt = null;
      try {
        if ("url".equalsIgnoreCase(type)) elt = OptionsHandler.addPageFromURL(src, null);
        else elt = OptionsHandler.addPageFromXml(src);
        OptionsHandler.loadPreferences();
        OptionsHandler.getBuilder().triggerInitialEvents(elt);
        Frame frame = OptionsHandler.getMainWindow();
        if (createFrame) {
          if (frame == null) {
            frame = new JFrame(elt.getLabel());
            OptionsHandler.setMainWindow(frame);
            DockingManager.getInstance().setMainView(frame, (OptionContainer) elt);
            frame.setIconImage(GuiUtils.loadIcon(GuiUtils.JPPF_ICON).getImage());
            frame.addWindowListener(new WindowClosingListener());
          }
          StatsHandler.getInstance();
          if (idx >= 0) frame.add(elt.getUIComponent(), idx);
          else frame.add(elt.getUIComponent());
          OptionsHandler.loadMainWindowAttributes(OptionsHandler.getPreferences());
        } else {
          if (idx < 0) {
            JComponent comp = elt.getUIComponent();
            comp.addHierarchyListener(new MainFrameObserver(elt));
          } else {
            frame.add(elt.getUIComponent(), idx);
          }
        }
        OptionsHandler.getPluggableViewHandler().installViews();
      } catch (Exception e) {
        e.printStackTrace();
        log.error(e.getMessage(), e);
      }
      consoleComponent = (elt == null) ? null : elt.getUIComponent();
    }
    return consoleComponent;
  }

  /**
   * Listens for hierarchy events to find out when the component is finally linked to a frame.
   * @since 5.0
   */
  private final static class MainFrameObserver implements HierarchyListener {
    /**
     * Set to true whenever the application main frame is found.
     */
    private boolean frameFound = false;
    /**
     * Contains the root UI component of the admin console.
     */
    private final OptionElement uiRoot;

    /**
     * Initiialize this observer with the specified UI root.
     * @param uiRoot contains the root UI component of the admin console.
     */
    private MainFrameObserver(final OptionElement uiRoot) {
      this.uiRoot = uiRoot;
    }

    @Override
    public void hierarchyChanged(final HierarchyEvent event) {
      if (frameFound) return;
      long flags = event.getChangeFlags();
      Frame frame = getTopFrame(event.getChanged());
      if (frame != null) {
        frameFound = true;
        OptionsHandler.setMainWindow(frame);
        DockingManager.getInstance().setMainView(frame, (OptionContainer) uiRoot);
      }
    }

    /**
     * Get the frame, if any, at the top of the specified components hierarchy.
     * @param comp the component for which to lookup the hierarchy.
     * @return the top {@link Frame}, or {@code null} if there is no frame in the hierarchy.
     */
    private Frame getTopFrame(final Component comp) {
      if (comp instanceof Frame) return (Frame) comp;
      else if (comp != null) {
        Component tmp = comp;
        while (tmp.getParent() != null) tmp = tmp.getParent();
        if (tmp instanceof Frame) return (Frame) tmp;
      }
      return null;
    }
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
