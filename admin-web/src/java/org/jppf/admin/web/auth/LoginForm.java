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

package org.jppf.admin.web.auth;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.Model;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class LoginForm extends Form<String> {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(LoginForm.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Prefix for the ids of this form and its fields.
   */
  private static final String PREFIX = "login";
  /**
   * Text field for the number of threads.
   */
  private TextField<String> username;
  /**
   * Text field for the number of threads.
   */
  private PasswordTextField password;
  /**
   * The eventual error message.
   */
  private Label error;
  /**
   * 
   */
  private boolean hasError;

  /**
   * .
   */
  public LoginForm() {
    super(PREFIX + ".form");
    add(username = new TextField<>(PREFIX + ".username.field", Model.of("")));
    add(password = new PasswordTextField(PREFIX + ".password.field", Model.of("")));
    password.setRequired(false);
    add(error = new Label(PREFIX + ".error", Model.of("")) {

      @Override
      protected void onComponentTag(final ComponentTag tag) {
        if (hasError) tag.append("style", "margin-top: 15px", ";");
      }
    });
    AjaxButton button = new AjaxButton(PREFIX + ".ok") {
      @Override
      protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        if (debugEnabled) log.debug("clicked on login.ok");
        doOK(target);
      }
    };
    add(button);
    setDefaultButton(button);
  }

  /**
   * @return the number of threads.
   */
  public String getUsername() {
    return username.getModelObject();
  }

  /**
   * Set the user name.
   * @param username the user name to set..
   */
  public void setUsername(final String username) {
    this.username.setModel(Model.of(username));
  }

  /**
   * @return the threads priority.
   */
  public String getPassword() {
    return password.getModelObject();
  }

  /**
   * Set the threads priority.
   * @param password the password.
   */
  public void setPpassword(final String password) {
    this.password.setModel(Model.of(password));
  }

  /**
   * @param target .
   */
  private void doOK(final AjaxRequestTarget target) {
    if (AuthenticatedWebSession.get().signIn(getUsername(), getPassword())) {
      continueToOriginalDestination();
      setResponsePage(getApplication().getHomePage());
      hasError = false;
    } else {
      String message = LocalizationUtils.getLocalized(LoginPage.class.getName(), "login.error", Session.get().getLocale());
      this.error.setDefaultModel(Model.of(message));
      target.add(this.getParent());
      hasError = true;
    }
  }
}
