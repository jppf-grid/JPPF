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

package org.jppf.admin.web.auth;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.cycle.RequestCycle;
import org.jppf.admin.web.*;
import org.slf4j.*;
import org.wicketstuff.wicket.mount.core.annotation.MountPath;

/**
 * Login page class.
 * @author Laurent Cohen
 */
@MountPath("login.html")
public class LoginPage extends WebPage {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(LoginPage.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * 
   */
  public LoginPage() {
    setVersioned(false);
    add(new HeaderPanel());
    add(new LoginForm());
    add(new FooterPanel());
  }

  @Override
  protected void onConfigure() {
    super.onConfigure();
    if (debugEnabled) log.debug("in onConfigure() for page {} ==> '{}'", getClass().getSimpleName(), RequestCycle.get().urlFor(getClass(), null));
  }
}

