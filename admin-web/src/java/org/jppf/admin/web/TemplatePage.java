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

import java.util.Set;

import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.apache.wicket.markup.head.*;
import org.apache.wicket.markup.html.link.*;
import org.jppf.admin.web.admin.AdminPage;
import org.jppf.admin.web.auth.JPPFRole;
import org.jppf.utils.LocalizationUtils;

/**
 * SUperclass for all pages in the web admin console (except the login page).
 * Its associated html file provides the basic layout for all other pages.
 * @author Laurent Cohen
 */
public class TemplatePage extends AbstractJPPFPage {
  /**
   * Link to the admin page. Made invisible to non-admin users.
   */
  protected Link<String> adminLink;

  /**
   *
   */
  public TemplatePage() {
    setVersioned(false);
    add(new HeaderPanel());
    add(new FooterPanel());
    adminLink =  new BookmarkablePageLink<>("jppf.admin.link", AdminPage.class);
    add(adminLink);
    JPPFWebSession session = JPPFWebSession.get();
    Roles roles = session.getRoles();
    Set<String> set = JPPFRole.getRoles(roles);
    if (!set.contains(JPPFRole.ADMIN.getRoleName())) {
      adminLink.setVisible(false);
      adminLink.setEnabled(false);
    }
  }

  @Override
  public void renderHead(final IHeaderResponse response) {
    super.renderHead(response);
    String name = getClass().getSimpleName();
    String base = getClass().getName();
    String localized = LocalizationUtils.getLocalized(base, name, null, JPPFWebSession.get().getLocale());
    if (localized == null) {
      int idx = name.lastIndexOf("Page");
      localized = (idx >= 0) ? name.substring(0, idx) : name;
    }
    StringHeaderItem item = StringHeaderItem.forString("<title>" + localized + "</title>");
    response.render(item);
  }
}
