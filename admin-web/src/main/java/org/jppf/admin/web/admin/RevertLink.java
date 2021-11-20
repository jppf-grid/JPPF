/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.admin.web.admin;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.Model;
import org.jppf.admin.web.JPPFWebConsoleApplication;
import org.slf4j.*;

/**
 * This class represents the revert configuration button in the config panel of the admin page.
 * @author Laurent Cohen
 */
public class RevertLink extends AbstractAdminLink {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(RevertLink.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize.
   * @param type the type of config panel to add this button to.
   */
  public RevertLink(final ConfigType type) {
    super(type, AdminConfigConstants.REVERT_ACTION, "revert.png");
  }

  @Override
  public void onSubmit(final AjaxRequestTarget target) {
    if (debugEnabled) log.debug("clicked on {}.revert", type.getPrefix());
    final TextArea<String> area = ((AdminPage) target.getPage()).getConfigPanel(type).getConfig();
    area.setModel(Model.of(JPPFWebConsoleApplication.get().getConfig(type).getProperties().asString()));
    target.add(getForm());
  }
}
