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
package org.jppf.node;

import java.security.*;
import java.util.*;
import java.util.concurrent.*;

import org.jppf.*;
import org.jppf.classloader.*;
import org.jppf.comm.discovery.*;
import org.jppf.comm.socket.SocketWrapper;
import org.jppf.logging.jmx.JmxMessageNotifier;
import org.jppf.node.initialization.InitializationHooksHandler;
import org.jppf.process.LauncherListener;
import org.jppf.security.JPPFPolicy;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Bootstrap class for launching a JPPF node. The node class is dynamically loaded from a remote server.
 * @author Laurent Cohen
 */
public class NodeRunner
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(NodeRunner.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The ClassLoader used for loading the classes of the framework.
   */
  private static AbstractJPPFClassLoader classLoader = null;
  /**
   * Determine whether a security manager has already been set.
   */
  private static boolean securityManagerSet = false;
  /**
   * The actual socket connection used by the node.
   * Provided as a means to reuse it when the node updates its own code, therefore removing the need to
   * disconnect from the server.
   */
  private static SocketWrapper nodeSocket = null;
  /**
   * Container for data stored at the JVM level.
   */
  private static Hashtable<Object, Object> persistentData = new Hashtable<Object, Object>();
  /**
   * Used to executed a JVM termination task;
   */
  private static ExecutorService executor = Executors.newFixedThreadPool(1);
  /**
   * Task used to shutdown the node.
   */
  private static final ShutdownOrRestart SHUTDOWN_TASK = new ShutdownOrRestart(false);
  /**
   * Task used to restart the node.
   */
  private static final ShutdownOrRestart RESTART_TASK = new ShutdownOrRestart(true);
  /**
   * The JPPF node.
   */
  private static Node node = null;
  /**
   * Used to synchronize start and stop methods when the node is run as a service.
   */
  private static SimpleObjectLock serviceLock = new SimpleObjectLock();
  /**
   * This node's universal identifier.
   */
  private static String uuid = new JPPFUuid(JPPFUuid.HEXADECIMAL_CHAR, 32).toString().toUpperCase();
  /**
   * Handles include and exclude IP filters.
   */
  private static IPFilter ipFilter = new IPFilter(JPPFConfiguration.getProperties());
  /**
   * The initial configuration, such as read from the config file.
   * The JPPF config is modified by the discovery mechanism, so we want to store the initial values somewhere.
   */
  private static TypedProperties initialConfig = null;
  /**
   * Loads and invokes node initialization hooks defined via their SPI definition.
   */
  private static InitializationHooksHandler hooksHandler = null;

  /**
   * Run a node as a standalone application.
   * @param args not used.
   */
  public static void main(final String...args)
  {
    node = null;
    try
    {
      // initialize the jmx logger
      new JmxMessageNotifier();
      initialConfig = new TypedProperties(JPPFConfiguration.getProperties());
      if (debugEnabled) log.debug("launching the JPPF node");
      hooksHandler = new InitializationHooksHandler(initialConfig);
      hooksHandler.loadHooks();
      if ((args == null) || (args.length <= 0))
        throw new JPPFException("The node should be run with an argument representing a valid TCP port or 'noLauncher'");
      if (!"noLauncher".equals(args[0]))
      {
        int port = Integer.parseInt(args[0]);
        new LauncherListener(port).start();
      }
    }
    catch(Exception e)
    {
      log.error(e.getMessage(), e);
      System.exit(1);
    }
    try
    {
      int pid = SystemUtils.getPID();
      if (pid > 0) System.out.println("node process id: " + pid);
      log.info("starting node, uuid=" + uuid + ", PID=" + pid);
      // to ensure VersionUtils is loaded by the same class loader as this class.
      VersionUtils.getBuildNumber();
      while (true)
      {
        try
        {
          node = createNode();
          node.run();
        }
        catch(JPPFNodeReconnectionNotification e)
        {
          if (debugEnabled) log.debug("received reconnection notification");
          if (classLoader != null) classLoader.close();
          classLoader = null;
          if (node != null) node.stopNode();
          unsetSecurity();
        }
      }
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
  }

  /**
   * Run a node as a standalone application.
   * @param args not used.
   */
  public static void start(final String...args)
  {
    main(args);
    serviceLock.goToSleep();
  }

  /**
   * Run a node as a standalone application.
   * @param args not used.
   */
  public static void stop(final String...args)
  {
    serviceLock.wakeUp();
    System.exit(0);
  }

  /**
   * Start the node.
   * @return the node that was started, as a <code>MonitoredNode</code> instance.
   * @throws Exception if the node failed to run or couldn't connect to the server.
   */
  public static Node createNode() throws Exception
  {
    hooksHandler.callHooks();
    if (JPPFConfiguration.getProperties().getBoolean("jppf.discovery.enabled", true)) discoverDriver();
    setSecurity();
    String className = "org.jppf.server.node.remote.JPPFRemoteNode";
    Class clazz = getJPPFClassLoader().loadJPPFClass(className);
    Node node = (Node) clazz.newInstance();
    if (debugEnabled) log.debug("Created new node instance: " + node);
    node.setSocketWrapper(nodeSocket);
    return node;
  }

  /**
   * Automatically discover the server connection information using a datagram multicast.
   * Upon receiving the connection information, the JPPF configuration is modified to take into
   * account the discovered information. If no information could be received, the node relies on
   * the static information in the configuration file.
   */
  public static void discoverDriver()
  {
    JPPFMulticastReceiver receiver = new JPPFMulticastReceiver(ipFilter);
    JPPFConnectionInformation info = receiver.receive();
    receiver.setStopped(true);
    if (info == null)
    {
      if (debugEnabled) log.debug("Could not auto-discover the driver connection information");
      restoreInitialConfig();
      return;
    }
    if (debugEnabled) log.debug("Discovered driver: " + info);
    TypedProperties config = JPPFConfiguration.getProperties();
    config.setProperty("jppf.server.host", info.host);
    config.setProperty("jppf.server.port", StringUtils.buildString(info.serverPorts));
    if (info.managementHost != null) config.setProperty("jppf.management.host", info.managementHost);
    if (info.recoveryPort >= 0)
    {
      config.setProperty("jppf.recovery.server.port", "" + info.recoveryPort);
    }
    else config.setProperty("jppf.recovery.enabled", "false");
  }

  /**
   * Restore the configuration from the sna^shot taken at startup time.
   */
  private static void restoreInitialConfig()
  {
    TypedProperties config = JPPFConfiguration.getProperties();
    for (Map.Entry<Object, Object> entry: initialConfig.entrySet())
    {
      if ((entry.getKey() instanceof String) && (entry.getValue() instanceof String))
      {
        config.setProperty((String) entry.getKey(), (String) entry.getValue());
      }
    }
  }

  /**
   * Set the security manager with the permission granted in the policy file.
   * @throws Exception if the security could not be set.
   */
  public static void setSecurity() throws Exception
  {
    if (!securityManagerSet)
    {
      TypedProperties props = JPPFConfiguration.getProperties();
      String s = props.getString("jppf.policy.file");
      if (s != null)
      {
        if (debugEnabled) log.debug("setting security");
        //java.rmi.server.hostname
        String rmiHostName = props.getString("jppf.management.host", "localhost");
        System.setProperty("java.rmi.server.hostname", rmiHostName);
        Policy.setPolicy(new JPPFPolicy(getJPPFClassLoader()));
        System.setSecurityManager(new SecurityManager());
        securityManagerSet = true;
      }
    }
  }

  /**
   * Set the security manager with the permission granted in the policy file.
   */
  public static void unsetSecurity()
  {
    if (securityManagerSet)
    {
      if (debugEnabled) log.debug("un-setting security");
      PrivilegedAction<Object> pa = new PrivilegedAction<Object>()
      {
        @Override
        public Object run()
        {
          System.setSecurityManager(null);
          return null;
        }
      };
      AccessController.doPrivileged(pa);
      securityManagerSet = false;
    }
  }

  /**
   * Get the main classloader for the node. This method performs a lazy initialization of the classloader.
   * @return a <code>AbstractJPPFClassLoader</code> used for loading the classes of the framework.
   */
  public static AbstractJPPFClassLoader getJPPFClassLoader()
  {
    synchronized(JPPFClassLoader.class)
    {
      if (classLoader == null)
      {
        PrivilegedAction<JPPFClassLoader> pa = new PrivilegedAction<JPPFClassLoader>()
        {
          @Override
          public JPPFClassLoader run()
          {
            return new JPPFClassLoader(NodeRunner.class.getClassLoader());
          }
        };
        classLoader = AccessController.doPrivileged(pa);
        Thread.currentThread().setContextClassLoader(classLoader);
      }
      return classLoader;
    }
  }

  /**
   * Set a persistent object with the specified key.
   * @param key the key associated with the object's value.
   * @param value the object to persist.
   */
  public static synchronized void setPersistentData(final Object key, final Object value)
  {
    persistentData.put(key, value);
  }

  /**
   * Get a persistent object given its key.
   * @param key the key used to retrieve the persistent object.
   * @return the value associated with the key.
   */
  public static synchronized Object getPersistentData(final Object key)
  {
    return persistentData.get(key);
  }

  /**
   * Remove a persistent object.
   * @param key the key associated with the object to remove.
   * @return the value associated with the key, or null if the key was not found.
   */
  public static synchronized Object removePersistentData(final Object key)
  {
    return persistentData.remove(key);
  }

  /**
   * Get the JPPF node.
   * @return a <code>Node</code> instance.
   */
  public static Node getNode()
  {
    return node;
  }

  /**
   * Shutdown and eventually restart the node.
   * @param node the node to shutdown or restart.
   * @param restart determines whether this node should be restarted by the node launcher.
   */
  public static void shutdown(final Node node, final boolean restart)
  {
    //node.stopNode(true);
    //executor.submit(restart ? RESTART_TASK : SHUTDOWN_TASK);
    new ShutdownOrRestart(restart).run();
  }

  /**
   * Task used to terminate the JVM.
   */
  public static class ShutdownOrRestart implements Runnable
  {
    /**
     * True if the node is to be restarted, false to only shut it down.
     */
    private boolean restart = false;

    /**
     * Initialize this task.
     * @param restart true if the node is to be restarted, false to only shut it down.
     */
    public ShutdownOrRestart(final boolean restart)
    {
      this.restart = restart;
    }

    /**
     * Execute this task.
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run()
    {
      AccessController.doPrivileged(new PrivilegedAction<Object>()
      {
        @Override
        public Object run()
        {
          System.exit(restart ? 2 : 0);
          return null;
        }
      });
    }
  }

  /**
   * This node's universal identifier.
   * @return a uuid as a string.
   */
  public static String getUuid()
  {
    return uuid;
  }
}
