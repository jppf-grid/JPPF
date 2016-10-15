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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.Model;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class ProvisioningForm extends Form<String> {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(ProvisioningForm.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Prefix for the ids of this form and its fields.
   */
  private static final String PREFIX = "provisioning";
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
    super(PREFIX + ".form");
    add(new Label(PREFIX + ".nb_slaves.label", Model.of("Number of slaves")));
    add(nbSlavesField = new TextField<>(PREFIX + ".nb_slaves.field", Model.of(0)));
    add(new Label(PREFIX + ".interrupt.label", Model.of("Interrupt even if running")));
    add(interruptField = new CheckBox(PREFIX + ".interrupt.field", Model.of(true)));
    add(new Label(PREFIX + ".use_overrides.label", Model.of("Use configuration overrides")));
    add(useOverridesField = new CheckBox(PREFIX + ".use_overrides.field", Model.of(false)));
    add(new Label(PREFIX + ".overrides.label", Model.of("Configuration overrides")));
    add(overridesField = new TextArea<>(PREFIX + ".overrides.field", Model.of("")));
    AjaxButton okButton = new AjaxButton(PREFIX + ".ok", Model.of("OK")) {
      @Override
      protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        if (debugEnabled) log.debug("clicked on provisioning.ok");
        if (okAction != null) okAction.run();
        modal.close(target);
      }
    };
    add(okButton);
    setDefaultButton(okButton);
    add(new AjaxButton(PREFIX + ".cancel", Model.of("Cancel")) {
      @Override
      protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        if (debugEnabled) log.debug("clicked on provisioning.cancel");
        modal.close(target);
      }
    });
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
}
