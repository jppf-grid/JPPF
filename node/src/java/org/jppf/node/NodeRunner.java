/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

import java.lang.reflect.Constructor;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.*;
import org.jppf.classloader.*;
import org.jppf.logging.jmx.JmxMessageNotifier;
import org.jppf.node.connection.*;
import org.jppf.node.initialization.InitializationHook;
import org.jppf.process.LauncherListener;
import org.jppf.security.JPPFPolicy;
import org.jppf.server.node.JPPFNode;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.jppf.utils.hooks.HookFactory;
import org.slf4j.*;

/**
 * Bootstrap class for launching a JPPF node. The node class is dynamically loaded from a remote server.
 * @author Laurent Cohen
 */
public class NodeRunner {
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
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
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
  private static JPPFNode node = null;
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
  private static boolean offline = JPPFConfiguration.get(JPPFProperties.NODE_OFFLINE);
  /**
   * The initial configuration, such as read from the config file.
   * The JPPF config is modified by the discovery mechanism, so we want to store the initial values somewhere.
   */
  private static TypedProperties initialConfig = null;
  /**
   * Determines whether this node is currently shutting down.
   */
  private static AtomicBoolean shuttingDown = new AtomicBoolean(false);
  /**
   * The current server connection information.
   */
  private static DriverConnectionInfo currentConnectionInfo = null;
  /**
   * 
   */
  private static LauncherListener launcherListener = null;
  /**
   * 
   */
  private static boolean ANDROID = JPPFConfiguration.get(JPPFProperties.NODE_ANDROID);

  /**
   * Run a node as a standalone application.
   * @param args not used.
   */
  public static void main(final String...args) {
    node = null;
    try {
      if (!ANDROID) new JmxMessageNotifier(); // initialize the jmx logger
      Thread.setDefaultUncaughtExceptionHandler(new JPPFDefaultUncaughtExceptionHandler());
      if (debugEnabled) log.debug("launching the JPPF node");
      VersionUtils.logVersionInformation("node", uuid);
      HookFactory.registerSPIMultipleHook(InitializationHook.class, null, null);
      HookFactory.registerConfigSingleHook(JPPFProperties.SERVER_CONNECTION_STRATEGY, DriverConnectionStrategy.class, new JPPFDefaultConnectionStrategy(), null);
      if ((args == null) || (args.length <= 0))
        throw new JPPFException("The node should be run with an argument representing a valid TCP port or 'noLauncher'");
      if (!"noLauncher".equals(args[0])) {
        int port = Integer.parseInt(args[0]);
        (launcherListener = new LauncherListener(port)).start();
      }
    } catch(Exception e) {
      log.error(e.getMessage(), e);
      System.exit(1);
    }
    try {
      ConnectionContext context = new ConnectionContext("Initial connection", null, ConnectionReason.INITIAL_CONNECTION_REQUEST);
      while (!getShuttingDown().get()) {
        try {
          if (initialConfig == null) initialConfig = new TypedProperties(JPPFConfiguration.getProperties());
          else restoreInitialConfig();
          node = createNode(context);
          if (launcherListener != null) launcherListener.setActionHandler(new ShutdownRestartNodeProtocolHandler(node));
          node.run();
        } catch(JPPFNodeReconnectionNotification e) {
          if (debugEnabled) log.debug("received reconnection notification : {}", ExceptionUtils.getStackTrace(e));
          context = new ConnectionContext(e.getMessage(), e.getCause(), e.getReason());
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
   * @param connectionContext provides context information on the new connection request to the driver.
   * @return the node that was started, as a <code>JPPFNode</code> instance.
   * @throws Exception if the node failed to run or couldn't connect to the server.
   * @exclude
   */
  public static JPPFNode createNode(final ConnectionContext connectionContext) throws Exception {
    HookFactory.invokeHook(InitializationHook.class, "initializing", new UnmodifiableTypedProperties(initialConfig));
    SystemUtils.printPidAndUuid("node", uuid);
    currentConnectionInfo = (DriverConnectionInfo) HookFactory.invokeHook(DriverConnectionStrategy.class, "nextConnectionInfo", currentConnectionInfo, connectionContext)[0];
    setSecurity();
    //String className = "org.jppf.server.node.remote.JPPFRemoteNode";
    String className = JPPFConfiguration.get(JPPFProperties.NODE_CLASS);
    Class<?> clazz = getJPPFClassLoader().loadClass(className);
    Constructor c = clazz.getConstructor(DriverConnectionInfo.class);
    JPPFNode node = (JPPFNode) c.newInstance(currentConnectionInfo);
    if (debugEnabled) log.debug("Created new node instance: " + node);
    return node;
  }

  /**
   * Restore the configuration from the snapshot taken at startup time.
   */
  public static void restoreInitialConfig() {
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
      String s = JPPFConfiguration.get(JPPFProperties.POLICY_FILE);
      if (s != null) {
        if (debugEnabled) log.debug("setting security");
        Policy.setPolicy(new JPPFPolicy(getJPPFClassLoader()));
        System.setSecurityManager(new SecurityManager());
        securityManagerSet = true;
      }
    }
  }

  /**
   * Set the security manager with the permissions granted in the policy file.
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
            return new JPPFClassLoader(offline ? null : new RemoteClassLoaderConnection(currentConnectionInfo), NodeRunner.class.getClassLoader());
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
      node.stopJmxServer();
      Runnable r = new Runnable() {
        @Override
        public void run() {
          try {
            node.stopJmxServer();
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
   * @return an {@link AtomicBoolean} instance whose value is <code>true</code> if the node is shutting down, <code>false</code> otherwise.
   */
  public static AtomicBoolean getShuttingDown() {
    return shuttingDown;
  }

  /**
   * Get the offline node flag.
   * @return <code>true</code> if the node is offline, <code>false</code> otherwise.
   */
  public static boolean isOffline() {
    return offline;
  }
}
