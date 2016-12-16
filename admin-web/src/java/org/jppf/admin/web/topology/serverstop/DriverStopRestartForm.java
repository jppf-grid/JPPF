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

import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.Model;
import org.jppf.admin.web.utils.AbstractModalForm;
import org.jppf.utils.TypedProperties;

/**
 *
 * @author Laurent Cohen
 */
public class DriverStopRestartForm extends AbstractModalForm {
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
    super("server_stop_restart", modal, okAction);
  }

  @Override
  protected void createFields() {
    add(shutdownDelayField = createLongField(prefix + ".shutdown_delay.field", 0L, 0L, Long.MAX_VALUE, 1L));
    add(restartField = new CheckBox(prefix + ".restart.field", Model.of(true)));
    add(restartDelayField = createLongField(prefix + ".restart_delay.field", 0L, 0L, Long.MAX_VALUE, 1L));
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

  @Override
  protected void loadSettings(final TypedProperties props) {
    setShutdownDelay(props.getLong(shutdownDelayField.getId(), 0L));
    setRestart(props.getBoolean(restartField.getId(), true));
    setRestartDelay(props.getLong(restartDelayField.getId(), 0L));
  }

  @Override
  protected boolean saveSettings(final TypedProperties props) {
    props.setLong(shutdownDelayField.getId(), getShutdownDelay())
      .setBoolean(restartField.getId(), isRestart())
      .setLong(restartDelayField.getId(), getRestartDelay());
    return true;
  }
}
