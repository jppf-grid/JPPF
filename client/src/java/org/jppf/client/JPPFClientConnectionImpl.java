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

package org.jppf.client;

import static org.jppf.client.JPPFClientConnectionStatus.*;

import org.jppf.JPPFError;
import org.jppf.client.event.*;
import org.jppf.comm.discovery.JPPFConnectionInformation;
import org.jppf.comm.socket.*;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * Instances of this class represent connections to remote JPPF drivers.
 * @author Laurent Cohen
 * @exclude
 */
public class JPPFClientConnectionImpl extends AbstractJPPFClientConnection {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFClientConnectionImpl.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * Initialize this client with a specified application UUID.
   * @param client the JPPF client that owns this connection.
   * @param uuid the unique identifier of the remote driver.
   * @param name configuration name for this local client.
   * @param info the connection properties for this connection.
   * @param pool the connection pool this connection belongs to.
   */
  JPPFClientConnectionImpl(final JPPFClient client, final String uuid, final String name, final JPPFConnectionInformation info, final JPPFConnectionPool pool) {
    super(pool);
    if (client.isClosed()) {
      if (debugEnabled) log.debug("error: initializing connection {} while client is closed", name);
      throw new IllegalStateException("error: initializing connection " + name + " while client is closed");
    }
    boolean ssl = pool.isSslEnabled();
    if (ssl && (info.sslServerPorts == null)) throw new IllegalStateException("ssl is enabled but no ssl port is provided");
    this.connectionUuid = client.getUuid() + '_' + connectionCount.incrementAndGet();
    configure(uuid, name, 0);
    displayName = name + '[' + getHost() + ':' + getPort() + ']';
    pool.add(this);
    int jmxPort = ssl ? info.sslManagementPort : info.managementPort;
    if (jmxPort >= 0) pool.setJmxPort(jmxPort);
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public void init() {
    try {
      if (isClosed()) {
        log.warn("attempting to init closed " + getClass().getSimpleName() + ", aborting");
        return;
      }
      delegate = new ClassServerDelegateImpl(this, pool.getClient().getUuid(), getHost(), getPort());
      delegate.addClientConnectionStatusListener(new ClientConnectionStatusListener() {
        @Override
        public void statusChanged(final ClientConnectionStatusEvent event) {
          delegateStatusChanged(event);
        }
      });
      taskServerConnection.addClientConnectionStatusListener(new ClientConnectionStatusListener() {
        @Override
        public void statusChanged(final ClientConnectionStatusEvent event) {
          taskServerConnectionStatusChanged(event);
        }
      });
      connect();
      JPPFClientConnectionStatus status = getStatus();
      if (debugEnabled) log.debug("connection [" + name + "] status=" + status);
      if (pool.getClient().isClosed()) close();
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      setStatus(FAILED);
    } catch (JPPFError e) {
      setStatus(FAILED);
      throw e;
    }
  }

  /**
   * Connect to the driver.
   * @throws Exception if connection failed.
   */
  void connect() throws Exception {
    delegate.init();
    if (!delegate.isClosed()) {
      new Thread(delegate, delegate.getName()).start();
      taskServerConnection.init();
    }
  }

  @Override
  SocketInitializer createSocketInitializer() {
    return new SocketInitializerImpl();
  }
}
