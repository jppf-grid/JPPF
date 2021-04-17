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

package org.jppf.management;

import java.lang.reflect.*;
import java.util.*;

import javax.management.MBeanServer;

import org.jppf.JPPFUnsupportedOperationException;
import org.jppf.comm.socket.BootstrapObjectSerializer;
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
  private static final Logger log = LoggerFactory.getLogger(JMXMPServer.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether trace log statements are enabled.
   */
  private static final boolean traceEnabled = log.isTraceEnabled();
  /**
   * 
   */
  static final ObjectWrappingInvocationHandler objectWrappingInvocationHandler = new ObjectWrappingInvocationHandler();
  /**
   * An ordered set of configuration properties to use for looking up the desired management port.
   */
  private final JPPFProperty<Integer> portProperty;

  /**
   * Initialize this JMX server with the specified uuid.
   * @param config the configuration to use.
   * @param id the unique id of the driver or node holding this jmx server.
   * @param ssl specifies whether JMX should be used over an SSL/TLS connection.
   * @param portProperty an ordered set of configuration properties to use for looking up the desired management port.
   * @param mbeanServer the mbean server to use.
   * @exclude
   */
  public JMXMPServer(final TypedProperties config, final String id, final boolean ssl, final JPPFProperty<Integer> portProperty, final MBeanServer mbeanServer) {
    super(config, mbeanServer);
    this.uuid = id;
    this.ssl = ssl;
    if (portProperty == null) this.portProperty = ssl ? JPPFProperties.MANAGEMENT_SSL_PORT : JPPFProperties.MANAGEMENT_SSL_PORT;
    else this.portProperty = portProperty;
  }

  /**
   * Initialize this JMX server with the specified uuid.
   * @param config the configuration to use.
   * @param id the unique id of the driver or node holding this jmx server.
   * @param ssl specifies whether JMX should be used over an SSL/TLS connection.
   * @param portProperty an ordered set of configuration properties to use for looking up the desired management port.
   * @exclude
   */
  public JMXMPServer(final TypedProperties config, final String id, final boolean ssl, final JPPFProperty<Integer> portProperty) {
    this(config, id, ssl, portProperty, null);
  }

  /**
   * @exclude
   */
  @Override
  public void start(final ClassLoader cl) throws Exception {
    if (debugEnabled) log.debug("starting remote connector server");
    System.out.println("starting JMXMP server");
    final ClassLoader tmp = Thread.currentThread().getContextClassLoader();
    lock.lock();
    try {
      Thread.currentThread().setContextClassLoader(cl);
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
      if (ssl) new SSLHelper(config).configureJMXProperties(JMXHelper.JMXMP_PROTOCOL, env);
      env.put("jmx.remote.object.wrapping", newObjectWrapping());
      startConnectorServer(JMXHelper.JMXMP_PROTOCOL, env);
    } finally {
      lock.unlock();
      Thread.currentThread().setContextClassLoader(tmp);
    }
  }

  /**
   * @return a new instance of an implementation of {@code ObjectWrapping}.
   * @throws Exception if any eror occurs.
   * @exclude
   */
  public static Object newObjectWrapping() throws Exception {
    final ClassLoader cl = JMXMPServer.class.getClassLoader();
    final Class<?>[] infs = { Class.forName("javax.management.remote.generic.ObjectWrapping", true, cl) };
    return Proxy.newProxyInstance(cl, infs, objectWrappingInvocationHandler);
  }

  /**
   * @exclude
   */
  public static class ObjectWrappingInvocationHandler implements InvocationHandler {
    /**
     * 
     */
    private static final BootstrapObjectSerializer SERIALIZER = new BootstrapObjectSerializer();

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
      if ("wrap".equals(method.getName())) {
        if (traceEnabled) log.trace("wrap: arg0 is a {} : {}", SystemUtils.getSystemIdentity(args[0]), args[0]);
        return SERIALIZER.serialize(args[0]).buffer;
      } else if ("unwrap".equals(method.getName())) {
        if (traceEnabled) log.trace("unwrap: arg0 is a {} : {}", SystemUtils.getSystemIdentity(args[0]), args[0]);
        return SERIALIZER.deserialize((byte[]) args[0]);
      }
      throw new JPPFUnsupportedOperationException("no support for %s" + method);
    }
  }
}
