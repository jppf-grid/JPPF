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

package org.jppf.admin.web.jobs.priority;

import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.jppf.admin.web.utils.AbstractModalForm;
import org.jppf.utils.TypedProperties;

/**
 *
 * @author Laurent Cohen
 */
public class PriorityForm extends AbstractModalForm {
  /**
   * Text field for the number of threads.
   */
  private TextField<Integer> priorityField;

  /**
   * @param modal the modal window.
   * @param okAction the ok action.
   */
  public PriorityForm(final ModalWindow modal, final Runnable okAction) {
    super("priority", modal, okAction);
  }

  @Override
  protected void createFields() {
    add(priorityField = createIntField(prefix + ".priority.field", 0, Integer.MIN_VALUE, Integer.MAX_VALUE, 1));
  }

  /**
   * @return the job(s) priority.
   */
  public int getPriority() {
    return (Integer) priorityField.getDefaultModelObject();
  }

  /**
   * Set the priority of the job(s).
   * @param priority the priority to set.
   */
  public void setPriority(final int priority) {
    priorityField.setModel(Model.of(priority));
  }

  @Override
  protected void loadSettings(final TypedProperties props) {
    setPriority(props.getInt(priorityField.getId(), 0));
  }

  @Override
  protected boolean saveSettings(final TypedProperties props) {
    props.setInt(priorityField.getId(), getPriority());
    return true;
  }
}
