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

package org.jppf.admin.web;

import java.util.Set;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.apache.wicket.markup.head.*;
import org.apache.wicket.markup.html.link.*;
import org.jppf.admin.web.admin.AdminPage;
import org.jppf.admin.web.auth.*;
import org.jppf.admin.web.filter.NodeFilterPage;
import org.jppf.admin.web.health.HealthPage;
import org.jppf.admin.web.jobs.JobsPage;
import org.jppf.admin.web.settings.UserSettings;
import org.jppf.admin.web.stats.StatisticsPage;
import org.jppf.admin.web.topology.TopologyPage;
import org.jppf.utils.LocalizationUtils;

/**
 * SUperclass for all pages in the web admin console (except the login page).
 * Its associated html file provides the basic layout for all other pages.
 * @author Laurent Cohen
 */
public class TemplatePage extends AbstractJPPFPage {
  /**
   * Link to the node filter. Its color changes based on whether it is active (green) or inactive (red).
   */
  protected Link<String> nodeFilterLink;

  /**
   *
   */
  public TemplatePage() {
    setVersioned(false);
    final HeaderPanel hp = new HeaderPanel();
    add(hp);
    setTooltip(hp.getShowIPCheckBox(), HeaderPanel.class.getName());
    add(new FooterPanel());
    final JPPFWebSession session = JPPFWebSession.get();
    final Roles roles = session.getRoles();
    final Set<String> set = JPPFRole.getRoles(roles);
    addWithRoles("jppf.admin.link", AdminPage.class, set, JPPFRoles.ADMIN);
    addWithRoles("jppf.topology.link", TopologyPage.class, set, JPPFRoles.MONITOR, JPPFRoles.MANAGER);
    addWithRoles("jppf.health.link", HealthPage.class, set, JPPFRoles.MONITOR, JPPFRoles.MANAGER);
    addWithRoles("jppf.jobs.link", JobsPage.class, set, JPPFRoles.MONITOR, JPPFRoles.MANAGER);
    addWithRoles("jppf.stats.link", StatisticsPage.class, set, JPPFRoles.MONITOR, JPPFRoles.MANAGER);
    nodeFilterLink =  addWithRoles("jppf.filter.link", NodeFilterPage.class, set, JPPFRoles.MONITOR, JPPFRoles.MANAGER);
    final UserSettings settings = session.getUserSettings();
    if (getClass() != NodeFilterPage.class) {
      final boolean active = settings.getProperties().getBoolean(JPPFWebSession.NODE_FILTER_ACTIVE_PROP, false);
      nodeFilterLink.add(new AttributeModifier("style", "color: " + (active ? "green" : "red")));
    }
  }

  @Override
  public void renderHead(final IHeaderResponse response) {
    super.renderHead(response);
    final String name = getClass().getSimpleName();
    final String base = getClass().getName();
    String localized = LocalizationUtils.getLocalized(base, name, null, JPPFWebSession.get().getLocale());
    if (localized == null) {
      final int idx = name.lastIndexOf("Page");
      localized = (idx >= 0) ? name.substring(0, idx) : name;
    }
    final StringHeaderItem item = StringHeaderItem.forString("<title>" + localized + "</title>");
    response.render(item);
  }

  /**
   * Create a bookmarkable link and set its visibility based on whether the user is in one of the specified roles.
   * @param id the id of the link to create.
   * @param clazz the class of the page of the link.
   * @param userRoles the roles of the currently logged-in user.
   * @param visibleRoles the roles in which the component is visible.
   * @return the component, for method chaining.
   */
  private BookmarkablePageLink<String> addWithRoles(final String id, final Class<? extends TemplatePage> clazz, final Set<String> userRoles, final String...visibleRoles) {
    final BookmarkablePageLink<String> link = new BookmarkablePageLink<>(id, clazz);
    boolean show = false;
    for (final String role: visibleRoles) {
      if (userRoles.contains(role)) {
        show = true;
        break;
      }
    }
    if (!show) {
      link.setVisible(false);
      link.setEnabled(false);
    }
    add(link);
    // highlight the current page link
    if (clazz == getClass()) {
      link.add(new AttributeModifier("style", "color: #6D78B6; background-color: #C5D0F0"));
      link.setEnabled(false);
    }
    return link;
  }
}
