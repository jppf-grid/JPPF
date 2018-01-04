/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

package org.jppf.admin.web.topology.loadbalancing;

import java.util.*;

import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.Model;
import org.jppf.admin.web.JPPFWebSession;
import org.jppf.admin.web.utils.AbstractModalForm;
import org.jppf.utils.*;

/**
 *
 * @author Laurent Cohen
 */
public class LoadBalancingForm extends AbstractModalForm {
  /**
   * Contains the name of the load-balancing algorithm.
   */
  private DropDownChoice<String> algorithmField;
  /**
   * Text area for the slaves' configuration overrides.
   */
  private TextArea<String> propertiesField;
  /**
   * The label containing the name of the driver.
   */
  private Label driverNameLabel;

  /**
   * @param modal the modal window.
   * @param okAction the ok action.
   */
  public LoadBalancingForm(final ModalWindow modal, final Runnable okAction) {
    super("load_balancing", modal, okAction);
  }

  @Override
  protected void createFields() {
    add(driverNameLabel = new Label(prefix + ".caption", Model.of("")));
    add(algorithmField = new DropDownChoice<>(prefix + ".algorithm.field"));
    add(propertiesField = new TextArea<>(prefix + ".properties.field", Model.of("")));
  }

  /**
   * @param driverName the driver name.
   */
  public void setDriverName(final String driverName) {
    final String key = driverNameLabel.getId() + ".label";
    final String caption = LocalizationUtils.getLocalized(LoadBalancingPage.class.getName(), key, key, JPPFWebSession.get().getLocale(), driverName);
    driverNameLabel.setDefaultModel(Model.of(caption));
  }

  /**
   * @return the selected algorithm.
   */
  public String getAlgorithm() {
    return algorithmField.getModelObject();
  }

  /**
   * @param algorithm the current algorithm.
   */
  public void setAlgorithm(final String algorithm) {
    algorithmField.setModel(Model.of(algorithm));
  }

  /**
   * @return the available algorithms.
   */
  public List<String> getAlgorithmChoices() {
    return new ArrayList<>(algorithmField.getChoices());
  }

  /**
   * @param algorithms the currently available algorithms.
   */
  public void setAlgorithmChoices(final List<String> algorithms) {
    algorithmField.setChoices(algorithms);
  }

  /**
   * @return the driver's load-balancing properties.
   */
  public String getProperties() {
    return propertiesField.getModelObject();
  }

  /**
   * Set the driver's load-balancing properties.
   * @param config the driver's load-balancing properties.
   */
  public void setProperties(final String config) {
    propertiesField.setModel(Model.of(config));
  }

  @Override
  protected void loadSettings(final TypedProperties props) {
  }

  @Override
  protected boolean saveSettings(final TypedProperties props) {
    return false;
  }
}
