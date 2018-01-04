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

import java.lang.management.ManagementFactory;
import java.util.*;

import javax.management.remote.generic.ObjectWrapping;

import org.jppf.jmx.JMXHelper;
import org.jppf.ssl.SSLHelper;
import org.jppf.utils.*;
import org.jppf.utils.configuration.*;
import org.slf4j.*;

/**
 * Wrapper around the JMXMP remote connector server implementation.
 * @author Laurent Cohen
 * @exclude
 */
public class JMXMPServer extends AbstractJMXServer {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JMXMPServer.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * An ordered set of configuration properties to use for looking up the desired management port.
   */
  private final JPPFProperty<Integer> portProperty;

  /**
   * Initialize this JMX server with the specified uuid.
   * @param id the unique id of the driver or node holding this jmx server.
   * @param ssl specifies whether JMX should be used over an SSL/TLS connection.
   * @param portProperty an ordered set of configuration properties to use for looking up the desired management port.
   * @exclude
   */
  public JMXMPServer(final String id, final boolean ssl, final JPPFProperty<Integer> portProperty) {
    this.uuid = id;
    this.ssl = ssl;
    if (portProperty == null) this.portProperty = ssl ? JPPFProperties.MANAGEMENT_SSL_PORT : JPPFProperties.MANAGEMENT_SSL_PORT;
    else this.portProperty = portProperty;
  }

  /**
   * @exclude
   */
  @Override
  public void start(final ClassLoader cl) throws Exception {
    if (debugEnabled) log.debug("starting remote connector server");
    final ClassLoader tmp = Thread.currentThread().getContextClassLoader();
    lock.lock();
    try {
      Thread.currentThread().setContextClassLoader(cl);
      mbeanServer = ManagementFactory.getPlatformMBeanServer();
      final TypedProperties config = JPPFConfiguration.getProperties();
      managementPort = config.get(portProperty);
      if (debugEnabled) log.debug("managementPort={}, portProperties={}", managementPort, Arrays.asList(portProperty));
      final Map<String, Object> env = new HashMap<>();
      env.put("jmx.remote.default.class.loader", cl);
      env.put("jmx.remote.protocol.provider.class.loader", cl);
      env.put("jmx.remote.x.server.max.threads", 1);
      env.put("jmx.remote.x.client.connection.check.period", 0);
      // remove the "JMX server connection timeout Thread-*" threads. See bug http://www.jppf.org/tracker/tbg/jppf/issues/JPPF-249
      env.put("jmx.remote.x.server.connection.timeout", Long.MAX_VALUE);
      if (ssl) SSLHelper.configureJMXProperties(JMXHelper.JMXMP_PROTOCOL, env);
      env.put("jmx.remote.object.wrapping", newObjectWrapping());
      startConnectorServer("jmxmp", env);
    } finally {
      lock.unlock();
      Thread.currentThread().setContextClassLoader(tmp);
    }
  }

  /**
   * @return a new instance of an implementation of {@code ObjectWrapping}.
   */
  public static ObjectWrapping newObjectWrapping() {
    //return new CustomWrapping();
    return new CustomWrapping2();
  }
}
