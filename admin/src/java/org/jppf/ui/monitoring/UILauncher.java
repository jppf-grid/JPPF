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
import org.jppf.utils.JPPFConfiguration;
import org.slf4j.*;

/**
 * This class provides a graphical interface for monitoring the status and health
 * of the JPPF server.<br>
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
   * The splash screen window.
   */
  private static JPPFSplash splash = null;

  /**
   * Start this UI.
   * @param args not used.
   */
  public static void main(final String...args) {
    try {
      if ((args  == null) || (args.length < 2)) throw new Exception("Usage: UILauncher page_location location_source");
      String[] laf = { "com.jgoodies.looks.windows.WindowsLookAndFeel", "com.jgoodies.looks.plastic.PlasticLookAndFeel",
          "com.jgoodies.looks.plastic.Plastic3DLookAndFeel", "com.jgoodies.looks.plastic.PlasticXPLookAndFeel" };
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
      loadUI(args[0], args[1], true);
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
    return loadUI("org/jppf/ui/options/xml/JPPFAdminTool.xml", "file", false);
  }

  /**
   * Load the UI from the specified path to an XML descriptor.
   * @param src the path tot he XML docuemnt to load
   * @param type the type of the path: url or file.
   * @param createFrame determines whether the main application frame should alsd be created and initialized.
   * @return the root {@link JComponent} of the admin console.
   * @since 5.0
   */
  private static JComponent loadUI(final String src, final String type, final boolean createFrame) {
    try {
      OptionElement elt = null;
      if ("url".equalsIgnoreCase(type)) elt = OptionsHandler.addPageFromURL(src, null);
      else elt = OptionsHandler.addPageFromXml(src);
      OptionsHandler.loadPreferences();
      OptionsHandler.getBuilder().triggerInitialEvents(elt);
      if (createFrame) {
        JFrame frame = new JFrame(elt.getLabel());
        OptionsHandler.setMainWindow(frame);
        DockingManager.getInstance().setMainView(frame, (OptionContainer) elt);
        frame.setIconImage(GuiUtils.loadIcon(GuiUtils.JPPF_ICON).getImage());
        frame.addWindowListener(new WindowClosingListener());
        StatsHandler.getInstance();
        frame.getContentPane().add(elt.getUIComponent());
        OptionsHandler.loadMainWindowAttributes(OptionsHandler.getPreferences().node("JPPFAdminTool"));
      } else {
        JComponent comp = elt.getUIComponent();
        comp.addHierarchyListener(new MainFrameObserver(elt));
      }
      return elt.getUIComponent();
    } catch (Exception e) {
      e.printStackTrace();
      log.error(e.getMessage(), e);
    }
    return null;
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
        //System.out.println("found frame = " + frame);
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
}
