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

package org.jppf.ui.console;

import javax.swing.JComponent;

import org.jppf.ui.monitoring.ConsoleLauncher;

/**
 * This class provides an API to launch the JPPF admininstration console
 * and to embed it within an external GUI application.
 * @author Laurent Cohen
 * @since 5.0
 */
public class JPPFAdminConsole {
  /**
   * Launch the JPPF administration console.
   * @param args the command-line arguments are not used.
   */
  public static void main(final String[] args) {
    ConsoleLauncher.main("org/jppf/ui/options/xml/JPPFAdminTool.xml", "file");
  }

  /**
   * Get the administration console as a {@link JComponent} that can be added to any Swing container.
   * This method always returns the same {@code JComponent} instance.
   * @return a {@link JComponent} enclosing the JPPF administration console.
   */
  public static JComponent getAdminConsole() {
    return ConsoleLauncher.loadAdminConsole();
  }
}
