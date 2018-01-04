/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

import org.jppf.ui.console.JPPFAdminConsole;
import org.jppf.ui.utils.GuiUtils;

/**
 * Test embedding the admin console in a user-defined GUI.
 * @author Laurent Cohen
 */
public class EmbeddedConsole {
  /**
   * 
   * @param args not used
   * @throws Exception if any error occurs.
   */
  public static void main(final String[] args) throws Exception {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    final JPanel mainPanel = new JPanel();
    // layout the components vetically within a box
    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
    // the top component is a label within an image and some text
    final ImageIcon icon = GuiUtils.loadIcon("../admin/jppf_logo.gif");
    final JLabel label = new JLabel("Test JPPF embedded console", icon, SwingConstants.LEFT);
    mainPanel.add(label);
    mainPanel.add(JPPFAdminConsole.getAdminConsole());
    // add the admin console as the bottom componenty
    final JFrame frame = new JFrame("Embedded console");
    frame.setSize(800, 600);
    frame.add(mainPanel);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setVisible(true);
  }
}
