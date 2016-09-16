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

package org.jppf.admin.web.tabletree;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.IModel;
import org.jppf.admin.web.*;

/**
 *
 * @author Laurent Cohen
 */
public abstract class AbstractActionLink extends AjaxLink<String> {
  /**
   *
   * @param id the lnk id.
   */
  public AbstractActionLink(final String id) {
    super(id);
  }

  /**
   *
   * @param id the lnk id.
   * @param model the display model.
   */
  public AbstractActionLink(final String id, final IModel<String> model) {
    super(id, model);
  }

  /**
   * Get the session from the ajax target.
   * @param target the ajax target.
   * @return a {@link JPPFWebSession} instance.
   */
  protected JPPFWebSession getSession(final AjaxRequestTarget target) {
    TemplatePage page = (TemplatePage) target.getPage();
    return page.getJPPFSession();
  }
}
