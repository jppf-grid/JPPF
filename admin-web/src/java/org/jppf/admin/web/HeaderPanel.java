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

import java.util.Locale;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.jppf.utils.LocalizationUtils;

/**
 * A the header panel.
 * @author Laurent Cohen
 */
public class HeaderPanel extends Panel {
  /**
   *
   */
  public HeaderPanel() {
    super("jppf.header");
    String user = JPPFWebSession.getUserName();
    Locale locale = Session.get().getLocale();
    String s = (user != null)
      ? LocalizationUtils.getLocalized(getClass().getName(), "jppf.header.user.label", locale) + " " + user
      : LocalizationUtils.getLocalized(getClass().getName(), "jppf.header.not.signed_in.label", locale);
    add(new Label("jppf.header.user", Model.of(s)));
    Form<String> form = new Form<>("login.signout.form");
    AjaxButton link = new AjaxButton("login.signout.link", Model.of("Sign out")) {
      @Override
      protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        AuthenticatedWebSession.get().signOut();
        AuthenticatedWebSession.get().invalidate();
        target.add(form);
        setResponsePage(getApplication().getHomePage());
      }
    };
    form.add(link);
    form.setVisible(user != null);
    link.setVisible(user != null);
    add(form);
  }
}
