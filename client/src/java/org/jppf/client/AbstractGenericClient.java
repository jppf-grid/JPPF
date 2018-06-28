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
import java.util.concurrent.*;

import javax.sql.DataSource;

import org.jppf.client.balancer.*;
import org.jppf.client.balancer.queue.JPPFPriorityQueue;
import org.jppf.client.event.*;
import org.jppf.comm.discovery.*;
import org.jppf.discovery.*;
import org.jppf.load.balancer.persistence.*;
import org.jppf.load.balancer.spi.JPPFBundlerFactory;
import org.jppf.persistence.JPPFDatasourceFactory;
import org.jppf.queue.*;
import org.jppf.startup.JPPFClientStartupSPI;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.*;
import org.jppf.utils.configuration.*;
import org.jppf.utils.hooks.HookFactory;
import org.slf4j.*;

/**
 * This class provides an API to submit execution requests and administration commands,
 * and request server information data.<br>
 * It has its own unique identifier, used by the nodes, to determine whether classes from
 * the submitting application should be dynamically reloaded or not, depending on whether
 * the uuid has changed or not.
 * @author Laurent Cohen
 */
public abstract class AbstractGenericClient extends AbstractJPPFClient implements QueueListener<ClientJob, ClientJob, ClientTaskBundle> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractGenericClient.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Constant for JPPF automatic connection discovery.
   */
  static final String VALUE_JPPF_DISCOVERY = "jppf_discovery";
  /**
   * The pool of threads used for submitting execution requests.
   */
  private ThreadPoolExecutor executor = null;
  /**
   * Performs server discovery.
   */
  private JPPFMulticastReceiverThread receiverThread = null;
  /**
   * The job manager.
   */
  private JobManager jobManager;
  /**
   * Handles the class loaders used for inbound class loading requests from the servers.
   */
  private final ClassLoaderRegistrationHandler classLoaderRegistrationHandler;
  /**
   * The list of listeners on the queue associated with this client.
   */
  private final List<ClientQueueListener> queueListeners = new CopyOnWriteArrayList<>();
  /**
   * Supports built-in and custom discovery mechanisms.
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  final DriverDiscoveryHandler<ClientConnectionPoolInfo> discoveryHandler = new DriverDiscoveryHandler(ClientDriverDiscovery.class);
  /**
   * Listens to new connection pool notifications from {@link DriverDiscovery} instances.
   */
  private ClientDriverDiscoveryListener discoveryListener;
  /**
   * The factory that creates load-balancer instances.
   */
  JPPFBundlerFactory bundlerFactory;
  /**
   * Manages the persisted states of the load-balancers.
   */
  LoadBalancerPersistenceManager loadBalancerPersistenceManager;

  /**
   * Initialize this client with a specified application UUID.
   * @param uuid the unique identifier for this local client.
   * @param configuration the object holding the JPPF configuration.
   * @param listeners the listeners to add to this JPPF client to receive notifications of new connections.
   */
  public AbstractGenericClient(final String uuid, final TypedProperties configuration, final ConnectionPoolListener... listeners) {
    super(uuid);
    this.classLoaderRegistrationHandler = new ClassLoaderRegistrationHandler();
    if ((listeners != null) && (listeners.length > 0)) {
      for (ConnectionPoolListener listener: listeners) {
        if (listener != null) addConnectionPoolListener(listener);
      }
    }
    discoveryListener = new ClientDriverDiscoveryListener(this);
    init(configuration);
  }

  /**
   * Initialize this client with the specified configuration.
   * @param configuration the configuration to use with this client.
   */
  protected void init(final TypedProperties configuration) {
    if (debugEnabled) log.debug("initializing client");
    closed.set(false);
    resetting.set(false);
    this.config = initConfig(configuration);
    try {
      final Map<String, DataSource> result = JPPFDatasourceFactory.getInstance().createDataSources(config);
      log.info(String.format("created client-side datasources: %s", result.keySet()));
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
    this.bundlerFactory = new JPPFBundlerFactory(JPPFBundlerFactory.Defaults.CLIENT, config);
    this.loadBalancerPersistenceManager = new LoadBalancerPersistenceManager(this.bundlerFactory);
    try {
      HookFactory.registerSPIMultipleHook(JPPFClientStartupSPI.class, null, null).invoke("run");
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
    final int coreThreads = Runtime.getRuntime().availableProcessors();
    final BlockingQueue<Runnable> queue = new SynchronousQueue<>();
    executor = new ThreadPoolExecutor(coreThreads, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, queue, new JPPFThreadFactory("JPPF Client"));
    executor.allowCoreThreadTimeOut(true);
    if (jobManager == null) jobManager = createJobManager();
    final Runnable r = new Runnable() {
      @Override
      public void run() {
        initPools(config);
      }
    };
    ThreadUtils.startThread(r, "InitPools");
  }

  /**
   * Initialize this client's configuration.
   * @param configuration an object holding the JPPF configuration.
   * @return <code>TypedProperties</code> instance holding JPPF configuration. Never be <code>null</code>.
   * @exclude
   */
  protected TypedProperties initConfig(final Object configuration) {
    if (configuration instanceof TypedProperties) return (TypedProperties) configuration;
    return JPPFConfiguration.getProperties();
  }

  /**
   * @exclude
   */
  @Override
  protected void initPools(final TypedProperties config) {
    if (debugEnabled) log.debug("initializing connections");
    if (config.get(JPPFProperties.LOCAL_EXECUTION_ENABLED)) setLocalExecutionEnabled(true);
    if (config.get(JPPFProperties.REMOTE_EXECUTION_ENABLED)) initRemotePools(config);
    discoveryHandler.register(discoveryListener.open()).start();
  }

  /**
   * Initialize remote connection pools according to configuration.
   * @param config The JPPF configuration properties.
   * @exclude
   */
  protected void initRemotePools(final TypedProperties config) {
    try {
      boolean initPeers;
      if (config.get(JPPFProperties.DISCOVERY_ENABLED)) {
        final int priority = config.get(JPPFProperties.DISCOVERY_PRIORITY);
        final boolean acceptMultipleInterfaces = config.get(JPPFProperties.DISCOVERY_ACCEPT_MULTIPLE_INTERFACES);
        if (debugEnabled) log.debug("initializing connections from discovery with priority = {} and acceptMultipleInterfaces = {}", priority, acceptMultipleInterfaces);
        receiverThread = new JPPFMulticastReceiverThread(new JPPFMulticastReceiverThread.ConnectionHandler() {
          @Override
          public void onNewConnection(final String name, final JPPFConnectionInformation info) {
            final boolean ssl = config.get(JPPFProperties.SSL_ENABLED);
            if (info.hasValidPort(ssl)) {
              final int poolSize = config.get(JPPFProperties.POOL_SIZE);
              final int jmxPoolSize = config.get(JPPFProperties.JMX_POOL_SIZE);
              newConnectionPool(name, info, priority, poolSize, ssl, jmxPoolSize);
            } else {
              final String type = ssl ? "secure" : "plain";
              final String msg = String.format("this client cannot fulfill a %s connection request to %s:%d because the host does not expose that port as a %s port",
                type, info.host, info.getValidPort(ssl), type);
              log.warn(msg);
            }
          }
        }, new IPFilter(config), acceptMultipleInterfaces);
        ThreadUtils.startDaemonThread(receiverThread, "ReceiverThread");
        initPeers = false;
      } else {
        receiverThread = null;
        initPeers = true;
      }
      if (debugEnabled) log.debug("looking for peers in the configuration");
      final String[] names = config.get(JPPFProperties.DRIVERS);
      if (debugEnabled) log.debug("list of drivers: {}", Arrays.asList(names));
      for (final String name : names) initPeers |= VALUE_JPPF_DISCOVERY.equals(name);
      if (debugEnabled) log.debug("initPeers = {}", initPeers);
      if (initPeers) {
        final List<ClientConnectionPoolInfo> infoList = new ArrayList<>();
        for (final String name : names) {
          if (!VALUE_JPPF_DISCOVERY.equals(name)) {
            final JPPFConnectionInformation info = new JPPFConnectionInformation();
            final boolean ssl = config.get(JPPFProperties.PARAM_SERVER_SSL_ENABLED, name);
            final String host =  config.get(JPPFProperties.PARAM_SERVER_HOST, name);
            info.host = host;
            final int port = config.get(JPPFProperties.PARAM_SERVER_PORT, name);
            if (!ssl) info.serverPorts = new int[] { port };
            else info.sslServerPorts = new int[] { port };
            if (receiverThread != null) receiverThread.addConnectionInformation(info);
            final int priority = config.get(JPPFProperties.PARAM_PRIORITY, name);
            final int poolSize = config.get(JPPFProperties.PARAM_POOL_SIZE, name);
            final int jmxPoolSize = config.get(JPPFProperties.PARAM_JMX_POOL_SIZE, name);
            final ClientConnectionPoolInfo ccpi = new ClientConnectionPoolInfo(name, ssl, host, port, priority, poolSize, jmxPoolSize);
            if (debugEnabled) log.debug("found pool definition in the configuration: {}", ccpi);
            infoList.add(ccpi);
          }
        }
        if (debugEnabled) log.debug("found {} pool definitions in the configuration", infoList.size());
        Collections.sort(infoList, new Comparator<ClientConnectionPoolInfo>() { // order by decreasing priority
          @Override
          public int compare(final ClientConnectionPoolInfo o1, final ClientConnectionPoolInfo o2) {
            final int p1 = o1.getPriority(), p2 = o2.getPriority();
            return p1 > p2 ? -1 : (p1 < p2 ? 1 : 0);
          }
        });
        for (ClientConnectionPoolInfo poolInfo: infoList) newConnectionPool(poolInfo);
      }
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Called when a new connection pool is read from the configuration.
   * @param name the name assigned to the connection pool.
   * @param info the information required for the connection to connect to the driver.
   * @param priority the priority assigned to the connection.
   * @param poolSize the size of the connection pool.
   * @param ssl determines whether the pool is for SSL connections.
   * @param jmxPoolSize the core size of the JMX connections pool.
   */
  void newConnectionPool(final String name, final JPPFConnectionInformation info, final int priority, final int poolSize, final boolean ssl, final int jmxPoolSize) {
    if (debugEnabled) log.debug("new connection pool: {}", name);
    final int size = poolSize > 0 ? poolSize : 1;
    final Runnable r = new Runnable() {
      @Override public void run() {
        final JPPFConnectionPool pool = new JPPFConnectionPool((JPPFClient) AbstractGenericClient.this, poolSequence.incrementAndGet(), name, priority, size, ssl, jmxPoolSize);
        pool.setDriverPort(ssl ? info.sslServerPorts[0] : info.serverPorts[0]);
        synchronized(pools) {
          pools.putValue(priority, pool);
        }
        HostIP hostIP = new HostIP(info.host, info.host);
        if (getConfig().get(JPPFProperties.RESOLVE_ADDRESSES)) hostIP = NetworkUtils.getHostIP(info.host);
        if (debugEnabled) log.debug("'{}' was resolved into '{}'", info.host, hostIP.hostName());
        pool.setDriverHostIP(hostIP);
        fireConnectionPoolAdded(pool);
        for (int i=1; i<=size; i++) {
          if (isClosed()) return;
          submitNewConnection(pool);
        }
      }
    };
    executor.execute(r);
  }

  /**
   * Called when a new connection pool is read from the configuration.
   * @param info the information required for the connection to connect to the driver.
   */
  void newConnectionPool(final ClientConnectionPoolInfo info) {
    if (debugEnabled) log.debug("new connection pool: {}", info.getName());
    final int size = info.getPoolSize() > 0 ? info.getPoolSize() : 1;
    final Runnable r = new Runnable() {
      @Override public void run() {
        final JPPFConnectionPool pool = new JPPFConnectionPool((JPPFClient) AbstractGenericClient.this, poolSequence.incrementAndGet(), info);
        pool.setDriverPort(info.getPort());
        synchronized(pools) {
          pools.putValue(info.getPriority(), pool);
        }
        HostIP hostIP = new HostIP(info.getHost(), info.getHost());
        if (getConfig().get(JPPFProperties.RESOLVE_ADDRESSES)) hostIP = NetworkUtils.getHostIP(info.getHost());
        if (debugEnabled) log.debug("'{}' was resolved into '{}'", info.getHost(), hostIP.hostName());
        pool.setDriverHostIP(hostIP);
        fireConnectionPoolAdded(pool);
        for (int i=1; i<=size; i++) {
          if (isClosed()) return;
          submitNewConnection(pool);
        }
      }
    };
    executor.execute(r);
  }

  /**
   * Called to submit the initialization of a new connection.
   * @param pool thez connection pool to which the connection belongs.
   * @exclude
   */
  protected void submitNewConnection(final JPPFConnectionPool pool) {
    final AbstractJPPFClientConnection c = createConnection(pool.getName() + "-" + pool.nextSequence(), pool);
    newConnection(c);
  }

  /**
   * Create a new driver connection based on the specified parameters.
   * @param name the name of the connection.
   * @param pool id of the connection pool the connection belongs to.
   * @return an instance of a subclass of {@link AbstractJPPFClientConnection}.
   */
  abstract AbstractJPPFClientConnection createConnection(String name, final JPPFConnectionPool pool);

  @Override
  void newConnection(final AbstractJPPFClientConnection c) {
    if (isClosed()) return;
    log.info("connection [" + c.getName() + "] created");
    c.addClientConnectionStatusListener(this);
    c.submitInitialization();
    //executor.execute(new ConnectionInitializer(c));
    fireConnectionAdded(c);
    if (debugEnabled) log.debug("end of of newConnection({})", c.getName());
  }

  /**
   * Invoked when the status of a connection has changed to <code>JPPFClientConnectionStatus.FAILED</code>.
   * @param connection the connection that failed.
   * @exclude
   */
  @Override
  protected void connectionFailed(final JPPFClientConnection connection) {
    if (debugEnabled) log.debug("Connection [{}] {}", connection.getName(), connection.getStatus());
    final JPPFConnectionPool pool = connection.getConnectionPool();
    connection.close();
    final boolean poolRemoved = removeClientConnection(connection);
    fireConnectionRemoved(connection);
    if (poolRemoved) {
      fireConnectionPoolRemoved(pool);
      if (receiverThread != null) receiverThread.removeConnectionInformation(connection.getDriverUuid());
      final ClientConnectionPoolInfo info = pool.getDiscoveryInfo();
      if (info != null) {
        final boolean b = discoveryListener.onPoolRemoved(info);
        if (debugEnabled) log.debug("removal of {} = {}", info, b);
      }
    }
  }

  @Override
  public void close() {
    close(false);
  }

  /**
   * Close this client.
   * @param reset if <code>true</code>, then this client is left in a state where it can be reopened.
   * @exclude
   */
  protected void close(final boolean reset) {
    try {
      log.info("closing JPPF client with uuid={}, PID={}", getUuid(), SystemUtils.getPID());
      if (!closed.compareAndSet(false, true)) return;
      if (debugEnabled) log.debug("closing discovery handler");
      discoveryListener.close();
      discoveryHandler.stop();
      if (debugEnabled) log.debug("closing broadcast receiver");
      if (receiverThread != null) {
        receiverThread.close();
        receiverThread = null;
      }
      if (debugEnabled) log.debug("unregistering startup classes");
      HookFactory.unregister(JPPFClientStartupSPI.class);
      if (jobManager != null) {
        if (reset) {
          if (debugEnabled) log.debug("resetting job manager");
          jobManager.reset();
        } else {
          if (debugEnabled) log.debug("closing job manager");
          jobManager.close();
          jobManager = null;
        }
      }
      if (debugEnabled) log.debug("closing executor");
      if (executor != null) {
        executor.shutdownNow();
        executor = null;
      }
      if (debugEnabled) log.debug("clearing registered class loaders");
      classLoaderRegistrationHandler.close();
      super.close();
    } catch(final Throwable t) {
      log.error(t.getMessage(), t);
    }
  }

  /**
   * Determine whether local execution is enabled on this client.
   * @return <code>true</code> if local execution is enabled, <code>false</code> otherwise.
   */
  public boolean isLocalExecutionEnabled() {
    final JobManager jobManager = getJobManager();
    return (jobManager != null) && jobManager.isLocalExecutionEnabled();
  }

  /**
   * Specify whether local execution is enabled on this client.
   * @param localExecutionEnabled <code>true</code> to enable local execution, <code>false</code> otherwise
   */
  public void setLocalExecutionEnabled(final boolean localExecutionEnabled) {
    final JobManager jobManager = getJobManager();
    if (jobManager != null) jobManager.setLocalExecutionEnabled(localExecutionEnabled);
  }

  /**
   * Determine whether there is a client connection available for execution.
   * @return true if at least one connection is available, false otherwise.
   */
  public boolean hasAvailableConnection() {
    final JobManager jobManager = getJobManager();
    return (jobManager != null) && jobManager.hasAvailableConnection();
  }

  /**
   * @exclude
   */
  @Override
  public void statusChanged(final ClientConnectionStatusEvent event) {
    super.statusChanged(event);
    final JobManager jobManager = getJobManager();
    if(jobManager != null) {
      final ClientConnectionStatusListener listener = jobManager.getClientConnectionStatusListener();
      if (listener != null) listener.statusChanged(event);
    }
  }

  /**
   * Get the pool of threads used for submitting execution requests.
   * @return a {@link ThreadPoolExecutor} instance.
   * @exclude
   */
  public ThreadPoolExecutor getExecutor() {
    return executor;
  }

  /**
   * Get the job manager for this JPPF client.
   * @return a <code>JobManager</code> instance.
   * @exclude
   */
  public JobManager getJobManager() {
    return jobManager;
  }

  /**
   * Create the job manager for this JPPF client.
   * @return a <code>JobManager</code> instance.
   */
  abstract JobManager createJobManager();

  /**
   * Cancel the job with the specified id.
   * @param jobId the id of the job to cancel.
   * @throws Exception if any error occurs.
   * @see org.jppf.server.job.management.DriverJobManagementMBean#cancelJob(java.lang.String)
   * @return a <code>true</code> when cancel was successful <code>false</code> otherwise.
   */
  public boolean cancelJob(final String jobId) throws Exception {
    if (jobId == null || jobId.isEmpty()) throw new IllegalArgumentException("jobUUID is blank");
    if (debugEnabled) log.debug("request to cancel job with uuid=" + jobId);
    return getJobManager().cancelJob(jobId);
  }

  /**
   * Get a class loader associated with a job.
   * @param uuid unique id assigned to classLoader. Added as temporary fix for problems hanging jobs.
   * @return a {@code Collection} of {@code RegisteredClassLoader} instances.
   * @exclude
   */
  public Collection<ClassLoader> getRegisteredClassLoaders(final String uuid) {
    return classLoaderRegistrationHandler.getRegisteredClassLoaders(uuid);
  }

  /**
   * Register a class loader for the specified job uuid.
   * @param cl the <code>ClassLoader</code> instance to register.
   * @param uuid the uuid of the job for which the class loader is registered.
   * @return a <code>RegisteredClassLoader</code> instance.
   */
  public ClassLoader registerClassLoader(final ClassLoader cl, final String uuid) {
    return classLoaderRegistrationHandler.registerClassLoader(cl, uuid);
  }

  /**
   * Unregisters the class loader associated with the specified job uuid.
   * @param uuid the uuid of the job the class loaders are associated with.
   * @exclude
   */
  public void unregisterClassLoaders(final String uuid) {
    classLoaderRegistrationHandler.unregister(uuid);
  }

  /**
   * Register the specified listener to receive client queue event notifications.
   * @param listener the listener to register.
   * @since 4.1
   */
  public void addClientQueueListener(final ClientQueueListener listener) {
    queueListeners.add(listener);
  }

  /**
   * Unregister the specified listener.
   * @param listener the listener to unregister.
   * @since 4.1
   */
  public void removeClientQueueListener(final ClientQueueListener listener) {
    queueListeners.remove(listener);
  }

  /**
   * Notify all client queue listeners that a queue event has occurred.
   * @param qEvent the actual event which occurred in the queue.
   * @param jobAdded {@code true} for a job added event, {@code false} for a job removed event.
   * @exclude
   */
  protected void fireQueueEvent(final QueueEvent<ClientJob, ClientJob, ClientTaskBundle> qEvent, final boolean jobAdded) {
    final ClientQueueEvent event = new ClientQueueEvent((JPPFClient) this, qEvent.getJob().getJob(), (JPPFPriorityQueue) qEvent.getQueue());
    if (jobAdded) {
      for (final ClientQueueListener listener: queueListeners) listener.jobAdded(event);
    } else {
      for (final ClientQueueListener listener: queueListeners) listener.jobRemoved(event);
    }
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public void bundleAdded(final QueueEvent<ClientJob, ClientJob, ClientTaskBundle> event) {
    fireQueueEvent(event, true);
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public void bundleRemoved(final QueueEvent<ClientJob, ClientJob, ClientTaskBundle> event) {
    fireQueueEvent(event, false);
  }

  /**
   * Get the object that manages the persisted states of the load-balancers.
   * @return an instance of {@link LoadBalancerPersistenceManagement}.
   * @since 6.0
   */
  public LoadBalancerPersistenceManagement getLoadBalancerPersistenceManagement() {
    return loadBalancerPersistenceManager;
  }
}
