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

package org.jppf.client;

import static org.jppf.client.JPPFClientConnectionStatus.*;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.*;

import org.jppf.JPPFError;
import org.jppf.client.event.*;
import org.jppf.comm.socket.*;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.node.protocol.*;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.ThreadUtils;
import org.slf4j.*;

/**
 * Instances of this class represent connections to remote JPPF drivers.
 * @author Laurent Cohen
 * @exclude
 */
public class JPPFClientConnectionImpl extends BaseJPPFClientConnection {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFClientConnectionImpl.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Count of instnces of this class.
   */
  private static AtomicLong instanceCount = new AtomicLong(0L);
  /**
   * List of status listeners for this connection.
   */
  private final List<ClientConnectionStatusListener> listeners = new CopyOnWriteArrayList<>();
  /**
   * The name displayed for this connection.
   */
  String displayName;
  /**
   * Whether this connection is closed.
   */
  private final AtomicBoolean closed = new AtomicBoolean(false);
  /**
   * Whether this connection is initializing.
   */
  final AtomicBoolean initializing = new AtomicBoolean(false);
  /**
   * Instance number for this instance.
   */
  final long instanceNumber = instanceCount.incrementAndGet();

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

  /**
   * Configure this client connection with the specified parameters.
   * @param uuid the remote driver's UUID.
   * @param name configuration name for this local client.
   */
  void configure(final String uuid, final String name) {
    pool.setDriverUuid(uuid);
    this.name = name;
    displayName = name;
  }

  /**
   * Get the priority assigned to this connection.
   * @return a priority as an int value.
   */
  @Override
  public int getPriority() {
    return pool.getPriority();
  }

  @Override
  public JPPFClientConnectionStatus getStatus() {
    return status.get();
  }

  @Override
  public void setStatus(final JPPFClientConnectionStatus status) {
    synchronized(statusChangeLock) {
      final JPPFClientConnectionStatus oldStatus = getStatus();
      if (debugEnabled) log.debug("connection '" + name + "' attempting to change status to " + status);
      if (status != oldStatus) {
        if (debugEnabled) log.debug("connection '" + name + "' status changing from " + oldStatus + " to " + status);
        this.status.set(status);
        fireStatusChanged(oldStatus);
      }
    }
  }

  @Override
  public void addClientConnectionStatusListener(final ClientConnectionStatusListener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeClientConnectionStatusListener(final ClientConnectionStatusListener listener) {
    listeners.remove(listener);
  }

  /**
   * Notify all listeners that the status of this connection has changed.
   * @param oldStatus the connection status before the change.
   */
  void fireStatusChanged(final JPPFClientConnectionStatus oldStatus) {
    final ClientConnectionStatusEvent event = new ClientConnectionStatusEvent(this, oldStatus);
    for (final ClientConnectionStatusListener listener : listeners) listener.statusChanged(event);
  }

  @Override
  public String toString() {
    return displayName + " : " + status + " (" + SystemUtils.getSystemIdentityName(this) + ")";
  }

  @Override
  public boolean isSSLEnabled() {
    return pool.isSslEnabled();
  }

  @Override
  public JPPFSystemInformation getSystemInfo() {
    return pool.getSystemInfo();
  }

  @Override
  TaskBundle sendHandshakeJob() throws Exception {
    final TaskBundle bundle = super.sendHandshakeJob();
    pool.setSystemInfo((JPPFSystemInformation) bundle.getParameter(BundleParameter.SYSTEM_INFO_PARAM));
    pool.setDriverUuid((String) bundle.getParameter(BundleParameter.DRIVER_UUID_PARAM));
    return bundle;
  }

  /**
   * Shutdown this connection and retrieve all pending executions for resubmission.
   */
  @Override
  public void close() {
    if (!closed.compareAndSet(false, true)) return;
    if (debugEnabled) log.debug("closing connection {}", this);
    if (!getStatus().isTerminatedStatus()) setStatus(CLOSED);
    listeners.clear();
    try {
      sendCloseConnectionCommand();
    } catch (final Exception e) {
      if (debugEnabled) log.debug('[' + name + "] " + e.getMessage(), e);
      else log.error('[' + name + "] " + e.getMessage());
    }
    try {
      if (debugEnabled) log.debug("closing task server connection " + this);
      if (taskServerConnection != null) taskServerConnection.close();
      if (debugEnabled) log.debug("closing class server connection " + this);
      if (delegate != null) delegate.close();
      //if (debugEnabled) log.debug("closing jmx connection " + this);
    } catch (final Exception e) {
      if (debugEnabled) log.debug('[' + name + "] " + e.getMessage(), e);
      else log.error('[' + name + "] " + e.getMessage());
    }
    if (debugEnabled) log.debug("connection " + toDebugString() + " closed");
  }

  @Override
  public boolean isClosed() {
    return pool.closed.get() || pool.getClient().isClosed() || closed.get();
  }

  @Override
  public JPPFConnectionPool getConnectionPool() {
    return pool;
  }

  /**
   * Submit the initialization of this connetion, some time in the future.
   * @exclude
   */
  public void submitInitialization() {
    if (initializing.compareAndSet(false, true)) {
      getClient().getExecutor().submit(new ConnectionInitializer(this));
    }
  }
}
