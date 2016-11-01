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

package org.jppf.admin.web.health.thresholds;

import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.jppf.admin.web.AbstractModalForm;
import org.jppf.utils.TypedProperties;

/**
 *
 * @author Laurent Cohen
 */
public class ThresholdsForm extends AbstractModalForm {
  /**
   * Field for the CPU warning alert level.
   */
  private TextField<Double> cpuWarningField;
  /**
   * Field for the CPU critical alert level.
   */
  private TextField<Double> cpuCriticalField;
  /**
   * Field for the memory warning alert level.
   */
  private TextField<Double> memoryWarningField;
  /**
   * Field for the memory critical alert level.
   */
  private TextField<Double> memoryCriticalField;

  /**
   * @param modal the modal window.
   * @param okAction the ok action.
   */
  public ThresholdsForm(final ModalWindow modal, final Runnable okAction) {
    super("thresholds", modal, okAction);
  }

  @Override
  protected void createFields() {
    add(cpuWarningField = createDecField(prefix + ".cpu.warning.field", 80d, 1d, 100d, 0.1d));
    add(cpuCriticalField = createDecField(prefix + ".cpu.critical.field", 90d, 1d, 100d, 1d));
    add(memoryWarningField = createDecField(prefix + ".memory.warning.field", 60d, 1d, 100d, 1d));
    add(memoryCriticalField = createDecField(prefix + ".memory.critical.field", 80d, 1d, 100d, 1d));
  }

  /**
   * @return the cpu % warning level.
   */
  public double getCpuWarningLevel() {
    return (Double) cpuWarningField.getDefaultModelObject();
  }

  /**
   * Set the cpu % warning level.
   * @param level the level to set.
   */
  public void setCpuWarningLevel(final double level) {
    cpuWarningField.setModel(Model.of(level));
  }

  /**
   * @return the cpu % critical level.
   */
  public double getCpuCriticalLevel() {
    return (Double) cpuCriticalField.getDefaultModelObject();
  }

  /**
   * Set the cpu % critical level.
   * @param level the level to set.
   */
  public void setCpuCriticalLevel(final double level) {
    cpuCriticalField.setModel(Model.of(level));
  }

  /**
   * @return the memory % warning level.
   */
  public double getMemoryWarningLevel() {
    return (Double) memoryWarningField.getDefaultModelObject();
  }

  /**
   * Set the memory % warning level.
   * @param level the level to set.
   */
  public void setMemoryWarningLevel(final double level) {
    memoryWarningField.setModel(Model.of(level));
  }

  /**
   * @return the memory % critical level.
   */
  public double getMemoryCriticalLevel() {
    return (Double) memoryCriticalField.getDefaultModelObject();
  }

  /**
   * Set the memory % critical level.
   * @param level the level to set.
   */
  public void setMemoryCriticalLevel(final double level) {
    memoryCriticalField.setModel(Model.of(level));
  }

  @Override
  protected void loadSettings(final TypedProperties props) {
    setCpuWarningLevel(props.getDouble(cpuWarningField.getId(), 80d));
    setCpuCriticalLevel(props.getDouble(cpuCriticalField.getId(), 90d));
    setMemoryWarningLevel(props.getDouble(memoryWarningField.getId(), 60d));
    setMemoryCriticalLevel(props.getDouble(memoryCriticalField.getId(), 80d));
  }

  @Override
  protected void saveSettings(final TypedProperties props) {
    props.setDouble(cpuWarningField.getId(), getCpuWarningLevel())
    .setDouble(cpuCriticalField.getId(), getCpuCriticalLevel())
    .setDouble(memoryWarningField.getId(), getMemoryWarningLevel())
    .setDouble(memoryCriticalField.getId(), getMemoryCriticalLevel());
  }
}
