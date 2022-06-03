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

package org.jppf.admin.web;

import java.util.Locale;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.*;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
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
  CheckBox showIPCheckBox;

  /**
   *
   */
  public HeaderPanel() {
    super("jppf.header");
    final String user = JPPFWebSession.getSignedInUser();
    final Locale locale = Session.get().getLocale();
    final String s = (user != null)
      ? LocalizationUtils.getLocalized(getClass().getName(), "jppf.header.user.label", locale) + " " + user
      : LocalizationUtils.getLocalized(getClass().getName(), "jppf.header.not.signed_in.label", locale);
    add(new Label("jppf.header.user", Model.of(s)));
    final Form<String> form = new Form<>("login.signout.form");
    final AjaxButton link = new AjaxButton("login.signout.link", Model.of("Sign out")) {
      @Override
      protected void onSubmit(final AjaxRequestTarget target) {
        AuthenticatedWebSession.get().signOut();
        AuthenticatedWebSession.get().invalidate();
        target.add(getForm());
        setResponsePage(getApplication().getHomePage());
      }
    };

    form.add(link);
    showIPCheckBox = new AjaxCheckBox("jppf.header.show.ip", Model.of(JPPFWebSession.get().isShowIP())) {
      @Override
      protected void onUpdate(final AjaxRequestTarget target) {
        final Boolean newSelection = this.getModelObject();
        JPPFWebSession.get().setShowIP((newSelection != null) && newSelection);
      }
    };
    form.add(showIPCheckBox);
    form.setVisible(user != null);
    link.setVisible(user != null);
    add(form);
  }

  /**
   * @return the check for show IP flag.
   */
  public CheckBox getShowIPCheckBox() {
    return showIPCheckBox;
  }
}
