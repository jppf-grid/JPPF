/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

import org.jppf.*;
import org.jppf.classloader.*;
import org.jppf.logging.jmx.JmxMessageNotifier;
import org.jppf.node.connection.*;
import org.jppf.node.initialization.InitializationHook;
import org.jppf.process.LauncherListener;
import org.jppf.security.JPPFPolicy;
import org.jppf.server.node.JPPFNode;
import org.jppf.utils.*;
import org.jppf.utils.configuration.*;
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
   * Determine whether a security manager has already been set.
   */
  private static boolean securityManagerSet = false;
  /**
   * Container for data stored at the JVM level.
   */
  private static Hashtable<Object, Object> persistentData = new Hashtable<>();
  /**
   * The ClassLoader used for loading the classes of the framework.
   */
  private AbstractJPPFClassLoader classLoader;
  /**
   * The JPPF node.
   */
  private JPPFNode node;
  /**
   * This node's universal identifier.
   */
  private String uuid;
  /**
   * The offline node flag.
   */
  private boolean offline;
  /**
   * The initial configuration, such as read from the config file.
   * The JPPF config is modified by the discovery mechanism, so we want to store the initial values somewhere.
   */
  private TypedProperties initialConfig;
  /**
   * The current server connection information.
   */
  private DriverConnectionInfo currentConnectionInfo = null;
  /**
   * 
   */
  private LauncherListener launcherListener = null;
  /**
   * Whether this is anAndroid node.
   */
  private boolean android;
  /**
   * The configuration of the node.
   */
  private final TypedProperties configuration;

  /**
   * Initialize this node runner.
   * @param configuration the configuration of the node.
   */
  public NodeRunner(final TypedProperties configuration) {
    this.configuration = configuration;
    if (configuration.getDefaults() != null) {
      this.initialConfig = (TypedProperties) configuration.getDefaults();
    } else {
      this.initialConfig = new TypedProperties(configuration);
      configuration.setDefaults(initialConfig);
    }
    this.uuid = this.configuration.getString("jppf.node.uuid", JPPFUuid.normalUUID());
    if (debugEnabled && this.configuration.get(JPPFProperties.DEBUG_ENABLED)) log.debug("starting node with configuration:\n{}", configuration);
    this.offline = this.configuration.get(JPPFProperties.NODE_OFFLINE);
    this.android = this.configuration.get(JPPFProperties.NODE_ANDROID);
  }

  /**
   * Run a node as a standalone application.
   * @param args not used.
   */
  public static void main(final String...args) {
    try {
      final TypedProperties config = JPPFConfiguration.getProperties();
      final TypedProperties defaults = new TypedProperties(config);
      final TypedProperties overrides = new ConfigurationOverridesHandler().load(true);
      if (overrides != null) {
        if (debugEnabled) log.debug("starting with overrides = {}", overrides);
        config.putAll(overrides);
        config.setBoolean("jppf.node.overrides.set", true);
      }
      config.setDefaults(defaults);
      final NodeRunner runner = new NodeRunner(config);
      runner.start(args);
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
      System.exit(1);
    }
  }

  /**
   * Run a node as a standalone application.
   * @param args the first argument, if any must represent a valid TCP port to a socket
   * opened by the porcess that launched this node.
   */
  public void start(final String...args) {
    try {
      if (!android) new JmxMessageNotifier(); // initialize the jmx logger
      Thread.setDefaultUncaughtExceptionHandler(new JPPFDefaultUncaughtExceptionHandler());
      if (debugEnabled) log.debug("launching the JPPF node");
      VersionUtils.logVersionInformation("node", uuid);
      if (debugEnabled) log.debug("registering hooks");
      HookFactory.registerSPIMultipleHook(InitializationHook.class, null, null);
      HookFactory.registerConfigSingleHook(JPPFProperties.SERVER_CONNECTION_STRATEGY, DriverConnectionStrategy.class, new JPPFDefaultConnectionStrategy(configuration), null,
        new Class<?>[] { TypedProperties.class}, configuration);
      if ((args != null) && (args.length > 0) && !"noLauncher".equals(args[0])) {
        if (debugEnabled) log.debug("setting up connection with parent process");
        final int port = Integer.parseInt(args[0]);
        (launcherListener = new LauncherListener(port)).start();
      }
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
      System.exit(1);
    }
    try {
      if (debugEnabled) log.debug("node startup main loop");
      ConnectionContext context = new ConnectionContext("Initial connection", null, ConnectionReason.INITIAL_CONNECTION_REQUEST);
      while (true) {
        node = null;
        try {
          if (debugEnabled) log.debug("initializing configuration");
          if (initialConfig == null) initialConfig = new TypedProperties(configuration);
          else {
            if (!configuration.getBoolean("jppf.node.overrides.set", false)) restoreInitialConfig();
            else configuration.remove("jppf.node.overrides.set");
          }
          if (debugEnabled) log.debug("creating node");
          node = createNode(context);
          if (launcherListener != null) launcherListener.setActionHandler(new ShutdownRestartNodeProtocolHandler(node));
          if (debugEnabled) log.debug("running node");
          node.run();
        } catch(final JPPFNodeReconnectionNotification e) {
          if (debugEnabled) log.debug("received reconnection notification : {}", ExceptionUtils.getStackTrace(e));
          context = new ConnectionContext(e.getMessage(), e.getCause(), e.getReason());
          if (classLoader != null) classLoader.close();
          classLoader = null;
          if (node != null) node.stopNode();
          unsetSecurity();
        } finally {
          if ((node == null) || node.getShuttingDown().get()) break;
        }
      }
    } catch(final Exception e) {
      log.error("error preventing node from running", e);
      e.printStackTrace();
    }
    if (debugEnabled) log.debug("node exiting");
  }

  /**
   * Start the node.
   * @param connectionContext provides context information on the new connection request to the driver.
   * @return the node that was started, as a {@code JPPFNode} instance.
   * @throws Exception if the node failed to run or couldn't connect to the server.
   */
  private JPPFNode createNode(final ConnectionContext connectionContext) throws Exception {
    if (debugEnabled) log.debug("creating node with connectionContext = {}, configuration=\n{}", connectionContext, configuration);
    HookFactory.invokeHook(InitializationHook.class, "initializing", initialConfig);
    SystemUtils.printPidAndUuid("node", uuid);
    currentConnectionInfo = (DriverConnectionInfo) HookFactory.invokeHook(DriverConnectionStrategy.class, "nextConnectionInfo", currentConnectionInfo, connectionContext)[0];
    setSecurity();
    //String className = "org.jppf.server.node.remote.JPPFRemoteNode";
    final String className = configuration.get(JPPFProperties.NODE_CLASS);
    final AbstractJPPFClassLoader loader = getJPPFClassLoader();
    final Class<?> clazz = loader.loadClass(className);
    final Constructor<?> c = clazz.getConstructor(String.class, TypedProperties.class, DriverConnectionInfo.class);
    final JPPFNode node = (JPPFNode) c.newInstance(uuid, configuration, currentConnectionInfo);
    node.setJPPFClassLoader(loader);
    if (debugEnabled) log.debug("Created new node instance: {}, config =\n{}", node, node.getConfiguration());
    return node;
  }

  /**
   * Restore the configuration from the snapshot taken at startup time.
   */
  private void restoreInitialConfig() {
    final TypedProperties config = configuration;
    for (final Map.Entry<Object, Object> entry: initialConfig.entrySet()) {
      if ((entry.getKey() instanceof String) && (entry.getValue() instanceof String)) {
        config.setProperty((String) entry.getKey(), (String) entry.getValue());
      }
    }
  }

  /**
   * Set the security manager with the permissions granted in the policy file.
   * @throws Exception if the security could not be set.
   */
  private void setSecurity() throws Exception {
    if (!securityManagerSet) {
      final String s = JPPFConfiguration.get(JPPFProperties.POLICY_FILE);
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
      final PrivilegedAction<Object> action = () -> {
        System.setSecurityManager(null);
        return null;
      };
      AccessController.doPrivileged(action);
      securityManagerSet = false;
    }
  }

  /**
   * Get the main classloader for the node. This method performs a lazy initialization of the classloader.
   * @return a {@code AbstractJPPFClassLoader} used for loading the classes of the framework.
   * @exclude
   */
  public AbstractJPPFClassLoader getJPPFClassLoader() {
    synchronized(JPPFClassLoader.class) {
      if (classLoader == null) {
        final PrivilegedAction<JPPFClassLoader> action = () -> new JPPFClassLoader(offline ? null : new RemoteClassLoaderConnection(uuid, currentConnectionInfo), NodeRunner.class.getClassLoader());
        classLoader = AccessController.doPrivileged(action);
        if (debugEnabled) log.debug("created new class loader {}", classLoader);
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
   * Task used to terminate the JVM.
   * @exclude
   */
  public static class ShutdownOrRestart implements Runnable {
    /**
     * {@code true} if the node is to be restarted, {@code false} to only shut it down.
     */
    private boolean restart;
    /**
     * The node to shutdon and/or restart.
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
          if (debugEnabled) log.debug("stopping the node");
          node.stopNode();
          // close the JMX server connection to avoid request being sent again by the client.
          if (debugEnabled) log.debug("stopping the JMX server");
          try {
            ((JPPFNode) node).stopJmxServer();
            Thread.sleep(500L);
          } catch(@SuppressWarnings("unused") final Exception ignore) {
          }
          final int exitCode = restart ? 2 : 0;
          log.info("exiting the node with exit code {}", exitCode);
          System.exit(exitCode);
          return null;
        }
      });
    }
  }

  /**
   * This node's universal identifier.
   * @return a uuid as a string.
   */
  public String getUuid() {
    return node.getUuid();
  }

  /**
   * Get the offline node flag.
   * @return {@code true} if the node is offline, {@code false} otherwise.
   */
  public boolean isOffline() {
    return node.isOffline();
  }
}
