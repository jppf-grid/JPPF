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

package org.jppf.jmxremote.utils;

import java.util.Map;

import javax.management.remote.JMXServiceURL;

import org.jppf.nio.acceptor.AcceptorNioServer;

/**
 * 
 * @author Laurent Cohen
 */
public class JPPFJMXHelper {
  /**
   * Name of the JMX remote protocol.
   */
  public static final String PROTOCOL = "jppf";

  /**
   * Create the acceptor server based on the specified parameters.
   * @param address encapsulates the connection information.
   * @param env the JMX server environment, including eventual SSL parameters. 
   * @return a new {@link AcceptorNioServer} instance.
   */
  public static AcceptorNioServer createAcceptor(final JMXServiceURL address, Map<String, ?> env) throws Exception {
    int port = address.getPort();
    Boolean tls = (Boolean) env.get("jppf.jmx.remote.tls.enabled");
    boolean secure = (tls == null) ? false : tls;
    int[] ports = { port };
    if (secure) return new AcceptorNioServer(null, ports);
    return new AcceptorNioServer(ports, null);
  }
}
