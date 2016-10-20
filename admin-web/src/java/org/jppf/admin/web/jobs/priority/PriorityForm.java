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
public class PriorityForm extends Form<String> {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(PriorityForm.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Prefix for the ids of this form and its fields.
   */
  private static final String PREFIX = "priority";
  /**
   * Text field for the number of threads.
   */
  private TextField<Integer> priorityField;

  /**
   * @param modal the modal window.
   * @param okAction the ok action.
   */
  public PriorityForm(final ModalWindow modal, final Runnable okAction) {
    super(PREFIX + ".form");
    add(new Label(PREFIX + ".priority.label", Model.of("Job priority")));
    add(priorityField = new TextField<>(PREFIX + ".priority.field", Model.of(0)));
    AjaxButton okButton = new AjaxButton(PREFIX + ".ok", Model.of("OK")) {
      @Override
      protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        if (debugEnabled) log.debug("clicked on node_threads.ok");
        if (okAction != null) okAction.run();
        modal.close(target);
      }
    };
    add(okButton);
    setDefaultButton(okButton);
    add(new AjaxButton(PREFIX + ".cancel", Model.of("Cancel")) {
      @Override
      protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        if (debugEnabled) log.debug("clicked on node_threads.cancel");
        modal.close(target);
      }
    });
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
}
