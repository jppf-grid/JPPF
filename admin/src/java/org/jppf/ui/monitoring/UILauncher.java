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
package org.jppf.ui.monitoring;

import java.awt.Frame;

import javax.swing.*;

import org.jppf.ui.options.OptionElement;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.ui.utils.JPPFSplash;
import org.jppf.utils.JPPFConfiguration;
import org.slf4j.*;

/**
 * This class provides a graphical interface for monitoring the status and health
 * of the JPPF server.<br>
 * It also provides a few customization options, such as setting the interval between 2 server refreshes,
 * and switching the color scheme (skin) for the whole UI.
 * @author Laurent Cohen
 */
public class UILauncher
{
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
  public static void main(final String...args)
  {
    try
    {
      if ((args  == null) || (args.length < 2)) throw new Exception("Usage: UILauncher page_location location_source");
      String[] laf = { "com.jgoodies.looks.windows.WindowsLookAndFeel", "com.jgoodies.looks.plastic.PlasticLookAndFeel",
          "com.jgoodies.looks.plastic.Plastic3DLookAndFeel", "com.jgoodies.looks.plastic.PlasticXPLookAndFeel" };
      int n = 3;
      boolean success = false;
      String s = System.getProperty("swing.defaultlaf");
      if (!success && (s != null))
      {
        try
        {
          UIManager.setLookAndFeel(s);
          success = true;
        }
        catch(Throwable t)
        {
          log.error("could not set specified look and feel '" + s + "' : " + t.getMessage());
          System.getProperties().remove("swing.defaultlaf");
        }
      }
      if (!success)
      {
        try
        {
          UIManager.setLookAndFeel(laf[n]);
        }
        catch(Throwable t)
        {
          log.error("could not set look and feel '" + laf[n] + "' : " + t.getMessage());
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
      }
      boolean showSplash = JPPFConfiguration.getProperties().getBoolean("jppf.ui.splash", true);
      if (showSplash)
      {
        splash = new JPPFSplash("The management console is starting ...");
        splash.start();
      }
      OptionElement elt = null;
      if ("url".equalsIgnoreCase(args[1])) elt = OptionsHandler.addPageFromURL(args[0], null);
      else elt = OptionsHandler.addPageFromXml(args[0]);
      OptionsHandler.loadPreferences();
      OptionsHandler.getBuilder().triggerInitialEvents(elt);
      if (showSplash) splash.stop();
      Frame[] frames = JFrame.getFrames();
      for (Frame f: frames) f.setVisible(true);
    }
    catch(Exception e)
    {
      e.printStackTrace();
      log.error(e.getMessage(), e);
      System.exit(1);
    }
  }
}
