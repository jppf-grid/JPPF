/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

import java.util.List;
import java.util.concurrent.*;

import org.jppf.client.ClassLoaderRegistrationHandler.RegisteredClassLoader;
import org.jppf.client.balancer.*;
import org.jppf.client.balancer.queue.JPPFPriorityQueue;
import org.jppf.client.event.*;
import org.jppf.client.submission.SubmissionManager;
import org.jppf.comm.discovery.*;
import org.jppf.queue.*;
import org.jppf.startup.JPPFClientStartupSPI;
import org.jppf.utils.*;
import org.jppf.utils.configuration.ConfigurationHelper;
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
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether trace-level logging is enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * Constant for JPPF automatic connection discovery
   */
  protected static final String VALUE_JPPF_DISCOVERY = "jppf_discovery";
  /**
   * The pool of threads used for submitting execution requests.
   * @exclude
   */
  protected ThreadPoolExecutor executor = null;
  /**
   * The JPPF configuration properties.
   */
  private TypedProperties config;
  /**
   * Performs server discovery.
   * @exclude
   */
  protected JPPFMulticastReceiverThread receiverThread = null;
  /**
   * Determines whether SSL communication is on or off.
   */
  protected boolean sslEnabled = false;
  /**
   * The submission manager.
   */
  private SubmissionManager submissionManager;
  /**
   * Handles the class loaders used for inbound class loading requests from the servers.
   */
  private final ClassLoaderRegistrationHandler classLoaderRegistrationHandler;
  /**
   * The list of listeners on the queue associated with this client.
   */
  private final List<ClientQueueListener> queueListeners = new CopyOnWriteArrayList<>();

  /**
   * Initialize this client with a specified application UUID.
   * @param uuid the unique identifier for this local client.
   * @param configuration the object holding the JPPF configuration.
   * @param listeners the listeners to add to this JPPF client to receive notifications of new connections.
   */
  public AbstractGenericClient(final String uuid, final TypedProperties configuration, final ClientListener... listeners) {
    super(uuid);
    this.classLoaderRegistrationHandler = new ClassLoaderRegistrationHandler();
    for (ClientListener listener : listeners) addClientListener(listener);
    init(configuration);
  }

  /**
   * Initialize this client with the specified configuration.
   * @param configuration the configuration to use with this client.
   */
  protected void init(final TypedProperties configuration) {
    closed.set(false);
    resetting.set(false);
    this.config = initConfig(configuration);
    sslEnabled = this.config.getBoolean("jppf.ssl.enabled", false);
    log.info("JPPF client starting with sslEnabled = " + sslEnabled);
    try {
      HookFactory.registerSPIMultipleHook(JPPFClientStartupSPI.class, null, null).invoke("run");
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    Runnable r = new Runnable() {
      @Override
      public void run() {
        initPools(config);
      }
    };
    new Thread(r, "InitPools").start();
  }

  /**
   * Get JPPF configuration properties. These properties are unmodifiable.
   * @return <code>TypedProperties</code> instance. With JPPF configuration.
   */
  public TypedProperties getConfig() {
    return config;
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
   * {@inheritDoc}
   * @exclude
   */
  @Override
  @SuppressWarnings("unchecked")
  protected void initPools(final TypedProperties config) {
    if (debugEnabled) log.debug("initializing connections");
    int coreThreads = Runtime.getRuntime().availableProcessors();
    //LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(coreThreads);
    executor = new ThreadPoolExecutor(coreThreads, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, queue, new JPPFThreadFactory("JPPF Client"));
    executor.allowCoreThreadTimeOut(true);
    if (config.getBoolean("jppf.local.execution.enabled", false)) setLocalExecutionEnabled(true);
    if (config.getBoolean("jppf.remote.execution.enabled", true)) initRemotePools(config);
  }

  /**
   * Initialize remote connection pools according to configuration.
   * @param config The JPPF configuration properties.
   * @exclude
   */
  protected void initRemotePools(final TypedProperties config) {
    try {
      boolean initPeers;
      if (config.getBoolean("jppf.discovery.enabled", true)) {
        final int priority = config.getInt("jppf.discovery.priority", 0);
        boolean acceptMultipleInterfaces = config.getBoolean("jppf.discovery.acceptMultipleInterfaces", false);
        if (debugEnabled) log.debug("initializing connections from discovery with priority = {} and acceptMultipleInterfaces = {}", priority, acceptMultipleInterfaces);
        receiverThread = new JPPFMulticastReceiverThread(new JPPFMulticastReceiverThread.ConnectionHandler() {
          @Override
          public void onNewConnection(final String name, final JPPFConnectionInformation info) {
            ConfigurationHelper ch = new ConfigurationHelper(config);
            int poolSize = ch.getInt("jppf.pool.size", 1, 1, Integer.MAX_VALUE);
            int jmxPoolSize = ch.getInt("jppf.jmx.pool.size", 1, 1, Integer.MAX_VALUE);
            newConnection(name, info, priority, poolSize, sslEnabled, jmxPoolSize);
          }
        }, new IPFilter(config), acceptMultipleInterfaces);
        new Thread(receiverThread).start();
        initPeers = false;
      } else {
        receiverThread = null;
        initPeers = true;
      }

      if (debugEnabled) log.debug("found peers in the configuration");
      String discoveryNames = config.getString("jppf.drivers");
      if ((discoveryNames == null) || "".equals(discoveryNames.trim())) discoveryNames = "default-driver";
      if (debugEnabled) log.debug("list of drivers: " + discoveryNames);
      String[] names = discoveryNames.split("\\s");
      for (String name : names) {
        initPeers |= VALUE_JPPF_DISCOVERY.equals(name);
      }

      if (initPeers) {
        for (String name : names) {
          if (!VALUE_JPPF_DISCOVERY.equals(name)) {
            JPPFConnectionInformation info = new JPPFConnectionInformation();
            info.host = config.getString(name + ".jppf.server.host", "localhost");
            int port = config.getInt(name + ".jppf.server.port", sslEnabled ? 11443 : 11111);
            if (!sslEnabled) info.serverPorts = new int[] { port };
            else info.sslServerPorts = new int[] { port };
            if (!sslEnabled) info.managementPort = config.getInt(name + ".jppf.management.port", -1);
            else info.sslManagementPort = config.getInt(name + ".jppf.management.port", -1);
            int priority = config.getInt(name + ".jppf.priority", 0);
            if (receiverThread != null) receiverThread.addConnectionInformation(info);
            ConfigurationHelper ch = new ConfigurationHelper(config);
            int poolSize = ch.getInt(name + ".jppf.pool.size", 1, 1, Integer.MAX_VALUE);
            int jmxPoolSize = ch.getInt(name + ".jppf.jmx.pool.size", 1, 1, Integer.MAX_VALUE);
            newConnection(name, info, priority, poolSize, sslEnabled, jmxPoolSize);
          }
        }
      }
    } catch(Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Called when a new connection pool is read from the configuration (as opposed to discovered from the network).
   * @param name the name assigned to the connection pool.
   * @param info the information required for the connection to connect to the driver.
   * @param priority the priority assigned to the connection.
   * @param poolSize the size of the connection pool.
   * @param ssl determines whether the pool is for SSL connections.
   * @param jmxPoolSize the core size of the JMX connections pool.
   * @exclude
   */
  protected void newConnection(final String name, final JPPFConnectionInformation info, final int priority, final int poolSize, final boolean ssl, final int jmxPoolSize) {
    int size = poolSize > 0 ? poolSize : 1;
    JPPFConnectionPool pool = new JPPFConnectionPool(this, poolSequence.incrementAndGet(), name, priority, poolSize, ssl, jmxPoolSize);
    pool.setDriverHost(info.host);
    pool.setDriverPort(ssl ? info.sslServerPorts[0] : info.serverPorts[0]);
    synchronized(pools) {
      pools.putValue(priority, pool);
      pendingPools.add(pool);
    }
    for (int i=1; i<=size; i++) {
      if (isClosed()) return;
      submitNewConnection(info, pool);
    }
  }

  /**
   * Called to submit the initialization of a new connection.
   * @param info the information required for the connection to connect to the driver.
   * @param pool thez connection pool to which the connection belongs.
   * @exclude
   */
  protected void submitNewConnection(final JPPFConnectionInformation info, final JPPFConnectionPool pool) {
    AbstractJPPFClientConnection c = createConnection(info.uuid, pool.getName() + "-" + pool.nextSequence(), info, pool);
    newConnection(c);
  }

  /**
   * Create a new driver connection based on the specified parameters.
   * @param uuid the uuid of the remote JPPF driver.
   * @param name the name of the connection.
   * @param info the driver connection information.
   * @param pool id of the connection pool the connection belongs to.
   * @return an instance of a subclass of {@link AbstractJPPFClientConnection}.
   * @exclude
   */
  protected abstract AbstractJPPFClientConnection createConnection(String uuid, String name, JPPFConnectionInformation info, final JPPFConnectionPool pool);

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public void newConnection(final JPPFClientConnection c) {
    if (isClosed()) return;
    log.info("connection [" + c.getName() + "] created");
    c.addClientConnectionStatusListener(this);
    c.setStatus(JPPFClientConnectionStatus.NEW);
    pendingConnections.add(c);
    executor.submit(new ConnectionInitializer(c));
    fireNewConnection(c);
    if (debugEnabled) log.debug("end of connection [" + c.getName() + "] created");
  }

  /**
   * Invoked when the status of a connection has changed to <code>JPPFClientConnectionStatus.FAILED</code>.
   * @param connection the connection that failed.
   * @exclude
   */
  @Override
  protected void connectionFailed(final JPPFClientConnection connection) {
    log.info("Connection [" + connection.getName() + "] failed");
    if (receiverThread != null) receiverThread.removeConnectionInformation(connection.getDriverUuid());
    connection.close();
    removeClientConnection(connection);
    fireConnectionFailed(connection);
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
    if (closed.get()) return;
    if (debugEnabled) log.debug("closing JPPF client");
    closed.set(true);
    if (debugEnabled) log.debug("unregistering startup classes");
    HookFactory.unregister(JPPFClientStartupSPI.class);
    if (debugEnabled) log.debug("closing submission manager");
    if (submissionManager != null) {
      if (reset) {
        submissionManager.reset();
      } else {
        submissionManager.close();
        submissionManager = null;
      }
    }
    if (debugEnabled) log.debug("closing broadcast receiver");
    if (receiverThread != null) {
      receiverThread.close();
      receiverThread = null;
    }
    if (debugEnabled) log.debug("closing executor");
    if (executor != null) {
      executor.shutdownNow();
      executor = null;
    }
    if (debugEnabled) log.debug("clearing registered class loaders");
    classLoaderRegistrationHandler.close();
    super.close();
  }

  /**
   * Determine whether local execution is enabled on this client.
   * @return <code>true</code> if local execution is enabled, <code>false</code> otherwise.
   */
  public boolean isLocalExecutionEnabled() {
    SubmissionManager submissionManager = getSubmissionManager();
    return (submissionManager != null) && submissionManager.isLocalExecutionEnabled();
  }

  /**
   * Specify whether local execution is enabled on this client.
   * @param localExecutionEnabled <code>true</code> to enable local execution, <code>false</code> otherwise
   */
  public void setLocalExecutionEnabled(final boolean localExecutionEnabled) {
    SubmissionManager submissionManager = getSubmissionManager();
    if (submissionManager != null) submissionManager.setLocalExecutionEnabled(localExecutionEnabled);
  }

  /**
   * Determine whether there is a client connection available for execution.
   * @return true if at least one connection is available, false otherwise.
   */
  public boolean hasAvailableConnection() {
    SubmissionManager submissionManager = getSubmissionManager();
    return (submissionManager != null) && submissionManager.hasAvailableConnection();
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public void statusChanged(final ClientConnectionStatusEvent event) {
    super.statusChanged(event);
    SubmissionManager submissionManager = getSubmissionManager();
    if(submissionManager != null) {
      ClientConnectionStatusListener listener = submissionManager.getClientConnectionStatusListener();
      if(listener != null) listener.statusChanged(event);
      if (submissionManager instanceof ThreadSynchronization) ((ThreadSynchronization) submissionManager).wakeUp();
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
   * Get the submission manager for this JPPF client.
   * @return a <code>JPPFSubmissionManager</code> instance.
   * @exclude
   */
  public SubmissionManager getSubmissionManager() {
    synchronized(this) {
      if ((submissionManager == null) && !isClosed()) submissionManager = createSubmissionManager();
    }
    return submissionManager;
  }

  /**
   * Set the submission manager for this JPPF client.
   * @param submissionManager a <code>JPPFSubmissionManager</code> instance.
   * @exclude
   */
  protected void setSubmissionManager(final SubmissionManager submissionManager) {
    synchronized (this) {
      this.submissionManager = submissionManager;
    }
  }

  /**
   * Create the submission manager for this JPPF client.
   * @return a <code>JPPFSubmissionManager</code> instance.
   */
  protected abstract SubmissionManager createSubmissionManager();

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
    return getSubmissionManager().cancelJob(jobId);
  }

  /**
   * Get a class loader associated with a job.
   * @param uuid unique id assigned to classLoader. Added as temporary fix for problems hanging jobs.
   * @return a <code>RegisteredClassLoader</code> instance.
   * @exclude
   */
  public RegisteredClassLoader getRegisteredClassLoader(final String uuid) {
    return classLoaderRegistrationHandler.getRegisteredClassLoader(uuid);
  }

  /**
   * Register class loader with this submission manager.
   * @param cl a <code>ClassLoader</code> instance.
   * @param uuid unique id assigned to classLoader. Added as temporary fix for problems hanging jobs.
   * @return a <code>RegisteredClassLoader</code> instance.
   * @exclude
   */
  public RegisteredClassLoader registerClassLoader(final ClassLoader cl, final String uuid) {
    return classLoaderRegistrationHandler.registerClassLoader(cl, uuid);
  }

  /**
   * Unregisters class loader from this submission manager.
   * @param registeredClassLoader a <code>RegisteredClassLoader</code> instance.
   * @exclude
   */
  protected void unregisterClassLoader(final RegisteredClassLoader registeredClassLoader) {
    classLoaderRegistrationHandler.unregister(registeredClassLoader);
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
   * @since 4.1
   */
  protected void fireQueueEvent(final QueueEvent<ClientJob, ClientJob, ClientTaskBundle> qEvent, final boolean jobAdded) {
    ClientQueueEvent event = new ClientQueueEvent(this, qEvent.getBundleWrapper().getJob(), (JPPFPriorityQueue) qEvent.getQueue());
    if (jobAdded) {
      for (ClientQueueListener listener: queueListeners) listener.jobAdded(event);
    } else {
      for (ClientQueueListener listener: queueListeners) listener.jobRemoved(event);
    }
  }

  /**
   * {@inheritDoc}
   * @exclude
   * @since 4.1
   */
  @Override
  public void bundleAdded(final QueueEvent event) {
    fireQueueEvent(event, true);
  }

  /**
   * {@inheritDoc}
   * @exclude
   * @since 4.1
   */
  @Override
  public void bundleRemoved(final QueueEvent event) {
    fireQueueEvent(event, false);
  }
}
