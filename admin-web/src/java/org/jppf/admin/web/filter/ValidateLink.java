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

package org.jppf.admin.web.filter;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.*;
import org.jppf.admin.web.JPPFWebSession;
import org.jppf.admin.web.utils.AjaxButtonWithIcon;
import org.jppf.ui.utils.NodeFilterUtils;
import org.jppf.utils.*;
import org.slf4j.*;

import com.googlecode.wicket.jquery.ui.widget.dialog.MessageDialog;

/**
 * This class represents the revert configuration button in the config panel of the admin page.
 * @author Laurent Cohen
 */
public class ValidateLink extends AjaxButtonWithIcon {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(ValidateLink.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * Initialize.
   */
  public ValidateLink() {
    super("node.filter.validate", "checkbox.png");
  }

  @Override
  public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
    if (debugEnabled) log.debug("clicked on node.filter.validate");
    NodeFilterPage page = (NodeFilterPage) target.getPage();
    TextArea<String> area = page.getPolicyField();
    String xml = area.getModelObject();
    Pair<Boolean, String> result = NodeFilterUtils.validatePolicy(xml);
    boolean valid = result.first();
    MessageDialog dialog = valid ? page.getValidDialog() : page.getErrorDialog();
    String message = LocalizationUtils.getLocalized("org.jppf.ui.i18n.FilterPanel", "node.filter." + (valid ? "valid" : "invalid") + ".message", JPPFWebSession.get().getLocale());
    if (!valid) message += "\n" + result.second();
    if (debugEnabled) log.debug("message = {}", message);
    dialog.setModelObject(message);
    dialog.open(target);
    //target.add(page);
  }
}
