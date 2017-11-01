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

package org.jppf.jmxremote.protocol.jppf;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;

import javax.management.remote.*;

import org.jppf.jmx.*;
import org.jppf.jmxremote.JPPFJMXConnector;

/**
 *
 */
public class ClientProvider implements JMXConnectorProvider {
  /**
   * Handles the envrionment providers that allow adding to, or overriding, the environment properties
   * passed to each new JMX connector instance.  
   */
  private static final EnvironmentProviderHandler<ClientEnvironmentProvider> ENV_HANDLER = new EnvironmentProviderHandler<>(ClientEnvironmentProvider.class);

  @Override
  public JMXConnector newJMXConnector(final JMXServiceURL serviceURL, final Map<String, ?> environment) throws IOException {
    if (!JMXHelper.JPPF_JMX_PROTOCOL.equals(serviceURL.getProtocol())) throw new MalformedURLException("Protocol not " + JMXHelper.JPPF_JMX_PROTOCOL + ": " + serviceURL.getProtocol());
    Map<String, Object> env = new HashMap<>(environment);
    for (ClientEnvironmentProvider provider: ENV_HANDLER.getProviders()) {
      if (provider != null) {
        Map<String, ?> map = provider.getEnvironment();
        if ((map != null) && !map.isEmpty()) env.putAll(map);
      }
    }
    return new JPPFJMXConnector(serviceURL, env);
  }
}
