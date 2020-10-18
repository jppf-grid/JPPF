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

package org.jppf.example.interceptor.auth;

import java.util.Collections;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;

/**
 * A very simple {@link Configuration} implementation that we can use instead of configuring a jaas.config file.
 * @author Laurent Cohen
 */
public class JPPFJaasConfiguration extends Configuration {
  /**
   * We have a single configuration entry that we use as a singleton.
   */
  private final AppConfigurationEntry[] ENTRIES = {
    new AppConfigurationEntry(InterceptorLoginModule.class.getName(), LoginModuleControlFlag.REQUIRED, Collections.emptyMap())
  };

  @Override
  public AppConfigurationEntry[] getAppConfigurationEntry(final String name) {
    return ("NetworkInterceptorDemo".equals(name)) ? ENTRIES : null;
  }
}
