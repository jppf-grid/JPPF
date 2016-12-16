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

package org.jppf.admin.web.layout;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.jppf.admin.web.utils.AbstractModalLink;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class SelectableLayoutLink extends AbstractModalLink<SelectableLayoutForm> {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(SelectableLayoutLink.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The selectable layout page to get the items from.
   */
  private final SelectableLayout layout;

  /**
   * @param layout the selectable layout page to get the items from.
   * @param form .
   */
  public SelectableLayoutLink(final SelectableLayout layout, final Form<String> form) {
    super("visible.items", Model.of("Visible items"), "table-column-hide.png", SelectableLayoutPage.class, form);
    this.layout = layout;
    modal.setInitialWidth(500);
    modal.setInitialHeight(300);
  }

  @Override
  protected SelectableLayoutForm createForm() {
    return new SelectableLayoutForm(layout, modal, new Runnable() { @Override public void run() { doOK(); } });
  }
  
  /**
   * Called when the ok button is clicked.
   */
  private void doOK() {
    layout.setVisibleItems(modalForm.getItems());
  }
}
