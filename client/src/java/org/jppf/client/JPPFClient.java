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

import java.util.*;

import org.jppf.client.balancer.JobManagerClient;
import org.jppf.client.debug.Debug;
import org.jppf.client.event.*;
import org.jppf.comm.discovery.JPPFConnectionInformation;
import org.jppf.discovery.ClientDriverDiscovery;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class provides an API to submit execution requests and administration commands,
 * and request server information data.<br>
 * It has its own unique identifier, used by the nodes, to determine whether classes from
 * the submitting application should be dynamically reloaded or not, depending on whether
 * the uuid has changed or not.
 * @author Laurent Cohen
 */
public class JPPFClient extends AbstractGenericClient {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFClient.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * Initialize this client with an automatically generated application UUID.
   */
  public JPPFClient() {
    super(null, JPPFConfiguration.getProperties());
    Debug.register(this);
  }

  /**
   * Initialize this client with the specified application UUID.
   * @param uuid the unique identifier for this local client.
   */
  public JPPFClient(final String uuid) {
    super(uuid, JPPFConfiguration.getProperties());
    Debug.register(this);
  }

  /**
   * Initialize this client with an automatically generated application UUID.
   * @param listeners the listeners to add to this JPPF client to receive notifications of new connections.
   */
  public JPPFClient(final ConnectionPoolListener... listeners) {
    super(null, JPPFConfiguration.getProperties(), listeners);
    Debug.register(this);
  }

  /**
   * Initialize this client with the specified application UUID and new connection listeners.
   * @param uuid the unique identifier for this local client.
   * @param listeners the listeners to add to this JPPF client to receive notifications of new connections.
   */
  public JPPFClient(final String uuid, final ConnectionPoolListener... listeners) {
    super(uuid, JPPFConfiguration.getProperties(), listeners);
    Debug.register(this);
  }

  /**
   * Initialize this client with the specified application UUID and new connection listeners.
   * @param uuid the unique identifier for this local client.
   * @param config the JPPF configuration to use for this client.
   * @param listeners the listeners to add to this JPPF client to receive notifications of new connections.
   * @exclude
   */
  public JPPFClient(final String uuid, final TypedProperties config, final ConnectionPoolListener... listeners) {
    super(uuid, config, listeners);
    Debug.register(this);
  }

  @Override
  AbstractJPPFClientConnection createConnection(final String uuid, final String name, final JPPFConnectionInformation info, final JPPFConnectionPool pool) {
    return new JPPFClientConnectionImpl(this, uuid, name, info, pool);
  }

