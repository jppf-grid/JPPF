/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

import org.jppf.client.event.ClientConnectionStatusEvent;
import org.jppf.client.loadbalancer.LoadBalancer;
import org.jppf.comm.discovery.*;
import org.jppf.startup.*;
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
public abstract class AbstractGenericClient extends AbstractJPPFClient
{
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
   */
  protected ThreadPoolExecutor executor = null;
  /**
   * The JPPF configuration properties.
   */
  protected TypedProperties config = null;
  /**
   * Performs server discovery.
   */
  protected JPPFMulticastReceiverThread receiverThread = null;
  /**
   * The load balancer for local versus remote execution.
   */
  protected LoadBalancer loadBalancer = new LoadBalancer();
  /**
   * Keeps a list of the valid connections not currently executing tasks.
   */
  protected Vector<JPPFClientConnection> availableConnections;
  /**
   * Mapping of class loader to requests uuids.
   */
  private final Map<String, ClassLoader> classLoaderMap = new Hashtable<String, ClassLoader>();

  /**
   * Initialize this client with an automatically generated application UUID.
   * @param configuration the object holding the JPPF configuration.
   */
  public AbstractGenericClient(final Object configuration)
  {
    super();
    initConfig(configuration);
    new JPPFStartupLoader().load(JPPFClientStartupSPI.class);
    initPools();
  }

  /**
   * Initialize this client with a specified application UUID.
   * @param uuid the unique identifier for this local client.
   * @param configuration the object holding the JPPF configuration.
   */
  public AbstractGenericClient(final String uuid, final Object configuration)
  {
    super(uuid);
    initConfig(configuration);
    new JPPFStartupLoader().load(JPPFClientStartupSPI.class);
    initPools();
  }

  /**
   * Initialize this client's configuration.
   * @param configuration an object holding the JPPF configuration.
   */
  protected abstract void initConfig(Object configuration);

  /**
   * Read all client connection information from the configuration and initialize
   * the connection pools accordingly.
   */
  @Override
  @SuppressWarnings("unchecked")
  protected void initPools()
  {
    if (debugEnabled) log.debug("initializing connections");
    //loadBalancer = new LoadBalancer();
    LinkedBlockingQueue queue = new LinkedBlockingQueue();
    executor = new ThreadPoolExecutor(1, 1, Long.MAX_VALUE, TimeUnit.MICROSECONDS, queue, new JPPFThreadFactory("JPPF Client"));
    if (config.getBoolean("jppf.remote.execution.enabled", true))
    {
      initRemotePools(config);
    }
  }

