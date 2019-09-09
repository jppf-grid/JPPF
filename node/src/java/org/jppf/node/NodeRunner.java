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
package org.jppf.node;

import java.lang.reflect.Constructor;
import java.security.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.*;
import org.jppf.classloader.*;
import org.jppf.logging.jmx.JmxMessageNotifier;
import org.jppf.node.connection.*;
import org.jppf.node.initialization.InitializationHook;
import org.jppf.process.LauncherListener;
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
  private static final Logger log = LoggerFactory.getLogger(NodeRunner.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static final boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
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
   * 
   */
  final AtomicBoolean embeddedShutdown = new AtomicBoolean(false);
  /**
   * 
   */
  boolean startedFromMain;

  /**
   * Initialize this node runner with the specified configuration.
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
   * @exclude
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
      runner.startedFromMain = true;
      runner.start(args);
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
      System.exit(1);
    }
  }

  /**
   * Run a node embedded in the current JVM.
   * <br><div class="note_tip"><b>Important note:</b> <i>when starting a node programmatically, you should ensure that this method is called from a separate thread,
   * to avoid the current thread being blocked indefinitely. For example:</i>
   * <pre> TypedProperties nodeConfig = ...;
   * NodeRunner nodeRunner = new NodeRunner(nodeConfig);
   * new Thread(() -> nodeRunner.start());</pre></div>
   * @param args the first argument, if any, must represent a valid TCP port to a socket opened by the process that launched this node.
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
        if (debugEnabled) log.debug("setting up connection with parent process on port {}", args[0]);
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
          if (node != null) {
            node.stopNode();
            if (node.isSlaveNode()) System.exit(0);
          }
        } finally {
          if ((node != null) && (node.getShuttingDown().get() || embeddedShutdown.get())) {
            if (debugEnabled) log.debug("exiting: node={}, shuttingDown={}, embeddedShutdown", node, (node == null) ? "n/a" :node.getShuttingDown().get(), embeddedShutdown.get());
            break;
          }
        }
      }
    } catch(final Throwable e) {
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
    //String className = "org.jppf.server.node.remote.JPPFRemoteNode";
    final String className = configuration.get(JPPFProperties.NODE_CLASS);
    final AbstractJPPFClassLoader loader = getJPPFClassLoader();
    if (debugEnabled) log.debug("got node class loader {}", loader);
    final Class<?> clazz = loader.loadClass(className);
    final Constructor<?> c = clazz.getConstructor(String.class, TypedProperties.class, DriverConnectionInfo.class);
    if (debugEnabled) log.debug("instantiating {}", className);
    final JPPFNode node = (JPPFNode) c.newInstance(uuid, configuration, currentConnectionInfo);
    node.setJPPFClassLoader(loader);
    node.setStartedFromMain(startedFromMain);
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
   * Shutdown the node.
   */
  public void shutdown() {
    if (embeddedShutdown.compareAndSet(false, true)) {
      new ShutdownOrRestart(false, false, node).run();
    }
  }

  /**
   * Get the node's universal unique identifier.
   * @return a uuid as a string.
   */
  public String getUuid() {
    return (node== null) ? null : node.getUuid();
  }

  /**
   * Determine whether the node is <a href="https://www.jppf.org/doc/6.1/index.php?title=Offline_nodes">offline</a>.
   * @return {@code true} if the node is offline, {@code false} otherwise.
   */
  public boolean isOffline() {
    return (node== null) ? false: node.isOffline();
  }

  /**
   * Get the actual node started by this node runner, if any.
   * @return the current JPPF node, or {@code null} if no node is started.
   */
  public Node getNode() {
    return node;
  }
}
