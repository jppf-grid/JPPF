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

package org.jppf.management;

import org.jppf.utils.LoggingUtils;
import org.jppf.utils.configuration.JPPFProperty;
import org.slf4j.*;

/**
 * This class provides factory methods to create rmeorte JMX connector servers.
 * @author Laurent Cohen
 * @exclude
 */
public class JMXServerFactory
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JMXServerFactory.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * Create a JMXServer instance based on the specified parameters.
   * @param uuid the server's unique identifier.
   * @param ssl specifies whether JMX should be used over an SSL/TLS connection.
   * @param portProperty an ordered set of configuration properties to use for looking up the desired management port.
   * @return an instance of {@link JMXServer}.
   * @throws Exception if the server could not be created.
   */
  public static JMXServer createServer(final String uuid, final boolean ssl, final JPPFProperty<Integer> portProperty) throws Exception
  {
    JMXServer server = new JMXMPServer(uuid, ssl, portProperty);
    if (debugEnabled) log.debug("created JMX server: " + server);
    return server;
  }
}
