/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

import java.util.*;

/**
 * Abstract implementation of the <code>ActionManager</code> interface.
 * @author Laurent Cohen
 */
public abstract class AbstractActionHandler implements ActionHandler
{
  /**
   * Mapping of actions to their name.
   */
  protected Map<String, UpdatableAction> actionMap = new HashMap<String, UpdatableAction>();
  /**
   * List of elements selected in the managed component.
   */
  protected List<Object> selectedElements = new LinkedList<Object>();

  /**
   * Add an action with the specified name to this action manager.
   * @param name the name of the action to add.
   * @param action the action to add.
   * @see org.jppf.ui.actions.ActionHandler#putAction(java.lang.String, org.jppf.ui.actions.UpdatableAction)
   */
  @Override
  public void putAction(final String name, final UpdatableAction action)
  {
    actionMap.put(name, action);
  }

  /**
   * Get the action with the specified name.
   * @param name the name of the action to find.
   * @return an <code>Action</code> or null if the specified name could not be found.
   * @see org.jppf.ui.actions.ActionHandler#getAction(java.lang.String)
   */
  @Override
  public UpdatableAction getAction(final String name)
  {
    return actionMap.get(name);
  }

  /**
   * Get the selected elements in the JTreeTable handled by this action manager.
   * @return a list of objects.
   * @see org.jppf.ui.actions.ActionHandler#getSelectedElements()
   */
  @Override
  public synchronized List<Object> getSelectedElements()
  {
    return selectedElements;
  }

  /**
   * Update the state of all actions registered with this <code>ActionHandler</code>.
   * @see org.jppf.ui.actions.ActionHandler#updateActions()
   */
  @Override
  public synchronized void updateActions()
  {
    for (UpdatableAction action: actionMap.values()) action.updateState(selectedElements);
  }
}
