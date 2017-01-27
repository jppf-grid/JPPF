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

package javax.management.remote.generic;

import java.util.Map;

/**
 * Implementations of this interface provide environment properties to add to or override those passed to
 * {@link javax.management.remote.JMXConnectorFactory#newJMXConnector(javax.management.remote.JMXServiceURL, Map)
 * JMXConnectorFactory.newJMXConnector()} when the client side of a JMX connection is attempted.
 * @author Laurent Cohen
 */
public interface ClientEnvironmentProvider {
  /**
   * Get a set of environment properties add to or override those passed to each new client-side connection.
   * @return an environment map.
   */
  Map<String, ?> getEnvironment();
}