  @Override
  public List<Task<?>> submitJob(final JPPFJob job) {
    if (isClosed()) throw new IllegalStateException("this client is closed");
    if (job == null) throw new IllegalArgumentException("job cannot be null");
    if (job.getJobTasks().isEmpty()) throw new IllegalStateException("job cannot be empty");
    if (job.client != null) {
      if (!job.isDone()) throw new IllegalStateException("this job is already submitted");
      job.cancelled.set(false);
      job.getResults().clear();
    }
    job.client = this;
    if (debugEnabled) log.debug("submitting job {}", job);
    if (log.isTraceEnabled()) {
      for (Task<?> task: job) {
        log.trace(String.format("task %s, position=%d, taskObject=%s, taskObject class=%s", task, task.getPosition(), task.getTaskObject(),
            (task.getTaskObject() != null) ? task.getTaskObject().getClass() : null));
      }
    }
    getJobManager().submitJob(job);
    if (job.isBlocking()) return job.awaitResults();
    return null;
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  protected JobManager createJobManager() {
    JobManager jobManager = null;
    try {
      jobManager = new JobManagerClient(this);
    } catch (Exception e) {
      log.error("Can't initialize job Manager", e);
    }
    return jobManager;
  }

  /**
   * Reset this client, that is, close it if necessary, reload its configuration, then open it again.
   * If the client is already closed or reseeting, this method has no effect.
   * @see #reset(TypedProperties)
   * @since 4.0
   */
  public void reset() {
    if (isClosed()) return;
    if (debugEnabled) log.debug("resetting client");
    if (resetting.compareAndSet(false, true)) {
      close(true);
      JPPFConfiguration.reset();
      init(JPPFConfiguration.getProperties());
    }
  }

  /**
   * Reset this client, that is, close it if necessary, then open it again, using the specified confguration.
   * If the client is already closed or reseeting, this method has no effect.
   * @param configuration the configuration to initialize this client with.
   * @see #reset()
   * @since 4.0
   */
  public void reset(final TypedProperties configuration) {
    if (isClosed()) return;
    if (debugEnabled) log.debug("resetting client");
    if (resetting.compareAndSet(false, true)) {
      close(true);
      init(configuration);
    }
  }

  /**
   * Wait until there is at least one connection pool with at least one connection in the {@link JPPFClientConnectionStatus#ACTIVE ACTIVE} status.
   * This is a shorthand for {@code awaitConnectionPool(Long.MAX_VALUE, JPPFClientConnectionStatus.ACTIVE)}.
   * @return a {@link JPPFConnectionPool} instance, or null if no pool has a connection in the one of the desird statuses.
   * @since 5.0
   */
  public JPPFConnectionPool awaitActiveConnectionPool() {
    return awaitConnectionPool(Long.MAX_VALUE, JPPFClientConnectionStatus.ACTIVE);
  }

  /**
   * Wait until there is at least one connection pool with at least one connection in the {@link JPPFClientConnectionStatus#ACTIVE ACTIVE}
   * or {@link JPPFClientConnectionStatus#EXECUTING EXECUTING} status.
   * This is a shorthand for {@code awaitConnectionPool(Long.MAX_VALUE, JPPFClientConnectionStatus.ACTIVE, JPPFClientConnectionStatus.EXECUTING)}.
   * @return a {@link JPPFConnectionPool} instance, or null if no pool has a connection in the one of the desird statuses.
   * @since 5.0
   */
  public JPPFConnectionPool awaitWorkingConnectionPool() {
    return awaitConnectionPool(Long.MAX_VALUE, JPPFClientConnectionStatus.ACTIVE, JPPFClientConnectionStatus.EXECUTING);
  }

  /**
   * Wait until there is at least one connection pool with at least one connection in one of the specified statuses.
   * This is a shorthand for {@code awaitConnectionPool(Long.MAX_VALUE, statuses)}.
   * @param statuses the possible statuses of the connections in the pools to wait for.
   * @return a {@link JPPFConnectionPool} instance, or null if no pool has a connection in the one of the desird statuses.
   * @since 5.0
   */
  public JPPFConnectionPool awaitConnectionPool(final JPPFClientConnectionStatus...statuses) {
    return awaitConnectionPool(Long.MAX_VALUE, statuses);
  }

  /**
   * Wait until at least one connection pool with at least one connection in one of the specified statuses,
   * or until the specified timeout to expire, whichever happens first.
   * @param timeout the maximum time to wait, in milliseconds. A value of zero means an infinite timeout.
   * @param statuses the possible statuses of the connections in the pools to wait for.
   * @return a {@link JPPFConnectionPool} instance, or null if no pool has a connection in the one of the desird statuses.
   * @since 5.0
   */
  public JPPFConnectionPool awaitConnectionPool(final long timeout, final JPPFClientConnectionStatus...statuses) {
    List<JPPFConnectionPool> list = awaitConnectionPools(timeout, statuses);
    return list.isEmpty() ? null : list.get(0);
  }

  /**
   * Wait until there is at least one connection pool with at least one connection in the {@link JPPFClientConnectionStatus#ACTIVE ACTIVE}
   * or {@link JPPFClientConnectionStatus#EXECUTING EXECUTING} status.
   * This is a shorthand for {@code awaitConnectionPools(Long.MAX_VALUE, JPPFClientConnectionStatus.ACTIVE, JPPFClientConnectionStatus.EXECUTING)}.
   * @return a list of {@link JPPFConnectionPool} instances, possibly empty but never null.
   * @since 5.1
   */
  public List<JPPFConnectionPool> awaitWorkingConnectionPools() {
    return awaitConnectionPools(Long.MAX_VALUE, JPPFClientConnectionStatus.ACTIVE, JPPFClientConnectionStatus.EXECUTING);
  }

  /**
   * Wait until there is at least one connection pool with at least one connection in the {@link JPPFClientConnectionStatus#ACTIVE ACTIVE}
   * or {@link JPPFClientConnectionStatus#EXECUTING EXECUTING} status, or the specified tiemoput expires, whichever happens first.
   * This is a shorthand for {@code awaitConnectionPools(tiemout, JPPFClientConnectionStatus.ACTIVE, JPPFClientConnectionStatus.EXECUTING)}.
   * @param timeout the maximum time to wait, in milliseconds. A value of zero means an infinite timeout.
   * @return a list of {@link JPPFConnectionPool} instances, possibly empty but never null.
   * @since 5.1
   */
  public List<JPPFConnectionPool> awaitWorkingConnectionPools(final long timeout) {
    return awaitConnectionPools(timeout, JPPFClientConnectionStatus.ACTIVE, JPPFClientConnectionStatus.EXECUTING);
  }

  /**
   * Wait until at least one connection pool with at least one connection in one of the specified statuses,
   * or until the specified timeout to expire, whichever happens first.
   * @param timeout the maximum time to wait, in milliseconds. A value of zero means an infinite timeout.
   * @param statuses the possible statuses of the connections in the pools to wait for.
   * @return a list of {@link JPPFConnectionPool} instances, possibly empty but never null.
   * @since 5.0
   */
  public List<JPPFConnectionPool> awaitConnectionPools(final long timeout, final JPPFClientConnectionStatus...statuses) {
    final MutableReference<List<JPPFConnectionPool>> ref = new MutableReference<>();
    ConcurrentUtils.awaitCondition(new ConcurrentUtils.Condition() {
      @Override public boolean evaluate() {
        return !ref.setSynchronized(findConnectionPools(statuses), pools).isEmpty();
      }
    }, timeout);
    return ref.get();
  }

  /**
   * Wait until there is at least one connection pool where the number of connections with the specified statuses
   * satisfy the specified condition, or until the specified timeout expires, whichever happens first.
   * @param operator the condition on the number of connections to wait for. If {@code null}, it is assumed to be {@link Operator#EQUAL}.
   * @param expectedConnections the expected number of connections to wait for.
   * @param timeout the maximum time to wait, in milliseconds. A value of zero means an infinite timeout.
   * @param statuses the possible statuses of the connections in the pools to wait for.
   * @return a list of {@link JPPFConnectionPool} instances, possibly empty but never null.
   * @since 5.0
   */
  public List<JPPFConnectionPool> awaitConnectionPools(final Operator operator, final int expectedConnections, final long timeout, final JPPFClientConnectionStatus...statuses) {
    final MutableReference<List<JPPFConnectionPool>> ref = new MutableReference<>();
    final ConnectionPoolFilter<JPPFConnectionPool> filter = new ConnectionPoolFilter<JPPFConnectionPool>() {
      @Override
      public boolean accepts(final JPPFConnectionPool pool) {
        List<JPPFClientConnection> list = pool.getConnections(statuses);
        return operator.evaluate(list.size(), expectedConnections);
      }
    };
    ConcurrentUtils.awaitCondition(new ConcurrentUtils.Condition() {
      @Override public boolean evaluate() {
        List<JPPFConnectionPool> result = new ArrayList<>();
        List<JPPFConnectionPool> temp = findConnectionPools(statuses);
        for (JPPFConnectionPool pool: temp) {
          if (filter.accepts(pool)) result.add(pool);
        }
        boolean empty = ref.setSynchronized(result, pools).isEmpty();
        return !empty || (empty && (expectedConnections <= 0));
      }
    }, timeout);
    return ref.get();
  }

  /**
   * Wait until there is at least one connection pool where at least one connections passes the specified filter,
   * or until the specified timeout expires, whichever happens first.
   * @param timeout the maximum time to wait, in milliseconds. A value of zero means an infinite timeout.
   * @param filter an implementation of the {@link ConnectionPoolFilter} interface. A {@code null} value is interpreted as no filter (all pools are accepted).
   * @return a list of {@link JPPFConnectionPool} instances, possibly empty but never null.
   * @since 5.0
   */
  public List<JPPFConnectionPool> awaitConnectionPools(final long timeout, final ConnectionPoolFilter<JPPFConnectionPool> filter) {
    final MutableReference<List<JPPFConnectionPool>> ref = new MutableReference<>();
    ConcurrentUtils.awaitCondition(new ConcurrentUtils.Condition() {
      @Override public boolean evaluate() {
        List<JPPFConnectionPool> result = new ArrayList<>();
        List<JPPFConnectionPool> temp = getConnectionPools();
        for (JPPFConnectionPool pool: temp) {
          if (filter.accepts(pool)) result.add(pool);
        }
        return !ref.setSynchronized(result, pools).isEmpty();
      }
    }, timeout);
    return ref.get();
  }

  @Override
  public void close() {
    Debug.unregister(this);
    super.close();
  }

  /**
   * Add a custom driver discovery mechanism to those already registered, if any.
   * @param discovery the driver discovery to add.
   */
  public void addDriverDiscovery(final ClientDriverDiscovery discovery) {
    discoveryHandler.addDiscovery(discovery);
  }

  /**
   * Remove a custom driver discovery mechanism from those already registered.
   * @param discovery the driver discovery to remove.
   */
  public void removeDriverDiscovery(final ClientDriverDiscovery discovery) {
    discoveryHandler.removeDiscovery(discovery);
  }
}
