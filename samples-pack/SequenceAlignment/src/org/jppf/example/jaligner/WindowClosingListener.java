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

package org.jppf.example.jaligner;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import org.jppf.ui.options.factory.OptionsHandler;

/**
 * This class performs cleanup and preferences storing actions when the GUI is closed.
 * @author Laurent Cohen
 */
public class WindowClosingListener extends WindowAdapter
{
  /**
   * Process the closing of the main frame.
   * @param event the event we're interested in.
   * @see java.awt.event.WindowAdapter#windowClosing(java.awt.event.WindowEvent)
   */
  @Override
  public void windowClosing(final WindowEvent event)
  {
    OptionsHandler.savePreferences();
    System.exit(0);
  }
}
