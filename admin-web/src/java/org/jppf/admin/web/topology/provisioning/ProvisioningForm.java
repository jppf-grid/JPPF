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

package org.jppf.admin.web.topology.provisioning;

import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.Model;
import org.jppf.admin.web.utils.AbstractModalForm;
import org.jppf.utils.TypedProperties;

/**
 *
 * @author Laurent Cohen
 */
public class ProvisioningForm extends AbstractModalForm {
  /**
   * Text field for the number of slaves.
   */
  private TextField<Integer> nbSlavesField;
  /**
   * Check box for whether to interrupt nodes if running.
   */
  private CheckBox interruptField;
  /**
   * Check box for whether the slaves use configuration overrides.
   */
  private CheckBox useOverridesField;
  /**
   * Text area for the slaves' configuration overrides.
   */
  private TextArea<String> overridesField;

  /**
   * @param modal the modal window.
   * @param okAction the ok action.
   */
  public ProvisioningForm(final ModalWindow modal, final Runnable okAction) {
    super("provisioning", modal, okAction);
  }

  @Override
  protected void createFields() {
    add(nbSlavesField = createIntField(prefix + ".nb_slaves.field", 0, 0, 1024, 1));
    add(interruptField = new CheckBox(prefix + ".interrupt.field", Model.of(true)));
    add(useOverridesField = new CheckBox(prefix + ".use_overrides.field", Model.of(false)));
    add(overridesField = new TextArea<>(prefix + ".overrides.field", Model.of("")));
  }

  /**
   * @return the number of slaves.
   */
  public int getNbSlaves() {
    return (Integer) nbSlavesField.getDefaultModelObject();
  }

  /**
   * Set the number of slaves.
   * @param nbSlaves the number of slaves to set.
   */
  public void setNbSlaves(final int nbSlaves) {
    nbSlavesField.setModel(Model.of(nbSlaves));
  }

  /**
   * @return whether to interrupt nodes if running.
   */
  public boolean isInterrupt() {
    return interruptField.getModelObject();
  }

  /**
   * Set whether to interrupt nodes if running.
   * @param interrupt whether to interrupt nodes if running.
   */
  public void setInterrupt(final boolean interrupt) {
    interruptField.setModel(Model.of(interrupt));
  }

  /**
   * @return whether the slaves use configuration overrides.
   */
  public boolean isUseOverrides() {
    return useOverridesField.getModelObject();
  }

  /**
   * Set whether the slaves use configuration overrides.
   * @param useOverrides whether the slaves use configuration overrides.
   */
  public void setUseOverrides(final boolean useOverrides) {
    useOverridesField.setModel(Model.of(useOverrides));
  }

  /**
   * @return the slaves' configuration overrides.
   */
  public String getOverrides() {
    return overridesField.getDefaultModelObjectAsString();
  }

  /**
   * Set the slaves' configuration overrides.
   * @param overrides the slaves' configuration overrides.
   */
  public void setOverrides(final String overrides) {
    overridesField.setModel(Model.of(overrides));
  }

  @Override
  protected void loadSettings(final TypedProperties props) {
    setNbSlaves(props.getInt(nbSlavesField.getId(), 0));
    setInterrupt(props.getBoolean(interruptField.getId(), true));
    setUseOverrides(props.getBoolean(useOverridesField.getId(), false));
    setOverrides(props.getString(overridesField.getId(), ""));
  }

  @Override
  protected void saveSettings(final TypedProperties props) {
    props.setInt(nbSlavesField.getId(), getNbSlaves())
      .setBoolean(interruptField.getId(), isInterrupt())
      .setBoolean(useOverridesField.getId(), isUseOverrides())
      .setString(overridesField.getId(), getOverrides());
  }
}
