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
package org.jppf.node;

import java.security.*;
import java.util.*;
import java.util.concurrent.*;

import org.jppf.*;
import org.jppf.classloader.*;
import org.jppf.comm.discovery.*;
import org.jppf.logging.jmx.JmxMessageNotifier;
import org.jppf.management.JMXServer;
import org.jppf.node.initialization.*;
import org.jppf.process.LauncherListener;
import org.jppf.security.JPPFPolicy;
import org.jppf.utils.*;
import org.jppf.utils.hooks.HookFactory;
import org.slf4j.*;

/**
 * Bootstrap class for launching a JPPF node. The node class is dynamically loaded from a remote server.
 * @author Laurent Cohen
 */
public class NodeRunner
{
  // this static block must be the first thing executed when this class is loaded
  static {
    JPPFInitializer.init();
  }
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
   * Container for data stored at the JVM level.
   */
  private static Hashtable<Object, Object> persistentData = new Hashtable<>();
  /**
   * Used to executed a JVM termination task;
   */
  private static ExecutorService executor = Executors.newFixedThreadPool(1);
  /**
   * The JPPF node.
   */
  private static NodeInternal node = null;
  /**
   * Used to synchronize start and stop methods when the node is run as a service.
   */
  private static SimpleObjectLock serviceLock = new SimpleObjectLock();
  /**
   * This node's universal identifier.
   */
  private static String uuid = JPPFConfiguration.getProperties().getString("jppf.node.uuid", JPPFUuid.normalUUID());
  /**
   * The offline node flag.
   */
  private static boolean offline = JPPFConfiguration.getProperties().getBoolean("jppf.node.offline", false);
  /**
   * The initial configuration, such as read from the config file.
   * The JPPF config is modified by the discovery mechanism, so we want to store the initial values somewhere.
   */
  private static TypedProperties initialConfig = null;
  /**
   * Determines whether this node is currently shutting down.
   */
  private static boolean shuttingDown = false;

