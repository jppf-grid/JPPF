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

package org.jppf.jmx;

/**
 * 
 * @author Laurent Cohen
 */
public class JMXHelper {
  /**
   * The JMXMP protocol string.
   */
  public static final String JMXMP_PROTOCOL = "jmxmp";
  /**
   * The JPPF JMX remote protocol string.
   */
  public static final String JPPF_JMX_PROTOCOL = "jppf";
  /**
   * The protocol string for local connections.
   */
  public static final String LOCAL_PROTOCOL = "local";

  /**
   * @return an array of all available remote protocols.
   */
  public static String[] remoteProtocols() {
    return new String[] { JMXMP_PROTOCOL, JPPF_JMX_PROTOCOL };
  }
}
