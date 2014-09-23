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

package test.console;

import javax.swing.*;

import org.jppf.ui.monitoring.UILauncher;
import org.jppf.ui.utils.GuiUtils;

/**
 * Test embedding the admin conosle in a user-defined GUI.
 * @author Laurent Cohen
 */
public class EmbeddedConsole {
  /**
   * 
   * @param args not used.
   */
  public static void main(final String[] args) {
    try {
      try {
        //UIManager.setLookAndFeel(NimbusLookAndFeel.class.getName());
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      } catch(Throwable t) {
        t.printStackTrace();;
      }
      JPanel topPanel = new JPanel();
      topPanel.setAlignmentX(0f);
      topPanel.setBorder(BorderFactory.createEtchedBorder());
      JLabel label = new JLabel("Test JPPF embedded console", GuiUtils.loadIcon("../admin/jppf_splash.gif"), SwingConstants.LEFT);
      topPanel.add(label);
      JPanel mainPanel = new JPanel();
      BoxLayout layout = new BoxLayout(mainPanel, BoxLayout.Y_AXIS);
      mainPanel.setLayout(layout);
      mainPanel.add(topPanel);
      mainPanel.add(UILauncher.loadUI());
      JFrame frame = new JFrame("Embedded console");
      frame.setSize(800, 800);
      frame.add(mainPanel);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setVisible(true);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
