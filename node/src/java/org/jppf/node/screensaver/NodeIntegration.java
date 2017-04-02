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

package org.jppf.node.screensaver;


/**
 * This interface groups two other interfaces, from which the screen saver can receive notifications from the node.
 * @author Laurent Cohen
 * @since 4.0
 */
public interface NodeIntegration extends org.jppf.node.event.NodeIntegration<JPPFScreenSaver> {
  /**
   * Provide a reference to the screen saver.
   * @param screensaver a {@link JPPFScreenSaver} instance.
   * @since 5.1
   */
  void setUiComponent(JPPFScreenSaver screensaver);

  /**
   * Provide a reference to the screen saver.
   * @param screensaver a {@link JPPFScreenSaver} instance.
   * @deprecated use {@link #setUiComponent(JPPFScreenSaver)} instead.
   */
  void setScreenSaver(JPPFScreenSaver screensaver);
}
