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

package org.jppf.ui.actions;

import java.util.List;

/**
 * Manages the state of actions associated to toolbar buttons and right-click menu items.
 * @author Laurent Cohen
 */
public interface ActionHandler
{
  /**
   * Add an action with the specified name to this action manager.
   * @param name the name of the action to add.
   * @param action the action to add.
   */
  void putAction(String name, UpdatableAction action);
  /**
   * Get the action with the specified name.
   * @param name the name of the action to find.
   * @return an <code>Action</code> or null if the specified name could not be found.
   */
  UpdatableAction getAction(String name);
  /**
   * Get the selected elements in the component handled by this action manager.
   * @return a list of objects.
   */
  List<Object> getSelectedElements();
  /**
   * Update the state of all actions registered with this <code>ActionHandler</code>.
   */
  void updateActions();
}
