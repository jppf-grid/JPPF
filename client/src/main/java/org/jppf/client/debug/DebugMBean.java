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

package org.jppf.client.debug;

/**
 * 
 * @author Laurent Cohen
 * @since 5.0
 */
public interface DebugMBean {
  /**
   * 
   */
  String MBEAN_NAME_PREFIX = "org.jppf:name=debug,type=client,uuid=";

  /**
   * Get a list of all the driver connections.
   * @return the list of connections as a formatted string.
   */
  String allConnections();

  /**
   * 
   * @param key .
   * @return .
   */
  String getParameter(String key);

  /**
   * 
   * @param key .
   * @param value .
   */
  void setParameter(String key, String value);

  /**
   * 
   * @param key .
   */
  void removeParameter(String key);

  /**
   * 
   * @return .
   */
  String allParameters();
}
