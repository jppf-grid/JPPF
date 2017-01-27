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

package org.jppf.admin.web.filter;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.form.upload.*;
import org.apache.wicket.model.Model;
import org.jppf.admin.web.utils.AjaxButtonWithIcon;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class represents the upload configuration button in the config panel of the admin page.
 * @author Laurent Cohen
 */
public class UploadLink extends AjaxButtonWithIcon {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(UploadLink.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * Initialize.
   */
  public UploadLink() {
    super("node.filter.upload", "upload.png");
    setEnabled(true);
  }

  @Override
  public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
    if (debugEnabled) log.debug("clicked on node.filter.upload");
    NodeFilterPage page = (NodeFilterPage) target.getPage();
    FileUploadField fileUploadField = page.getFileUploadField();
    FileUpload fileUpload = fileUploadField.getFileUpload();
    try {
      byte[] bytes = fileUpload.getBytes();
      String s = new String(bytes, "UTF-8");
      TextArea<String> area = page.getPolicyField();
      area.setModel(Model.of(s));
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    target.add(form);
  }

  @Override
  protected void onComponentTag(final ComponentTag tag) {
    super.onComponentTag(tag);
    String style = tag.getAttributes().getString("style");
    tag.getAttributes().put("style", (style == null) ? "display: none" : style + "; display: none");
  }
}
