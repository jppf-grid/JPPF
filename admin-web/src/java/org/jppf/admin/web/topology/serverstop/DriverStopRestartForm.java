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

package org.jppf.admin.web.topology.serverstop;

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
public class DriverStopRestartForm extends Form<String> {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(DriverStopRestartForm.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Prefix for the ids of this form and its fields.
   */
  private static final String PREFIX = "server_stop_restart";
  /**
   * Text field for the driver shutdown delay.
   */
  private TextField<Long> shutdownDelayField;
  /**
   * Text field for the driver restart delay.
   */
  private TextField<Long> restartDelayField;
  /**
   * Check box for whether to restart the driver.
   */
  private CheckBox restartField;

  /**
   * @param modal the modal window.
   * @param okAction the ok action.
   */
  public DriverStopRestartForm(final ModalWindow modal, final Runnable okAction) {
    super(PREFIX + ".form");
    add(new Label(PREFIX + ".shutdown_delay.label", Model.of("Shutdown delay")));
    add(shutdownDelayField = new TextField<>(PREFIX + ".shutdown_delay.field", Model.of(0L)));
    add(new Label(PREFIX + ".restart.label", Model.of("Restart")));
    add(restartField = new CheckBox(PREFIX + ".restart.field", Model.of(true)));
    add(new Label(PREFIX + ".restart_delay.label", Model.of("Restart delay")));
    add(restartDelayField = new TextField<>(PREFIX + ".restart_delay.field", Model.of(0L)));
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
  public long getShutdownDelay() {
    return shutdownDelayField.getModelObject();
  }

  /**
   * Set the number of slaves.
   * @param shutdownDelay the number of slaves to set.
   */
  public void setShutdownDelay(final long shutdownDelay) {
    this.shutdownDelayField.setModel(Model.of(shutdownDelay));
  }

  /**
   * @return whether to interrupt nodes if running.
   */
  public boolean isRestart() {
    return restartField.getModelObject();
  }

  /**
   * Set whether to interrupt nodes if running.
   * @param interrupt whether to interrupt nodes if running.
   */
  public void setRestart(final boolean interrupt) {
    this.restartField.setModel(Model.of(interrupt));
  }

  /**
   * @return the driver restart delay.
   */
  public long getRestartDelay() {
    return restartDelayField.getModelObject();
  }

  /**
   * Set the driver restart delay.
   * @param restartDelay the driver restart delay.
   */
  public void setRestartDelay(final long restartDelay) {
    this.restartDelayField.setModel(Model.of(restartDelay));
  }
}
