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

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.client.event.*;
import org.jppf.management.*;
import org.jppf.node.protocol.*;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * Instances of this class represent connections to remote JPPF drivers.
 * @author Laurent Cohen
 */
abstract class AbstractJPPFClientConnection extends BaseJPPFClientConnection {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractJPPFClientConnection.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
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
   * Initialize this connection with a parent pool.
   * @param pool the connection pool this connection belongs to.
   */
  AbstractJPPFClientConnection(final JPPFConnectionPool pool) {
    super(pool);
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
    JPPFClientConnectionStatus oldStatus = getStatus();
    if (status != oldStatus) {
      if (debugEnabled) log.debug("connection '" + name + "' status changing from " + oldStatus + " to " + status);
      this.status.set(status);
      fireStatusChanged(oldStatus);
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
    ClientConnectionStatusEvent event = new ClientConnectionStatusEvent(this, oldStatus);
    for (ClientConnectionStatusListener listener : listeners) listener.statusChanged(event);
  }

  /**
   * Get a string representation of this client connection.
   * @return a string representing this connection.
   */
  @Override
  public String toString() {
    return displayName + " : " + status;
  }

  /**
   * Cancel the job with the specified id.
   * @param jobId the id of the job to cancel.
   * @throws Exception if any error occurs.
   * @return a <code>true</code> when cancel was successful <code>false</code> otherwise.
   * @deprecated this method does not do anything and always returns {@code false}. Use {@link AbstractGenericClient#cancelJob(String)} instead.
   */
  public boolean cancelJob(final String jobId) throws Exception {
    return false;
  }

  /**
   * Invoked to notify of a status change event on a client connection.
   * @param event the event to notify of.
   */
  void delegateStatusChanged(final ClientConnectionStatusEvent event) {
    JPPFClientConnectionStatus s1 = event.getClientConnectionStatusHandler().getStatus();
    JPPFClientConnectionStatus s2 = taskServerConnection.getStatus();
    processStatusChanged(s1, s2);
  }

  /**
   * Invoked to notify of a status change event on a client connection.
   * @param event the event to notify of.
   */
  void taskServerConnectionStatusChanged(final ClientConnectionStatusEvent event) {
    JPPFClientConnectionStatus s1 = event.getClientConnectionStatusHandler().getStatus();
    JPPFClientConnectionStatus s2 = delegate.getStatus();
    processStatusChanged(s2, s1);
  }

  /**
   * Handle a status change from either the class server delegate or the task server connection
   * and determine whether it triggers a status change for the client connection.
   * @param delegateStatus status of the class server delegate connection.
   * @param taskConnectionStatus status of the task server connection.
   */
  void processStatusChanged(final JPPFClientConnectionStatus delegateStatus, final JPPFClientConnectionStatus taskConnectionStatus) {
    final boolean trace = log.isTraceEnabled();
    if (delegateStatus.isTerminatedStatus()) {
      if (trace) log.trace(String.format("trace 1 (delegate=%s, task=%s): setting status to %s", delegateStatus, taskConnectionStatus, delegateStatus));
      setStatus(delegateStatus);
    } else if (delegateStatus == ACTIVE) {
      if ((taskConnectionStatus == ACTIVE) && (this.getStatus() != ACTIVE)) {
        if (trace) log.trace(String.format("trace 2 (delegate=%s, task=%s): setting status to %s", delegateStatus, taskConnectionStatus, ACTIVE));
        setStatus(ACTIVE);
      } else if (taskConnectionStatus != this.getStatus()) {
        if (trace) log.trace(String.format("trace 3 (delegate=%s, task=%s): setting status to %s", delegateStatus, taskConnectionStatus, taskConnectionStatus));
        setStatus(taskConnectionStatus);
      }
    } else {
      if (taskConnectionStatus == ACTIVE) {
        if (trace) log.trace(String.format("trace 4 (delegate=%s, task=%s): setting status to %s", delegateStatus, taskConnectionStatus, delegateStatus));
        setStatus(delegateStatus);
      } else {
        int n = delegateStatus.compareTo(taskConnectionStatus);
        if ((n < 0) && (delegateStatus != this.getStatus())) {
          if (trace) log.trace(String.format("trace 5 (delegate=%s, task=%s): setting status to %s", delegateStatus, taskConnectionStatus, delegateStatus));
          setStatus(delegateStatus);
        } else if (taskConnectionStatus != this.getStatus()) {
          if (trace) log.trace(String.format("trace 6 (delegate=%s, task=%s): setting status to %s", delegateStatus, taskConnectionStatus, taskConnectionStatus));
          setStatus(taskConnectionStatus);
        }
      }
    }
  }

  /**
   * Handle a status change from either the class server delegate or the task server connection
   * and determine whether it triggers a status change for the client connection.
   * @param delegateStatus status of the class server delegate connection.
   * @param taskConnectionStatus status of the task server connection.
   */
  void processStatusChanged2(final JPPFClientConnectionStatus delegateStatus, final JPPFClientConnectionStatus taskConnectionStatus) {
    if (delegateStatus.isTerminatedStatus()) setStatus(delegateStatus);
    else if (delegateStatus == ACTIVE) {
      if ((taskConnectionStatus == ACTIVE) && (this.getStatus() != ACTIVE)) setStatus(ACTIVE);
      else if (taskConnectionStatus != this.getStatus()) setStatus(taskConnectionStatus);
    } else {
      if (taskConnectionStatus == ACTIVE) setStatus(delegateStatus);
      else {
        int n = delegateStatus.compareTo(taskConnectionStatus);
        if ((n < 0) && (delegateStatus != this.getStatus())) setStatus(delegateStatus);
        else if (taskConnectionStatus != this.getStatus()) setStatus(taskConnectionStatus);
      }
    }
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
    TaskBundle bundle = super.sendHandshakeJob();
    pool.setSystemInfo((JPPFSystemInformation) bundle.getParameter(BundleParameter.SYSTEM_INFO_PARAM));
    pool.setDriverUuid((String) bundle.getParameter(BundleParameter.DRIVER_UUID_PARAM));
    return bundle;
  }

  /**
   * Shutdown this client and retrieve all pending executions for resubmission.
   */
  @Override
  public void close() {
    if (!closed.compareAndSet(false, true)) return;
    if (!getStatus().isTerminatedStatus()) setStatus(CLOSED);
    if (debugEnabled) log.debug("closing connection " + toDebugString());
    listeners.clear();
    try {
      sendCloseConnectionCommand();
    } catch (Exception e) {
      if (debugEnabled) log.debug('[' + name + "] " + e.getMessage(), e);
      else log.error('[' + name + "] " + e.getMessage());
    }
    try {
      if (debugEnabled) log.debug("closing task server connection " + this);
      if (taskServerConnection != null) taskServerConnection.close();
      if (debugEnabled) log.debug("closing class server connection " + this);
      if (delegate != null) delegate.close();
      //if (debugEnabled) log.debug("closing jmx connection " + this);
    } catch (Exception e) {
      if (debugEnabled) log.debug('[' + name + "] " + e.getMessage(), e);
      else log.error('[' + name + "] " + e.getMessage());
    }
    if (debugEnabled) log.debug("connection " + toDebugString() + " closed");
  }

  @Override
  public boolean isClosed() {
    return pool.getClient().isClosed() || closed.get();
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