  /**
   * Initialize remote connection pools according to configuration.
   * @param props The JPPF configuration properties.
   */
  private void initRemotePools(final TypedProperties props)
  {
    try
    {
      boolean initPeers;
      if (props.getBoolean("jppf.discovery.enabled", true))
      {
        if (debugEnabled) log.debug("initializing connections from discovery");
        boolean acceptMultipleInterfaces = props.getBoolean("jppf.discovery.acceptMultipleInterfaces", false);
        receiverThread = new JPPFMulticastReceiverThread(new JPPFMulticastReceiverThread.ConnectionHandler() {
          @Override
          public void onNewConnection(final String name, final JPPFConnectionInformation info) {
            newConnection(name, info, 0, props.getInt("jppf.pool.size", 1));
          }
        }, new IPFilter(props), acceptMultipleInterfaces);
        new Thread(receiverThread).start();
        //waitForPools(false);
        initPeers = false;
      }
      else
      {
        receiverThread = null;
        initPeers = true;
      }

      if (debugEnabled) log.debug("found peers in the configuration");
      String discoveryNames = props.getString("jppf.drivers");
      if ((discoveryNames == null) || "".equals(discoveryNames.trim())) discoveryNames = "default-driver";
      if (debugEnabled) log.debug("list of drivers: " + discoveryNames);
      String[] names = discoveryNames.split("\\s");
      for (String name : names) {
        initPeers |= VALUE_JPPF_DISCOVERY.equals(name);
      }

      if(initPeers)
      {
        for (String name : names) {
          if(!VALUE_JPPF_DISCOVERY.equals(name))
          {
            JPPFConnectionInformation info = new JPPFConnectionInformation();
            info.host = props.getString(String.format("%s.jppf.server.host", name), "localhost");
            // for backward compatibility with v2.x configurations
            int port = props.getAndReplaceInt(String.format("%s.jppf.server.port", name), String.format("%s.class.server.port", name), 11111, false);
            info.serverPorts = new int[] { port };
            info.managementPort = props.getInt(String.format("%s.jppf.management.port", name), 11198);
            int priority = props.getInt(String.format("%s.priority", name), 0);
            if(receiverThread != null) receiverThread.addConnectionInformation(info);
            newConnection(name, info, priority, props.getInt(name + ".jppf.pool.size", 1));
          }
        }
      }

      if(receiverThread != null)
      {
        new Thread(receiverThread, "PeerDiscoveryThread").start();
        //waitForPools(true);
      }
    }
    catch(Exception e)
    {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Called when a new connection is read from the configuration (as opposed to discovered from the network).
   * @param name the name assigned to the connection.
   * @param info the information required for the connection to connect to the driver.
   * @param priority the priority assigned to the connection.
   * @param poolSize the size of the associated connection pool.
   */
  protected void newConnection(final String name, final JPPFConnectionInformation info, final int priority, final int poolSize)
  {
    for (int i=1; i<=poolSize; i++)
    {
      AbstractJPPFClientConnection c = createConnection(info.uuid, (poolSize > 1) ? name + '-' + i : name, info);
      c.setPriority(priority);
      newConnection(c);
    }
  }

  /**
   * Create a new driver connection based on the specified parameters.
   * @param uuid the uuid of the JPPF client.
   * @param name the name of the connection.
   * @param info the driver connection information.
   * @return an instance of a subclass of {@link AbstractJPPFClientConnection}.
   */
  protected abstract AbstractJPPFClientConnection createConnection(String uuid, String name, JPPFConnectionInformation info);

  /**
   * Invoked when a new connection is created.
   * @param c the connection that failed.
   * @see org.jppf.client.AbstractJPPFClient#newConnection(org.jppf.client.JPPFClientConnection)
   */
  @Override
  public void newConnection(final JPPFClientConnection c)
  {
    log.info("Connection [" + c.getName() + "] created");
    addClientConnection(c);
    int n = getAllConnectionsCount() + 1;
    if (executor.getCorePoolSize() < n)
    {
      executor.setMaximumPoolSize(n);
      executor.setCorePoolSize(n);
    }
    executor.submit(new ConnectionInitializer(c));
    fireNewConnection(c);
  }

  /**
   * Wait a maximum time specified in the configuration until at least one connection is initialized.
   * After this time, control is returned to the main application, no matter how many connections are initialized.
   * @param returnOnEmptyPool determines whether this method should return immediately when the pool of connections is empty.
   */
  private void waitForPools(final boolean returnOnEmptyPool)
  {
    if (returnOnEmptyPool && getAllConnectionsCount() == 0) return;
    long maxWait = JPPFConfiguration.getProperties().getLong("jppf.client.max.init.time", 5000L);
    if (maxWait <= 0) return;
    long elapsed = 0;
    while (elapsed < maxWait)
    {
      long start = System.currentTimeMillis();
      if (getClientConnection(true) != null) break;
      try
      {
        Thread.sleep(50);
      }
      catch(Exception ignored)
      {
        if (debugEnabled) log.debug(ignored.getMessage(), ignored);
      }
      elapsed += System.currentTimeMillis() - start;
    }
  }

  /**
   * Close this client and release all the resources it is using.
   */
  @Override
  public void close()
  {
    super.close();
    if (receiverThread != null) receiverThread.setStopped(true);
    if (executor != null) executor.shutdownNow();
  }

  /**
   * Get the load balancer for local versus remote execution.
   * @return a <code>LoadBalancer</code> instance.
   */
  public LoadBalancer getLoadBalancer()
  {
    return loadBalancer;
  }

  /**
   * Get the JPPF configuration properties.
   * @return the configuration as a {@link TypedProperties} instance.
   */
  public TypedProperties getConfig()
  {
    return config;
  }

  /**
   * Wrapper class for the initialization of a client connection.
   */
  protected static class ConnectionInitializer implements Runnable
  {
    /**
     * The client connection to initialize.
     */
    private JPPFClientConnection c = null;
    /**
     * Instantiate this connection initializer with the specified client connection.
     * @param c the client connection to initialize.
     */
    public ConnectionInitializer(final JPPFClientConnection c)
    {
      this.c = c;
    }

    /**
     * Perform the initialization of a client connection.
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run()
    {
      if (debugEnabled) log.debug("initializing driver connection '" + c+ '\'');
      c.init();
    }
  }

  /**
   * Determine whether local execution is enabled on this client.
   * @return <code>true</code> if local execution is enabled, <code>false</code> otherwise.
   */
  public boolean isLocalExecutionEnabled()
  {
    return loadBalancer != null && loadBalancer.isLocalEnabled();
  }

  /**
   * Specify whether local execution is enabled on this client.
   * @param localExecutionEnabled <code>true</code> to enable local execution, <code>false</code> otherwise
   */
  public void setLocalExecutionEnabled(final boolean localExecutionEnabled)
  {
    if (loadBalancer != null) loadBalancer.setLocalEnabled(localExecutionEnabled);
  }

  /**
   * Determine whether there is a client connection available for execution.
   * @return true if at least one connection is available, false otherwise.
   */
  public boolean hasAvailableConnection()
  {
    if (traceEnabled)
    {
      StringBuilder sb = new StringBuilder();
      sb.append("available connections: ").append(getAvailableConnections().size()).append(", ");
      sb.append("local execution enabled: ").append(loadBalancer.isLocalEnabled()).append(", ");
      sb.append("locally executing: ").append(loadBalancer.isLocallyExecuting());
      log.trace(sb.toString());
    }
    return (!getAvailableConnections().isEmpty() || (loadBalancer.isLocalEnabled() && !loadBalancer.isLocallyExecuting()));
  }

  /**
   * Determine whether there is a client connection available for execution.
   * @return true if at least one connection is available, false otherwise.
   */
  public Pair<Boolean, Boolean> handleAvailableConnection()
  {
    synchronized(loadBalancer.getAvailableConnectionLock())
    {
      boolean b1 = hasAvailableConnection();
      boolean b2 = false;
      if (b1 && (loadBalancer.isLocalEnabled() && !loadBalancer.isLocallyExecuting()))
      {
        loadBalancer.setLocallyExecuting(true);
        b2 = true;
      }
      return new Pair<Boolean, Boolean>(b1, b2);
    }
  }

  /**
   * Determine whether there is a client connection available for execution.
   * @return true if at least one connection is available, false otherwise.
   */
  public boolean handleAvailableConnection2()
  {
    synchronized(loadBalancer.getAvailableConnectionLock())
    {
      boolean b1 = hasAvailableConnection();
      boolean b2 = false;
      if (b1 && (loadBalancer.isLocalEnabled() && !loadBalancer.isLocallyExecuting()))
      {
        loadBalancer.setLocallyExecuting(true);
        b2 = true;
      }
      return b1;
    }
  }

  /**
   * Invoked when the status of a client connection has changed.
   * @param event the event to notify of.
   * @see org.jppf.client.event.ClientConnectionStatusListener#statusChanged(org.jppf.client.event.ClientConnectionStatusEvent)
   */
  @Override
  public void statusChanged(final ClientConnectionStatusEvent event)
  {
    super.statusChanged(event);
    JPPFClientConnection c = (JPPFClientConnection) event.getClientConnectionStatusHandler();
    if (debugEnabled) log.debug("connection=" + c + ", availableConnections=" + availableConnections);
    switch(c.getStatus())
    {
      case ACTIVE:
        getAvailableConnections().add(c);
        break;
      default:
        getAvailableConnections().remove(c);
        break;
    }
  }

  /**
   * Get the list of available connections.
   * @return a vector of connections instances.
   */
  public Vector<JPPFClientConnection> getAvailableConnections()
  {
    if (availableConnections == null) availableConnections = new Vector<JPPFClientConnection>();
    return availableConnections;
  }

  /**
   * Get the pool of threads used for submitting execution requests.
   * @return a {@link ThreadPoolExecutor} instance.
   */
  public ThreadPoolExecutor getExecutor()
  {
    return executor;
  }

  /**
   * Add a request uuid to class loader mapping to this submission manager.
   * @param uuid the uuid of the request.
   * @param cl the class loader for the request.
   */
  public void addRequestClassLoader(final String uuid, final ClassLoader cl)
  {
    synchronized(classLoaderMap)
    {
      classLoaderMap.put(uuid, cl);
    }
  }

  /**
   * Add a request uuid to class loader mapping to this submission manager.
   * @param uuid the uuid of the request.
   */
  public void removeRequestClassLoader(final String uuid)
  {
    synchronized(classLoaderMap)
    {
      classLoaderMap.remove(uuid);
    }
  }

  /**
   * Get a class loader from its request uuid.
   * @param uuid the uuid of the request.
   * @return a <code>ClassLoader</code> instance, or null if none exists for the key.
   */
  public ClassLoader getRequestClassLoader(final String uuid)
  {
    synchronized(classLoaderMap)
    {
      return classLoaderMap.get(uuid);
    }
  }
}
