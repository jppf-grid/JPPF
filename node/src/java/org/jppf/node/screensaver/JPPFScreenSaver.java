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

import javax.swing.JComponent;

import org.jppf.utils.TypedProperties;

/**
 * This is the interface to implement for any custom implementation of a screen saver associated with a JPPF node. 
 * @author Laurent Cohen
 */
public interface JPPFScreenSaver
{
  /**
   * Get the Swing component for this screen saver.
   * @return a {@link JComponent}.
   */
  JComponent getComponent();

  /**
   * Initialize this screen saver, and in particular its UI components.
   * @param config a copy of the JPPF configuration,
   * @param fullscreen <code>true</code> if the screen saver is to be displayed in full screen mode,
   * <code>false</code> if it is to run in windowed mode.
   */
  void init(TypedProperties config, boolean fullscreen);

  /**
   * Destroy this screen saver and release its resources.
   */
  void destroy();
}