  /**
   * Run a node as a standalone application.
   * @param args not used.
   */
  public static void main(final String...args) {
    node = null;
    try {
      // initialize the jmx logger
      new JmxMessageNotifier();
      Thread.setDefaultUncaughtExceptionHandler(new JPPFDefaultUncaughtExceptionHandler());
      VersionUtils.logVersionInformation("node", uuid);
      initialConfig = new TypedProperties(JPPFConfiguration.getProperties());
      if (debugEnabled) log.debug("launching the JPPF node");
      HookFactory.registerSPIMultipleHook(InitializationHook.class, null, null);
      if ((args == null) || (args.length <= 0))
        throw new JPPFException("The node should be run with an argument representing a valid TCP port or 'noLauncher'");
      if (!"noLauncher".equals(args[0])) {
        int port = Integer.parseInt(args[0]);
        new LauncherListener(port).start();
      }
    } catch(Exception e) {
      log.error(e.getMessage(), e);
      System.exit(1);
    }
    try {
      while (true) {
        try {
          node = createNode();
          node.run();
        } catch(JPPFNodeReconnectionNotification e) {
          if (debugEnabled) log.debug("received reconnection notification : {}", ExceptionUtils.getStackTrace(e));
          if (classLoader != null) classLoader.close();
          classLoader = null;
          if (node != null) node.stopNode();
          unsetSecurity();
        }
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Run a node as a standalone application.
   * @param args not used.
   * @exclude
   */
  public static void start(final String...args) {
    main(args);
    serviceLock.goToSleep();
  }

  /**
   * Run a node as a standalone application.
   * @param args not used.
   * @exclude
   */
  public static void stop(final String...args) {
    serviceLock.wakeUp();
    System.exit(0);
  }

  /**
   * Start the node.
   * @return the node that was started, as a <code>MonitoredNode</code> instance.
   * @throws Exception if the node failed to run or couldn't connect to the server.
   * @exclude
   */
  public static NodeInternal createNode() throws Exception
  {
    HookFactory.invokeHook(InitializationHook.class, "initializing", new UnmodifiableTypedProperties(initialConfig));
    if (JPPFConfiguration.getProperties().getBoolean("jppf.discovery.enabled", true)) discoverDriver();
    setSecurity();
    String className = "org.jppf.server.node.remote.JPPFRemoteNode";
    Class clazz = getJPPFClassLoader().loadClass(className);
    NodeInternal node = (NodeInternal) clazz.newInstance();
    if (debugEnabled) log.debug("Created new node instance: " + node);
    return node;
  }

  /**
   * Automatically discover the server connection information using a datagram multicast.
   * Upon receiving the connection information, the JPPF configuration is modified to take into
   * account the discovered information. If no information could be received, the node relies on
   * the static information in the configuration file.
   */
  private static void discoverDriver() {
    TypedProperties config = JPPFConfiguration.getProperties();
    JPPFMulticastReceiver receiver = new JPPFMulticastReceiver(new IPFilter(config));
    JPPFConnectionInformation info = receiver.receive();
    receiver.setStopped(true);
    if (info == null) {
      if (debugEnabled) log.debug("Could not auto-discover the driver connection information");
      restoreInitialConfig();
      return;
    }
    if (debugEnabled) log.debug("Discovered driver: " + info);
    boolean ssl = config.getBoolean("jppf.ssl.enabled", false);
    config.setProperty("jppf.server.host", info.host);
    config.setProperty("jppf.server.port", String.valueOf(ssl ? info.sslServerPorts[0] : info.serverPorts[0]));
    if (info.managementHost != null) config.setProperty("jppf.management.host", info.managementHost);
    if (info.recoveryPort >= 0) config.setProperty("jppf.recovery.server.port", "" + info.recoveryPort);
    else config.setProperty("jppf.recovery.enabled", "false");
  }

  /**
   * Restore the configuration from the sna^shot taken at startup time.
   */
  private static void restoreInitialConfig() {
    TypedProperties config = JPPFConfiguration.getProperties();
    for (Map.Entry<Object, Object> entry: initialConfig.entrySet()) {
      if ((entry.getKey() instanceof String) && (entry.getValue() instanceof String)) {
        config.setProperty((String) entry.getKey(), (String) entry.getValue());
      }
    }
  }

  /**
   * Set the security manager with the permission granted in the policy file.
   * @throws Exception if the security could not be set.
   */
  private static void setSecurity() throws Exception {
    if (!securityManagerSet) {
      TypedProperties props = JPPFConfiguration.getProperties();
      String s = props.getString("jppf.policy.file");
      if (s != null) {
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
  private static void unsetSecurity() {
    if (securityManagerSet) {
      if (debugEnabled) log.debug("un-setting security");
      PrivilegedAction<Object> pa = new PrivilegedAction<Object>() {
        @Override
        public Object run() {
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
   * @exclude
   */
  public static AbstractJPPFClassLoader getJPPFClassLoader() {
    synchronized(JPPFClassLoader.class) {
      if (classLoader == null) {
        PrivilegedAction<JPPFClassLoader> pa = new PrivilegedAction<JPPFClassLoader>() {
          @Override
          public JPPFClassLoader run() {
            return new JPPFClassLoader(offline ? null : new RemoteClassLoaderConnection(), NodeRunner.class.getClassLoader());
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
  public static void setPersistentData(final Object key, final Object value) {
    persistentData.put(key, value);
  }

  /**
   * Get a persistent object given its key.
   * @param key the key used to retrieve the persistent object.
   * @return the value associated with the key.
   */
  public static Object getPersistentData(final Object key) {
    return persistentData.get(key);
  }

  /**
   * Remove a persistent object.
   * @param key the key associated with the object to remove.
   * @return the value associated with the key, or null if the key was not found.
   */
  public static Object removePersistentData(final Object key) {
    return persistentData.remove(key);
  }

  /**
   * Get the JPPF node.
   * @return a <code>Node</code> instance.
   * @exclude
   */
  public static Node getNode() {
    return node;
  }

  /**
   * Shutdown and eventually restart the node.
   * @param node the node to shutdown or restart.
   * @param restart determines whether this node should be restarted by the node launcher.
   * @exclude
   */
  public static void shutdown(final NodeInternal node, final boolean restart) {
    //executor.submit(new ShutdownOrRestart(restart));
    new ShutdownOrRestart(restart, node).run();
  }

  /**
   * Stop the JMX server.
   */
  private static void stopJmxServer() {
    try {
      final JMXServer jmxServer = node.getJmxServer();
      if (jmxServer != null) {
        jmxServer.stop();
        Runnable r = new Runnable() {
          @Override
          public void run() {
            try {
              jmxServer.stop();
            } catch (Exception ignore) {
            }
          }
        };
        Future<?> f = executor.submit(r);
        // we don't want to wait forever for the connection to close
        try {
          f.get(1000L, TimeUnit.MILLISECONDS);
        } catch (Exception ignore) {
        }
        //if (!f.isDone()) f.cancel(true);
      }
    } catch (Exception ignore) {
    }
  }

  /**
   * Task used to terminate the JVM.
   * @exclude
   */
  public static class ShutdownOrRestart implements Runnable {
    /**
     * True if the node is to be restarted, false to only shut it down.
     */
    private boolean restart = false;
    /**
     * True if the node is to be restarted, false to only shut it down.
     */
    private final NodeInternal node;

    /**
     * Initialize this task.
     * @param restart true if the node is to be restarted, false to only shut it down.
     * @param node this node.
     */
    public ShutdownOrRestart(final boolean restart, final NodeInternal node) {
      this.restart = restart;
      this.node = node;
    }

    /**
     * Execute this task.
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
      AccessController.doPrivileged(new PrivilegedAction<Object>() {
        @Override
        public Object run() {
          node.stopNode();
          // close the JMX server connection to avoid request being sent again by the client.
          stopJmxServer();
          try {
            Thread.sleep(500L);
          } catch(Exception ignore) {
          }
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
  public static String getUuid() {
    return uuid;
  }

  /**
   * Determine whether this node is currently shutting down.
   * @return <code>true</code> if the node is shutting down, <code>false</code> otherwise.
   */
  public synchronized static boolean isShuttingDown() {
    return shuttingDown;
  }

  /**
   * Specify whether this node is currently shutting down.
   * @param shuttingDown <code>true</code> if the node is shutting down, <code>false</code> otherwise.
   */
  public synchronized static void setShuttingDown(final boolean shuttingDown) {
    NodeRunner.shuttingDown = shuttingDown;
  }

  /**
   * Get the offline node flag.
   * @return <code>true</code> if the node is offline, <code>false</code> otherwise.
   */
  public static boolean isOffline()
  {
    return offline;
  }
}
