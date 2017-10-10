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

package org.jppf.jmxremote;

import java.io.*;
import java.util.*;

import javax.management.MBeanServer;
import javax.management.remote.*;

/**
 * 
 * @author Laurent Cohen
 */
public class JPPFJMXConnectorServer extends JMXConnectorServer {
  /**
   * The environment for this connector.
   */
  private final Map<String, ?> environment;
  /**
   * The address of this connector.
   */
  private final JMXServiceURL address;

  /**
   * 
   * @param serviceURL the address of this connector.
   * @param environment the environment for this connector.
   * @param mbeanServer .
   */
  public JPPFJMXConnectorServer(final JMXServiceURL serviceURL, final Map<String, ?> environment, MBeanServer mbeanServer) {
    super(mbeanServer);
    this.environment = (environment == null) ? new HashMap<String, Object>() : environment;
    this.address = serviceURL;
  }

  @Override
  public void start() throws IOException {
  }

  @Override
  public void stop() throws IOException {
  }

  @Override
  public boolean isActive() {
    return false;
  }

  @Override
  public JMXServiceURL getAddress() {
    return address;
  }

  @Override
  public Map<String, ?> getAttributes() {
    Map<String, Object> map = new HashMap<>();
    for (Map.Entry<String, ?> entry: environment.entrySet()) {
      if (entry.getValue() instanceof Serializable) map.put(entry.getKey(), entry.getValue());
    }
    return Collections.unmodifiableMap(map);
  }
}
