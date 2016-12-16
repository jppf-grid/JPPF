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

package org.jppf.admin.web.topology.nodeconfig;

import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.Model;
import org.jppf.admin.web.utils.AbstractModalForm;
import org.jppf.utils.TypedProperties;

/**
 *
 * @author Laurent Cohen
 */
public class NodeConfigForm extends AbstractModalForm {
  /**
   * Check box for whether to interrupt nodes if running.
   */
  private CheckBox interruptField;
  /**
   * Check box for whether to force the nodes to restart.
   */
  private CheckBox forceRestartField;
  /**
   * Text area for the slaves' configuration overrides.
   */
  private TextArea<String> configField;
  //private TextArea<String> configField;

  /**
   * @param modal the modal window.
   * @param okAction the ok action.
   */
  public NodeConfigForm(final ModalWindow modal, final Runnable okAction) {
    super("node_config", modal, okAction);
  }

  @Override
  protected void createFields() {
    add(forceRestartField = new CheckBox(prefix + ".force_restart.field", Model.of(false)));
    add(interruptField = new CheckBox(prefix + ".interrupt.field", Model.of(true)));
    add(configField = new TextArea<>(prefix + ".config.field", Model.of("")));
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
   * @return whether hether to force the nodes to restart.
   */
  public boolean isForceRestart() {
    return forceRestartField.getModelObject();
  }

  /**
   * Set whether to force the nodes to restart.
   * @param useOverrides whether to force the nodes to restart.
   */
  public void setForceRestart(final boolean useOverrides) {
    forceRestartField.setModel(Model.of(useOverrides));
  }

  /**
   * @return the nodes' configuration overrides.
   */
  public String getConfig() {
    return configField.getModelObject();
  }

  /**
   * Set the nodes' configuration overrides.
   * @param config the nodes' updated configuration.
   */
  public void setConfig(final String config) {
    configField.setModel(Model.of(config));
  }

  @Override
  protected void loadSettings(final TypedProperties props) {
    setInterrupt(props.getBoolean(interruptField.getId(), true));
    setForceRestart(props.getBoolean(forceRestartField.getId(), false));
    setConfig(props.getString(configField.getId(), ""));
  }

  @Override
  protected boolean saveSettings(final TypedProperties props) {
    props.setBoolean(interruptField.getId(), isInterrupt())
    .setBoolean(forceRestartField.getId(), isForceRestart())
    .setString(configField.getId(), getConfig());
    return true;
  }
}
