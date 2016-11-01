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

package org.jppf.admin.web.jobs.maxnodes;

import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.Model;
import org.jppf.admin.web.AbstractModalForm;
import org.jppf.utils.TypedProperties;

/**
 *
 * @author Laurent Cohen
 */
public class MaxNodesForm extends AbstractModalForm {
  /**
   * Text field for the number of threads.
   */
  private TextField<Integer> nbNodesField;
  /**
   * Check box for whether to interrupt nodes if running.
   */
  private CheckBox unlimitedField;

  /**
   * @param modal the modal window.
   * @param okAction the ok action.
   */
  public MaxNodesForm(final ModalWindow modal, final Runnable okAction) {
    super("max_nodes", modal, okAction);
  }

  @Override
  protected void createFields() {
    add(nbNodesField = createIntField(prefix + ".nb_nodes.field", Integer.MAX_VALUE, 1, Integer.MAX_VALUE, 1));
    add(unlimitedField = new CheckBox(prefix + ".unlimited.field", Model.of(true)));
  }

  /**
   * @return the max number of nodes.
   */
  public int getNbNodes() {
    return (Integer) nbNodesField.getDefaultModelObject();
  }

  /**
   * Set the max number of nodes.
   * @param nbNodes the number of nodes to set.
   */
  public void setNbNodes(final int nbNodes) {
    nbNodesField.setModel(Model.of(nbNodes));
  }

  /**
   * @return whether the max number of nodes is unlimited.
   */
  public boolean isUnlimited() {
    return unlimitedField.getModelObject();
  }

  /**
   * Set whether the max number of nodes is unlimited.
   * @param unlimited whether to interrupt nodes if running.
   */
  public void setUnlimited(final boolean unlimited) {
    unlimitedField.setModel(Model.of(unlimited));
  }

  @Override
  protected void loadSettings(final TypedProperties props) {
    setNbNodes(props.getInt(nbNodesField.getId(), Integer.MAX_VALUE));
    setUnlimited(props.getBoolean(unlimitedField.getId(), true));
  }

  @Override
  protected void saveSettings(final TypedProperties props) {
    props.setInt(nbNodesField.getId(), getNbNodes()).setBoolean(unlimitedField.getId(), isUnlimited());
  }
}
