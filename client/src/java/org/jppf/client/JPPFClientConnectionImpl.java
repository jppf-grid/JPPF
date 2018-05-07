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
import org.jppf.comm.socket.*;
import org.jppf.utils.LoggingUtils;
import org.jppf.utils.concurrent.ThreadUtils;
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
   * @param name configuration name for this local client.
   * @param pool the connection pool this connection belongs to.
   */
  JPPFClientConnectionImpl(final JPPFClient client, final String name, final JPPFConnectionPool pool) {
    super(pool);
    if (client.isClosed()) {
      if (debugEnabled) log.debug("error: initializing connection {} while client is closed", name);
      throw new IllegalStateException("error: initializing connection " + name + " while client is closed");
    }
    this.connectionUuid = client.getUuid() + '_' + connectionCount.incrementAndGet();
    configure(pool.getDriverUuid(), name);
    displayName = name + '[' + getHost() + ':' + getPort() + "] (id=" + instanceNumber + ")";
    pool.add(this);
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
      if (delegate == null) delegate = new ClassServerDelegateImpl(this, pool.getClient().getUuid(), getHost(), getPort());
      if (taskServerConnection == null) taskServerConnection = new TaskServerConnectionHandler(this, getHost(), getPort());
      setStatus(CONNECTING);
      connect();
      setStatus(ACTIVE);
      final JPPFClientConnectionStatus status = getStatus();
      if (debugEnabled) log.debug("connection [" + name + "] status=" + status);
      if (pool.getClient().isClosed()) close();
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
      setStatus(FAILED);
    } catch (final JPPFError e) {
      setStatus(FAILED);
      throw e;
    }
  }

  /**
   * Connect to the driver.
   * @throws Exception if connection failed.
   */
  private void connect() throws Exception {
    delegate.init();
    if (!delegate.isClosed()) {
      ThreadUtils.startThread(delegate, delegate.getName());
      taskServerConnection.init();
    }
  }

  @Override
  SocketInitializer createSocketInitializer() {
    return SocketInitializer.Factory.newInstance(pool.getClient().getConfig());
  }
}
