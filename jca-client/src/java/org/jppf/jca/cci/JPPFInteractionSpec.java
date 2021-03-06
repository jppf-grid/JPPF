/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.jca.cci;

import java.beans.*;

import javax.resource.cci.InteractionSpec;

/**
 * Implementation of the InteractionSpec interface.
 * @author Laurent Cohen
 * @exclude
 */
public class JPPFInteractionSpec implements InteractionSpec {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Support for java bean properties changes.
   */
  private final PropertyChangeSupport support = new PropertyChangeSupport(this);

  /**
   * Name of the function associated with this interaction.
   */
  private String functionName;

  /**
   * Initialize this interaction spec with the specified function.
   * @param functionName the name of the function.
   */
  public JPPFInteractionSpec(final String functionName) {
    this.functionName = functionName;
  }

  /**
   * Add a property change listener.
   * @param listener the listener to add.
   */
  public void addPropertyChangeListener(final PropertyChangeListener listener) {
    support.addPropertyChangeListener(listener);
  }

  /**
   * Remove a property change listener.
   * @param listener the listener to add.
   */
  public void removePropertyChangeListener(final PropertyChangeListener listener) {
    support.removePropertyChangeListener(listener);
  }

  /**
   * Get the name of the function associated with this interaction spec.
   * @return the function name as a string.
   */
  public String getFunctionName() {
    return functionName;
  }

  /**
   * Set the name of the function associated with this interaction spec.
   * @param newFunctionName the function name as a string.
   */
  public void setFunctionName(final String newFunctionName) {
    final String oldFunctionName = functionName;
    functionName = newFunctionName;
    support.firePropertyChange("functionName", oldFunctionName, newFunctionName);
  }
}
