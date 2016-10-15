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
public class NodeConfigForm extends Form<String> {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(NodeConfigForm.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Prefix for the ids of this form and its fields.
   */
  private static final String PREFIX = "node_config";
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

  /**
   * @param modal the modal window.
   * @param okAction the ok action.
   */
  public NodeConfigForm(final ModalWindow modal, final Runnable okAction) {
    super(PREFIX + ".form");
    add(forceRestartField = new CheckBox(PREFIX + ".force_restart.field", Model.of(false)));
    add(new Label(PREFIX + ".force_restart.label", Model.of("Force node restart")));
    add(interruptField = new CheckBox(PREFIX + ".interrupt.field", Model.of(true)));
    add(new Label(PREFIX + ".interrupt.label", Model.of("Interrupt even if running")));
    add(new Label(PREFIX + ".config.label", Model.of("Updated configuration")));
    add(configField = new TextArea<>(PREFIX + ".config.field", Model.of("Updated configuration")));
    AjaxButton okButton = new AjaxButton(PREFIX + ".ok", Model.of("OK")) {
      @Override
      protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        if (debugEnabled) log.debug("clicked on node_config.ok");
        if (okAction != null) okAction.run();
        modal.close(target);
      }
    };
    add(okButton);
    setDefaultButton(okButton);
    add(new AjaxButton(PREFIX + ".cancel", Model.of("Cancel")) {
      @Override
      protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        if (debugEnabled) log.debug("clicked on node_config.cancel");
        modal.close(target);
      }
    });
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
}
