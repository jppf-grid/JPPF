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

package org.jppf.admin.web;

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.model.IModel;

/**
 *
 * @author Laurent Cohen
 */
public class ActionButton extends Button {
  /**
   * The action to run when clicked.
   */
  private final Runnable action;

  /**
   *
   * @param id the id of this button.
   * @param model this buttons model.
   * @param action the action to run when clicked.
   */
  public ActionButton(final String id, final IModel<String> model, final Runnable action) {
    super(id, model);
    this.action = action;
  }

  @Override
  public void onSubmit() {
    if (action != null) action.run();
  }
}
