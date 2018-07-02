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

package org.jppf.utils.configuration;

import java.io.File;
import java.security.KeyStore;
import java.util.List;

import org.jppf.jmx.JMXHelper;
import org.jppf.job.persistence.impl.DefaultFilePersistence;

/**
 * This class holds a static enumeration of the documented JPPF configuration properties.
 * @author Laurent Cohen
 * @since 5.2
. */
public class JPPFProperties {
  /** Server host name or IP address. */
  public static final JPPFProperty<String> SERVER_HOST = new StringProperty("jppf.server.host", "localhost");
  /** Server port. */
  public static final JPPFProperty<Integer> SERVER_PORT = new IntProperty("jppf.server.port", 11111);
  /** Server port number for secure connections. */
  public static final JPPFProperty<Integer> SERVER_SSL_PORT = new IntProperty("jppf.ssl.server.port", 11143);
  /** Same name as {@link #SERVER_PORT} but with default value of 11143. */
  public static final JPPFProperty<Integer> SERVER_SSL_PORT_NODE = new IntProperty("jppf.server.port", 11143);
  /** Whether to exit the JVM when shutting the driver down. */
  public static final JPPFProperty<Boolean> SERVER_EXIT_ON_SHUTDOWN = new BooleanProperty("jppf.server.exitOnShutdown", true);
  /** Whether to resolve IP addresses. */
  public static final JPPFProperty<Boolean> RESOLVE_ADDRESSES = new BooleanProperty("jppf.resolve.addresses", true, "org.jppf.resolve.addresses");
  /** Interval between updates of the JVM health data. */
  public static final JPPFProperty<Long> ADMIN_REFRESH_INTERVAL_HEALTH = new LongProperty("jppf.admin.refresh.interval.health", 3000L); 
  /** Interval between updates of the server statistics. */
  public static final JPPFProperty<Long> ADMIN_REFRESH_INTERVAL_STATS = new LongProperty("jppf.admin.refresh.interval.stats", 1000L);
  /** Interval between updates of the topology views. */
  public static final JPPFProperty<Long> ADMIN_REFRESH_INTERVAL_TOPOLOGY = new LongProperty("jppf.admin.refresh.interval.topology", 1000L);
  /** Wether to refresh the node's system info as well (to use for node filtering on the client side). */
  public static final JPPFProperty<Boolean> ADMIN_REFRESH_SYSTEM_INFO = new BooleanProperty("jppf.admin.refresh.system.info", false);
  /** Size of the class loader cache for the node. */
  public static final JPPFProperty<Integer> CLASSLOADER_CACHE_SIZE = new IntProperty("jppf.classloader.cache.size", 50);
  /** Enable/disable lookup of classpath resources in the file system. */
  public static final JPPFProperty<Boolean> CLASSLOADER_FILE_LOOKUP = new BooleanProperty("jppf.classloader.file.lookup", true);
  /** Class loader delegation mode: 'parent' or 'url'. */
  public static final JPPFProperty<String> CLASSLOADER_DELEGATION = new StringProperty("jppf.classloader.delegation", "parent");
  /** How often batched class loadin requests are sent to the server. */
  public static final JPPFProperty<Long> NODE_CLASSLOADING_BATCH_PERIOD = new LongProperty("jppf.node.classloading.batch.period", 100L);
  /** Prevent broadcast to the specified IPv4 addresses (exclusive filter, server only). */
  public static final JPPFProperty<String> DISCOVERY_BROADCAST_EXCLUDE_IPV4 = new StringProperty("jppf.discovery.broadcast.exclude.ipv4", null);
  /** Prevent broadcast to the specified IPv6 addresses (exclusive filter, server only). */
  public static final JPPFProperty<String> DISCOVERY_BROADCAST_EXCLUDE_IPV6 = new StringProperty("jppf.discovery.broadcast.exclude.ipv6", null);
  /** Broadcast to the specified IPv4 addresses (inclusive filter, server only). */
  public static final JPPFProperty<String> DISCOVERY_BROADCAST_INCLUDE_IPV4 = new StringProperty("jppf.discovery.broadcast.include.ipv4", null);
  /** Broadcast to the specified IPv6 addresses (inclusive filter, server only). */
  public static final JPPFProperty<String> DISCOVERY_BROADCAST_INCLUDE_IPV6 = new StringProperty("jppf.discovery.broadcast.include.ipv6", null);
  /** UDP broadcast interval in milliseconds. */
  public static final JPPFProperty<Long> DISCOVERY_BROADCAST_INTERVAL = new LongProperty("jppf.discovery.broadcast.interval", 5000L);
  /** Enable/disable server discovery. */
  public static final JPPFProperty<Boolean> DISCOVERY_ENABLED = new BooleanProperty("jppf.discovery.enabled", true);
  /** Whether to discover server connections from multiple network interfaces. */
  public static final JPPFProperty<Boolean> DISCOVERY_ACCEPT_MULTIPLE_INTERFACES = new BooleanProperty("jppf.discovery.acceptMultipleInterfaces", false);
  /** IPv4 exclusion patterns for server discovery. */
  public static final JPPFProperty<String> DISCOVERY_EXCLUDE_IPV4 = new StringProperty("jppf.discovery.exclude.ipv4", null);
  /** IPv6 exclusion patterns for server discovery. */
  public static final JPPFProperty<String> DISCOVERY_EXCLUDE_IPV6 = new StringProperty("jppf.discovery.exclude.ipv6", null);
  /** IPv4 inclusion patterns for server discovery. */
  public static final JPPFProperty<String> DISCOVERY_INCLUDE_IPV4 = new StringProperty("jppf.discovery.include.ipv4", null);
  /** IPv6 inclusion patterns for server discovery. */
  public static final JPPFProperty<String> DISCOVERY_INCLUDE_IPV6 = new StringProperty("jppf.discovery.include.ipv6", null);
  /** Server discovery: UDP multicast group. */
  public static final JPPFProperty<String> DISCOVERY_GROUP = new StringProperty("jppf.discovery.group", "230.0.0.1");
  /** Server discovery: UDP multicast port. */
  public static final JPPFProperty<Integer> DISCOVERY_PORT = new IntProperty("jppf.discovery.port", 11111);
  /** Server discovery timeout in milliseconds. */
  public static final JPPFProperty<Integer> DISCOVERY_TIMEOUT = new IntProperty("jppf.discovery.timeout", 1000);
  /** Priority assigned to discovered server connections (client/admiin console). */
  public static final JPPFProperty<Integer> DISCOVERY_PRIORITY = new IntProperty("jppf.discovery.priority", 0);
  /** Names of the manually configured servers in the client. */
  public static final JPPFProperty<String[]> DRIVERS = new StringArrayProperty("jppf.drivers", " ", new String[] {"default-driver"});
  /** Manually defined driver host for a client-to-driver connection (parametrized). */
  public static final JPPFProperty<String> PARAM_SERVER_HOST = new StringProperty("<driver_name>.jppf.server.host", "localhost");
  /** Manually defined driver port for a client-to-driver connection (parametrized). */
  public static final JPPFProperty<Integer> PARAM_SERVER_PORT = new IntProperty("<driver_name>.jppf.server.port", 11111, 0, 65535);
  /** Manually defined SSL enabled flag for a client-to-driver connection (parametrized). */
  public static final JPPFProperty<Boolean> PARAM_SERVER_SSL_ENABLED = new BooleanProperty("<driver_name>.jppf.ssl.enabled", false);
  /** Manually defined priority for a client-to-driver connection (parametrized). */
  public static final JPPFProperty<Integer> PARAM_PRIORITY = new IntProperty("<driver_name>.jppf.priority", 0);
  /** Manually defined connection ppol size for a client-to-driver connection (parametrized). */
  public static final JPPFProperty<Integer> PARAM_POOL_SIZE = new IntProperty("<driver_name>.jppf.pool.size", 1, 1, Integer.MAX_VALUE);
  /** Manually defined JMX connection ppol size for a client-to-driver connection (parametrized). */
  public static final JPPFProperty<Integer> PARAM_JMX_POOL_SIZE = new IntProperty("<driver_name>.jppf.jmx.pool.size", 1, 1, Integer.MAX_VALUE);
  /** UI refresh mode for the job data panel: 'immediate_notifications' | 'deferred_notifications' | 'polling'. */
  public static final JPPFProperty<String> GUI_PUBLISH_MODE = new StringProperty("jppf.gui.publish.mode", "immediate_notifications");
  /** Interval between updates of the job data view. */
  public static final JPPFProperty<Long> GUI_PUBLISH_PERIOD = new LongProperty("jppf.gui.publish.period", 1000L);
  /** Node idle mode: whether to shutdown the node at once when user activity resumes or wait until the node is no longer executing tasks. */
  public static final JPPFProperty<Boolean> IDLE_INTERRUPT_IF_RUNNING = new BooleanProperty("jppf.idle.interruptIfRunning", true);
  /** Enable/disable the idle mode. */
  public static final JPPFProperty<Boolean> IDLE_MODE_ENABLED = new BooleanProperty("jppf.idle.mode.enabled", false);
  /** Node idle mode: how often the node will check for keyboard and mouse inactivity. */
  public static final JPPFProperty<Long> IDLE_POLL_INTEFRVAL = new LongProperty("jppf.idle.poll.interval", 1000L);
  /** Node idle mode: the time of keyboard and mouse inactivity before considering the node idle. */
  public static final JPPFProperty<Long> IDLE_TIMEOUT = new LongProperty("jppf.idle.timeout", 300_000L);
  /** JMX connection pool size when discovery is enabled. */
  public static final JPPFProperty<Integer> JMX_POOL_SIZE = new IntProperty("jppf.jmx.pool.size", 1, 1, Integer.MAX_VALUE);
  /** JMX remote protocol. */
  public static final JPPFProperty<String> JMX_REMOTE_PROTOCOL = new StringProperty("jppf.jmxremote.protocol", JMXHelper.JPPF_JMX_PROTOCOL).setPossibleValues(JMXHelper.remoteProtocols());
  /** Timeout in milliseconds for JMX requests. */
  public static final JPPFProperty<Long> JMX_REMOTE_REQUEST_TIMEOUT = new LongProperty("jppf.jmxremote.request.timeout", Long.MAX_VALUE, 0L, Long.MAX_VALUE, "jppf.jmx.request.timeout");
  /** JVM options for the node or server process. */
  public static final JPPFProperty<String> JVM_OPTIONS = new StringProperty("jppf.jvm.options", null);
  /** Temporary buffer pool size for reading lengths as ints (size 4). */
  public static final JPPFProperty<Integer> LENGTH_BUFFER_POOL_SIZE = new IntProperty("jppf.length.buffer.pool.size", 100, 1, 2*1024);
  /** Load balancing algorithm name. */
  public static final JPPFProperty<String> LOAD_BALANCING_ALGORITHM = new StringProperty("jppf.load.balancing.algorithm", "proportional");
  /** Load balancing parameters profile name. */
  public static final JPPFProperty<String> LOAD_BALANCING_PROFILE = new StringProperty("jppf.load.balancing.profile", "jppf", "jppf.load.balancing.strategy");
  /** Class name of the implementation of the load-balancer persistence in the driver or client. */
  public static final JPPFProperty<String[]> LOAD_BALANCING_PERSISTENCE = new StringArrayProperty("jppf.load.balancing.persistence", " ", (String[]) null, "jppf.load.balancer.persistence");
  /** The hash function used to generate load-balancer state identifiers. */
  public static final JPPFProperty<String> LOAD_BALANCING_PERSISTENCE_HASH = new StringProperty("jppf.load.balancing.persistence.hash", "SHA-1", "jppf.load.balancer.persistence.hash");
  /** Enable/disable remote execution (client only). */
  public static final JPPFProperty<Boolean> REMOTE_EXECUTION_ENABLED = new BooleanProperty("jppf.remote.execution.enabled", true);
  /** Enable/disable local execution in the client. */
  public static final JPPFProperty<Boolean> LOCAL_EXECUTION_ENABLED = new BooleanProperty("jppf.local.execution.enabled", false);
  /** Maximum threads to use for local execution. */
  public static final JPPFProperty<Integer> LOCAL_EXECUTION_THREADS = new IntProperty("jppf.local.execution.threads", Runtime.getRuntime().availableProcessors());
  /** Priority assigned to the client local executor. */
  public static final JPPFProperty<Integer> LOCAL_EXECUTION_PRIORITY = new IntProperty("jppf.local.execution.priority", 0);
  /** Whether to enable a node to run in the same JVM as the driver. */
  public static final JPPFProperty<Boolean> LOCAL_NODE_ENABLED = new BooleanProperty("jppf.local.node.enabled", false);
  /** Number of processing threads in the node. */
  public static final JPPFProperty<Integer> PROCESSING_THREADS = new IntProperty("jppf.processing.threads", Runtime.getRuntime().availableProcessors(), "processing.threads");
  /** @exclude . */
  public static final JPPFProperty<Integer> PEER_PROCESSING_THREADS = new IntProperty("jppf.peer.processing.threads", Runtime.getRuntime().availableProcessors());
  /** JMX client connection timeout in millis. 0 or less means no timeout. */
  public static final JPPFProperty<Long> MANAGEMENT_CONNECTION_TIMEOUT = new LongProperty("jppf.management.connection.timeout", 60_000L);
  /** Enable/disable management of the node or server. */
  public static final JPPFProperty<Boolean> MANAGEMENT_ENABLED = new BooleanProperty("jppf.management.enabled", true);
  /** Management server host. */
  public static final JPPFProperty<String> MANAGEMENT_HOST = new StringProperty("jppf.management.host", null);
  /** Management remote connector port. */
  public static final JPPFProperty<Integer> MANAGEMENT_PORT = new IntProperty("jppf.management.port", 11198, 1024, 65535);
  /** Enable/disable JMX via secure connections. */
  public static final JPPFProperty<Boolean> MANAGEMENT_SSL_ENABLED = new BooleanProperty("jppf.management.ssl.enabled", false);
  /** Secure JMX server port. */
  public static final JPPFProperty<Integer> MANAGEMENT_SSL_PORT = new IntProperty("jppf.management.ssl.port", 93, 1024, 65535);
  /** Node management port (to distinguish from server management port when local node is on). */
  public static final JPPFProperty<Integer> MANAGEMENT_PORT_NODE = new IntProperty("jppf.node.management.port", 11198, 1024, 65535, "jppf.management.port");
  /** Node secure management port (to distinguish from server management port when local node is on). */
  public static final JPPFProperty<Integer> MANAGEMENT_SSL_PORT_NODE = new IntProperty("jppf.node.management.ssl.port", 93, 1024, 65535, "jppf.management.ssl.port");
  /** Fully qualifed class name of a MBeanServerForwarder implementation with optional space-separated string parameters. */
  public static final JPPFProperty<String[]> MANAGEMENT_SERVER_FORWARDER = new StringArrayProperty("jppf.management.server.forwarder", " ", null);
  /** Size of the pool of threads used to process node forwarding requests and notifications. */
  public static final JPPFProperty<Integer> NODE_FORWARDING_POOL_SIZE = new IntProperty("jppf.node.forwarding.pool.size", Runtime.getRuntime().availableProcessors());
  /** enable/disable network connection checks on write operations. */
  public static final JPPFProperty<Boolean> NIO_CHECK_CONNECTION = new BooleanProperty("jppf.nio.check.connection", true, "jppf.nio.connection.check");
  /** Whether the node runs in offline mode . */
  public static final JPPFProperty<Boolean> NODE_OFFLINE = new BooleanProperty("jppf.node.offline", false);
  /** Whether the node is an Android node. */
  public static final JPPFProperty<Boolean> NODE_ANDROID = new BooleanProperty("jppf.node.android", false);
  /** Whether the node is .Net-enabled. */
  public static final JPPFProperty<Boolean> DOTNET_BRIDGE_INITIALIZED = new BooleanProperty("jppf.dotnet.bridge.initialized", false);
  /** Whether the node is a master node. */
  public static final JPPFProperty<Boolean> PROVISIONING_MASTER = new BooleanProperty("jppf.node.provisioning.master", true);
  /** Whether the node is a slave node. */
  public static final JPPFProperty<Boolean> PROVISIONING_SLAVE = new BooleanProperty("jppf.node.provisioning.slave", false);
  /** UUID of the master node for a given slave node. */
  public static final JPPFProperty<String> PROVISIONING_MASTER_UUID = new StringProperty("jppf.node.provisioning.master.uuid", null);
  /** Directory where slave-specific configuration files are located. */
  public static final JPPFProperty<String> PROVISIONING_SLAVE_CONFIG_PATH = new StringProperty("jppf.node.provisioning.slave.config.path", "config");
  /** JVM options always added to the slave startup command. */
  public static final JPPFProperty<String> PROVISIONING_SLAVE_JVM_OPTIONS = new StringProperty("jppf.node.provisioning.slave.jvm.options", null);
  /** Path prefix for the root directory of slave nodes. */
  public static final JPPFProperty<String> PROVISIONING_SLAVE_PATH_PREFIX = new StringProperty("jppf.node.provisioning.slave.path.prefix", "slave_nodes/node_");
  /** Number of slaves to launch upon master node startup. */
  public static final JPPFProperty<Integer> PROVISIONING_STARTUP_SLAVES = new IntProperty("jppf.node.provisioning.startup.slaves", 0);
  /** Path to an optional config overrides file for slaves launched at startup. */
  public static final JPPFProperty<File> PROVISIONING_STARTUP_OVERRIDES_FILE = new FileProperty("jppf.node.provisioning.startup.overrides.file", null);
  /** An optional config overrides source (name of a class implementing {@link org.jppf.utils.JPPFConfiguration.ConfigurationSourceReader ConfigurationSourceReader}
   * or {@link org.jppf.utils.JPPFConfiguration.ConfigurationSource ConfigurationSource}) for slaves launched at startup. */
  public static final JPPFProperty<String> PROVISIONING_STARTUP_OVERRIDES_SOURCE = new StringProperty("jppf.node.provisioning.startup.overrides.source", null);
  /** Id of a slave node generated by/scoped by its master. */
  public static final JPPFProperty<Integer> PROVISIONING_SLAVE_ID = new IntProperty("jppf.node.provisioning.slave.id", -1);
  /** @exclude . */
  public static final JPPFProperty<Long> PROVISIONING_REQUEST_CHECK_TIMEOUT = new LongProperty("jppf.provisioning.request.check.timeout", 15_000L);
  /** Serialization scheme: name of a class implementing {@link org.jppf.serialization.JPPFSerialization JPPFSerialization}. */
  public static final JPPFProperty<String> OBJECT_SERIALIZATION_CLASS = new StringProperty("jppf.object.serialization.class", null);
  /** Whether to send jobs to orphan peer servers. */
  public static final JPPFProperty<Boolean> PEER_ALLOW_ORPHANS = new BooleanProperty("jppf.peer.allow.orphans", false);
  /** Enable/disable peer server discovery. */
  public static final JPPFProperty<Boolean> PEER_DISCOVERY_ENABLED = new BooleanProperty("jppf.peer.discovery.enabled", false);
  /** Size of discovered peer server connection pools. */
  public static final JPPFProperty<Integer> PEER_POOL_SIZE = new IntProperty("jppf.peer.pool.size", 1);
  /** Toggle secure connections to remote peer servers. */
  public static final JPPFProperty<Boolean> PEER_SSL_ENABLED = new BooleanProperty("jppf.peer.ssl.enabled", false);
  /** @exclude . */
  public static final JPPFProperty<Long> PEER_HANDLER_PERIOD = new LongProperty("jppf.peer.handler.period", 1000L);
  /** @exclude . */
  public static final JPPFProperty<Long> PEER_DISCOVERY_REMOVAL_CLEANUP_INTERVAL = new LongProperty("jppf.peer.discovery.removal.cleanup.interval", 30_000L);
  /** Space-separated list of peer server names. */
  public static final JPPFProperty<String> PEERS = new StringProperty("jppf.peers", null);
  /** Server host for a manually configured peer driver connection (parametrized). */
  public static final JPPFProperty<String> PARAM_PEER_SERVER_HOST = new StringProperty("jppf.peer.<peer_name>.server.host", "localhost");
  /** Server port for a manually configured peer driver connection (parametrized). */
  public static final JPPFProperty<Integer> PARAM_PEER_SERVER_PORT = new IntProperty("jppf.peer.<peer_name>.server.port", 11111, 0, 65535);
  /** SSL enabled flag for a manually configured peer driver connection (parametrized). */
  public static final JPPFProperty<Boolean> PARAM_PEER_SSL_ENABLED = new BooleanProperty("jppf.peer.<peer_name>.ssl.enabled", false);
  /** COnnection pool size for a manually configured peer driver connection (parametrized). */
  public static final JPPFProperty<Integer> PARAM_PEER_POOL_SIZE = new IntProperty("jppf.peer.<peer_name>.pool.size", 1, 1, Integer.MAX_VALUE);
  /** Path to the security policy file. */
  public static final JPPFProperty<String> POLICY_FILE = new StringProperty("jppf.policy.file", null);
  /** Connection pool size for discovered server conenctions. */
  public static final JPPFProperty<Integer> POOL_SIZE = new IntProperty("jppf.pool.size", 1, 1, Integer.MAX_VALUE);
  /** Delay in seconds before the first (re)connection attempt. */
  public static final JPPFProperty<Long> RECONNECT_INITIAL_DELAY = new LongProperty("jppf.reconnect.initial.delay", 0L, "reconnect.initial.delay");
  /** Frequency in seconds of reconnection attempts. */
  public static final JPPFProperty<Long> RECONNECT_INTERVAL = new LongProperty("jppf.reconnect.interval", 1L, "reconnect.interval");
  /** Time in seconds after which reconnection attempts stop. A negative value means never stop. */
  public static final JPPFProperty<Long> RECONNECT_MAX_TIME = new LongProperty("jppf.reconnect.max.time", 60L, "reconnect.max.time");
  /** Enable/disable recovery from hardware failures. */
  public static final JPPFProperty<Boolean> RECOVERY_ENABLED = new BooleanProperty("jppf.recovery.enabled", false);
  /** Maximum number of pings to the node before the connection is considered broken. */
  public static final JPPFProperty<Integer> RECOVERY_MAX_RETRIES = new IntProperty("jppf.recovery.max.retries", 3);
  /** Maximum ping response time from the node. */
  public static final JPPFProperty<Integer> RECOVERY_READ_TIMEOUT = new IntProperty("jppf.recovery.read.timeout", 15000);
  /** Number of threads allocated to the node connection reaper. */
  public static final JPPFProperty<Integer> RECOVERY_REAPER_POOL_SIZE = new IntProperty("jppf.recovery.reaper.pool.size", Runtime.getRuntime().availableProcessors());
  /** File to redirect {@link System#err} to. */
  public static final JPPFProperty<File> REDIRECT_ERR = new FileProperty("jppf.redirect.err", null);
  /** Append to existing file ({@code true}) or create new one ({@code false}). */
  public static final JPPFProperty<Boolean> REDIRECT_ERR_APPEND = new BooleanProperty("jppf.redirect.err.append", false);
  /** File to redirect {@link System#out} to. */
  public static final JPPFProperty<File> REDIRECT_OUT = new FileProperty("jppf.redirect.out", null);
  /** Append to existing file ({@code true}) or create new one ({@code false}). */
  public static final JPPFProperty<Boolean> REDIRECT_OUT_APPEND = new BooleanProperty("jppf.redirect.out.append", false);
  /** Root location of the file-persisted caches. */
  public static final JPPFProperty<String> RESOURCE_CACHE_DIR = new StringProperty("jppf.resource.cache.dir", System.getProperty("java.io.tmpdir"));
  /** Whether the class loader resource cache is enabled. */
  public static final JPPFProperty<Boolean> RESOURCE_CACHE_ENABLED = new BooleanProperty("jppf.resource.cache.enabled", true);
  /** Type of cache storage: either 'file' or 'memory'. */
  public static final JPPFProperty<String> RESOURCE_CACHE_STORAGE = new StringProperty("jppf.resource.cache.storage", "file").setPossibleValues("file", "memory");
  /** @exclude . */
  public static final JPPFProperty<String> RESOURCE_PROVIDER_CLASS = new StringProperty("jppf.resource.provider.class", "org.jppf.classloader.ResourceProviderImpl");
  /** Enable/disable the screen saver. */
  public static final JPPFProperty<Boolean> SCREENSAVER_ENABLED = new BooleanProperty("jppf.screensaver.enabled", false);
  /** Class name of an implementation of {@code org.jppf.node.screensaver.JPPFScreenSaver}. */
  public static final JPPFProperty<String> SCREENSAVER_CLASS = new StringProperty("jppf.screensaver.class", null);
  /** Class name of an implementation of  {@code org.jppf.node.screensaver.NodeIntegration}. */
  public static final JPPFProperty<String> SCREENSAVER_NODE_LISTENER = new StringProperty("jppf.screensaver.node.listener", null);
  /** Title of the screensaver's JFrame in windowed mode. */
  public static final JPPFProperty<String> SCREENSAVER_TITLE = new StringProperty("jppf.screensaver.title", "JPPF screensaver");
  /** Path to the image for the frame's icon (windowed mode). */
  public static final JPPFProperty<String> SCREENSAVER_ICON = new StringProperty("jppf.screensaver.icon", "org/jppf/node/jppf-icon.gif");
  /** Whether to display the screen saver in full screen mode. */
  public static final JPPFProperty<Boolean> SCREENSAVER_FULLSCREEN = new BooleanProperty("jppf.screensaver.fullscreen", false);
  /** Width in pixels (windowed mode). */
  public static final JPPFProperty<Integer> SCREENSAVER_WIDTH = new IntProperty("jppf.screensaver.width", 1000);
  /** Height in pixels (windowed mode). */
  public static final JPPFProperty<Integer> SCREENSAVER_HEIGHT = new IntProperty("jppf.screensaver.height", 800);
  /** Screensaver's on-screen X coordinate (windowed mode). */
  public static final JPPFProperty<Integer> SCREENSAVER_LOCATION_X = new IntProperty("jppf.screensaver.location.x", 0);
  /** Screensaver's on-screen Y coordinate (windowed mode). */
  public static final JPPFProperty<Integer> SCREENSAVER_LOCATION_Y = new IntProperty("jppf.screensaver.location.y", 0);
  /** Whether to close the screensaver on mouse motion (full screen mode). */
  public static final JPPFProperty<Boolean> SCREENSAVER_MOUSE_MOTION_CLOSE = new BooleanProperty("jppf.screensaver.mouse.motion.close", true);
  /** @exclude . */
  public static final JPPFProperty<Long> SCREENSAVER_MOUSE_MOTION_DELAY = new LongProperty("jppf.screensaver.mouse.motion.delay", 500L);
  /** Handle collisions between moving logos (built-in default screensaver). */
  public static final JPPFProperty<Boolean> SCREENSAVER_HANDLE_COLLISIONS = new BooleanProperty("jppf.screensaver.handle.collisions", true);
  /** Number of moving moving logos (built-in default screensaver). */
  public static final JPPFProperty<Integer> SCREENSAVER_LOGOS = new IntProperty("jppf.screensaver.logos", 10);
  /** Speed of moving moving logos! from 1 to 100 (built-in default screensaver). */
  public static final JPPFProperty<Integer> SCREENSAVER_SPEED = new IntProperty("jppf.screensaver.speed", 100, 1, 100);
  /** Path(s) to the moving logo image(s) (built-in default screensaver). */
  public static final JPPFProperty<String> SCREENSAVER_LOGO_PATH = new StringProperty("jppf.screensaver.logo.path", "org/jppf/node/jppf_group_small.gif");
  /** Path to the larger image at the center of the screen (built-in default screensaver). */
  public static final JPPFProperty<String> SCREENSAVER_CENTERIMAGE = new StringProperty("jppf.screensaver.centerimage", "org/jppf/node/jppf@home.gif");
  /** Hhorizontal alignment of the status panel (built-in default screensaver). */
  public static final JPPFProperty<String> SCREENSAVER_STATUS_PANEL_ALIGNMENT = new StringProperty("jppf.screensaver.status.panel.alignment", "center");
  /** Eeceive/send buffer size for socket connections. */
  public static final JPPFProperty<Integer> SOCKET_BUFFER_SIZE = new IntProperty("jppf.socket.buffer.size", 32 * 1024, 1024, 1024 * 1024);
  /** Enable/disable socket keepalive. */
  public static final JPPFProperty<Boolean> SOCKET_KEEPALIVE = new BooleanProperty("jppf.socket.keepalive", false);
  /** Seconds a socket connection can remain idle before being closed (client only). */
  public static final JPPFProperty<Long> SOCKET_MAX_IDLE = new LongProperty("jppf.socket.max-idle", -1L);
  /** Enable/disable Nagle's algorithm. */
  public static final JPPFProperty<Boolean> SOCKET_TCP_NODELAY = new BooleanProperty("jppf.socket.tcp_nodelay", true);
  /** Enabled/diable secure connections. */
  public static final JPPFProperty<Boolean> SSL_ENABLED = new BooleanProperty("jppf.ssl.enabled", false);
  /** Space-separated enabled cipher suites. */
  public static final JPPFProperty<String[]> SSL_CIPHER_SUITES = new StringArrayProperty("jppf.ssl.cipher.suites", " ", null);
  /** SSL client authentication level: 'none' | 'want' | 'need'. */
  public static final JPPFProperty<String> SSL_CLIENT_AUTH = new StringProperty("jppf.ssl.client.auth", "none");
  /** Whether to use a separate trust store for client certificates (server only). */
  public static final JPPFProperty<Boolean> SSL_CLIENT_DISTINCT_TRUSTSTORE = new BooleanProperty("jppf.ssl.client.distinct.truststore", false);
  /** Path to the client trust store in the file system or classpath. */
  public static final JPPFProperty<String> SSL_CLIENT_TRUSTSTORE_FILE = new StringProperty("jppf.ssl.client.truststore.file", null);
  /** Plain text client trust store password. */
  public static final JPPFProperty<String> SSL_CLIENT_TRUSTSTORE_PASSWORD = new StringProperty("jppf.ssl.client.truststore.password", null);
  /** Client trust store password as an arbitrary source. */
  public static final JPPFProperty<String> SSL_CLIENT_TRUSTSTORE_PASSWORD_SOURCE = new StringProperty("jppf.ssl.client.truststore.password.source", null);
  /** Client trust store location as an arbitrary source. */
  public static final JPPFProperty<String> SSL_CLIENT_TRUSTSTORE_SOURCE = new StringProperty("jppf.ssl.client.truststore.source", null);
  /** Client trust store format, e.g. 'JKS'. */
  public static final JPPFProperty<String> SSL_CLIENT_TRUSTSTORE_TYPE = new StringProperty("jppf.ssl.client.truststore.type", KeyStore.getDefaultType());
  /** Path to the SSL configuration in the file system or classpath. */
  public static final JPPFProperty<String> SSL_CONFIGURATION_FILE = new StringProperty("jppf.ssl.configuration.file", null);
  /** SSL configuration as an arbitrary source. */
  public static final JPPFProperty<String> SSL_CONFIGURATION_SOURCE = new StringProperty("jppf.ssl.configuration.source", null);
  /** {@link javax.net.ssl.SSLContext SSLContext} protocol. */
  public static final JPPFProperty<String> SSL_CONTEXT_PROTOCOL = new StringProperty("jppf.ssl.context.protocol", "TLSv1.2");
  /** Path to the key store in the file system or classpath. */
  public static final JPPFProperty<String> SSL_KEYSTORE_FILE = new StringProperty("jppf.ssl.keystore.file", null);
  /** Plain text key store password. */
  public static final JPPFProperty<String> SSL_KEYSTORE_PASSWORD = new StringProperty("jppf.ssl.keystore.password", null);
  /** Key store password as an arbitrary source. */
  public static final JPPFProperty<String> SSL_KEYSTORE_PASSWORD_SOURCE = new StringProperty("jppf.ssl.keyststore.password.source", null);
  /** Key store format, e.g. 'JKS'. */
  public static final JPPFProperty<String> SSL_KEYSTORE_TYPE = new StringProperty("jppf.ssl.keyststore.type", KeyStore.getDefaultType());
  /** Key store location as an arbitrary source. */
  public static final JPPFProperty<String> SSL_KEYSTORE_SOURCE = new StringProperty("jppf.ssl.keytstore.source", null);
  /** A list of space-separated enabled protocols. */
  public static final JPPFProperty<String[]> SSL_PROTOCOLS = new StringArrayProperty("jppf.ssl.protocols", " ", null);
  /** Path to the trust store in the file system or classpath. */
  public static final JPPFProperty<String> SSL_TRUSTSTORE_FILE = new StringProperty("jppf.ssl.truststore.file", null);
  /** Plain text trust store password. */
  public static final JPPFProperty<String> SSL_TRUSTSTORE_PASSWORD = new StringProperty("jppf.ssl.truststore.password", null);
  /** Trust store password as an arbitrary source. */
  public static final JPPFProperty<String> SSL_TRUSTSTORE_PASSWORD_SOURCE = new StringProperty("jppf.ssl.truststore.password.source", null);
  /** Trust store location as an arbitrary source. */
  public static final JPPFProperty<String> SSL_TRUSTSTORE_SOURCE = new StringProperty("jppf.ssl.truststore.source", null);
  /** Trust store format, e.g. 'JKS'. */
  public static final JPPFProperty<String> SSL_TRUSTSTORE_TYPE = new StringProperty("jppf.ssl.truststore.type", KeyStore.getDefaultType());
  /** @exclude . */
  public static final JPPFProperty<Integer> SSL_THREAD_POOL_SIZE = new IntProperty("jppf.ssl.thread.pool.size", 10, "jppf.ssl.thread.pool");
  /** Maximum size of temporary buffers pool. */
  public static final JPPFProperty<Integer> TEMP_BUFFER_POOL_SIZE = new IntProperty("jppf.temp.buffer.pool.size", 10, 1, 2*1024);
  /** Size of temporary buffers used in I/O transfers. */
  public static final JPPFProperty<Integer> TEMP_BUFFER_SIZE = new IntProperty("jppf.temp.buffer.size", 32*1024, 1024, 64*1024);
  /** Type of NIO thread pool: either dynamic or fixed. */
  public static final JPPFProperty<String> NIO_THREAD_POOL_TYPE = new StringProperty("jppf.nio.thread.pool.type", "sync").setPossibleValues("dynamic", "fixed", "sync");
  /** Core number of threads performing network I/O . */
  public static final JPPFProperty<Integer> NIO_THREAD_POOL_SIZE = new IntProperty("jppf.nio.thread.pool.size", Runtime.getRuntime().availableProcessors(), 2, 32*1024, "jppf.transition.thread.pool.size");
  /** Queue size for the pool of threads performing network I/O . */
  public static final JPPFProperty<Integer> NIO_THREAD_QUEUE_SIZE = new IntProperty("jppf.nio.thread.queue.size", 50, 1, Integer.MAX_VALUE);
  /** TTL in milliseconds for threads beyond the core number performing network I/O. */
  public static final JPPFProperty<Long> NIO_THREAD_TTL = new LongProperty("jppf.nio.thread.ttl", 15_000L, 1L, Long.MAX_VALUE);
  /** Whether to display the animated splash screen at console startup, defaults to false. */
  public static final JPPFProperty<Boolean> UI_SPLASH = new BooleanProperty("jppf.ui.splash", true);
  /** Interval between images in milliseconds. */
  public static final JPPFProperty<Long> UI_SPLASH_DELAY = new LongProperty("jppf.ui.splash.delay", 500L);
  /** One or more paths to the images displayed in a rolling sequence (like a slide show), separated by '|' (pipe) characters. */
  public static final JPPFProperty<String> UI_SPLASH_IMAGES = new StringProperty("jppf.ui.splash.images", null);
  /** The fixed text displayed at center of the splash screen. */
  public static final JPPFProperty<String> UI_SPLASH_MESSAGE = new StringProperty("jppf.ui.splash.message", "");
  /** The color of the splash screen message. */
  public static final JPPFProperty<String> UI_SPLASH_MESSAGE_COLOR = new StringProperty("jppf.ui.splash.message.color", "64, 64, 128");
  /** @exclude . */
  public static final JPPFProperty<Long> NIO_SELECT_TIMEOUT = new LongProperty("jppf.nio.select.timeout", 1000L);
  /** Ratio of available heap over the size of an object to deserialize, below which disk overflow is triggered. */
  public static final JPPFProperty<Double> DISK_OVERFLOW_THRESHOLD = new DoubleProperty("jppf.disk.overflow.threshold", 2d);
  /** Whether to call System.gc() and recompute the avalaible heap size before triggering disk overflow. */
  public static final JPPFProperty<Boolean> GC_ON_DISK_OVERFLOW = new BooleanProperty("jppf.gc.on.disk.overflow", true);
  /** Minimum heap size in MB below which disk overflow is systematically triggered, to avoid heap fragmentation and ensure there's enough memory to deserialize job headers. */
  public static final JPPFProperty<Long> LOW_MEMORY_THRESHOLD = new LongProperty("jppf.low.memory.threshold", 32L);
  /** Whether to check for low memory and trigger disk offloading. */
  public static final JPPFProperty<Boolean> CHECK_LOW_MEMORY = new BooleanProperty("jppf.check.low.memory", true);
  /** Used heap in bytes above which notifications from task are offloaded to file. Defaults to 0.8 * maxHeapSize.. */
  public static final JPPFProperty<String> NOTIFICATION_OFFLOAD_MEMORY_THRESHOLD = new StringProperty("jppf.notification.offload.memory.threshold", "" + (long) (0.8d * Runtime.getRuntime().maxMemory()) + "b" );
  /** Determines the frequency at which the JVM's cpu load is recomputed, in ms. */
  public static final JPPFProperty<Long> CPU_LOAD_COMPUTATION_INTERVAL = new LongProperty("jppf.cpu.load.computation.interval", 1000L);
  /** Type of thread pool to use in the node: either 'default' or 'org.jppf.server.node.fj.ThreadManagerForkJoin'. */
  public static final JPPFProperty<String> THREAD_MANAGER_CLASS = new StringProperty("jppf.thread.manager.class", "default");
  /** Internal use. The class of node to instantiate upon node startup. For instance Java and Android nodes use a different class. */
  public static final JPPFProperty<String> NODE_CLASS = new StringProperty("jppf.node.class", "org.jppf.server.node.remote.JPPFRemoteNode");
  /** Default script language for scripted property values. */
  public static final JPPFProperty<String> SCRIPT_DEFAULT_LANGUAGE = new StringProperty("jppf.script.default.language", "javascript");
  /** Fully qualified name of a class implementing {@link org.jppf.node.connection.DriverConnectionStrategy DriverConnectionStrategy}. */
  public static final JPPFProperty<String> SERVER_CONNECTION_STRATEGY = new StringProperty("jppf.server.connection.strategy", null);
  /** @exclude . */
  public static final JPPFProperty<String> SERIALIZATION_EXCEPTION_HOOK = new StringProperty("jppf.serialization.exception.hook", null);
  /** Full path to the Java executable. */
  public static final JPPFProperty<String> JAVA_PATH = new StringProperty("jppf.java.path", null);
  /** Path to the temporary config overrides properties file. */
  public static final JPPFProperty<File> CONFIG_OVERRIDES_PATH = new FileProperty("jppf.config.overrides.path", new File("config/config-overrides.properties"));
  /** @exclude . */
  public static final JPPFProperty<Boolean> NODE_CHECK_CONNECTION = new BooleanProperty("jppf.node.check.connection", false);
  /** The default thickness of the scrollbars in the GUI. */
  public static final JPPFProperty<Integer> DEFAULT_SCROLLBAR_THICKNESS = new IntProperty("jppf.ui.default.scrollbar.thickness", 10);
  /** Whether a node is idle. This property is only set within a server. */
  public static final JPPFProperty<Boolean> NODE_IDLE = new BooleanProperty("jppf.node.idle", true);
  /** UUID of the job for which a node is reserved. */
  public static final JPPFProperty<String> NODE_RESERVED_JOB = new StringProperty("jppf.node.reserved.job", null);
  /** UUID of the node beofre restart, when it has a job reserved. */
  public static final JPPFProperty<String> NODE_RESERVED_UUID = new StringProperty("jppf.node.reserved.uuid", null);
  /** Whether debug mbean is enabled, defaults to false. */
  public static final JPPFProperty<Boolean> DEBUG_ENABLED = new BooleanProperty("jppf.debug.enabled", false);
  /** Interval in seconds between 2 refreshes of a page in the web admin console. */
  public static final JPPFProperty<Integer> WEB_ADMIN_REFRESH_INTERVAL = new IntProperty("jppf.web.admin.refresh.interval", 3);
  /** Class name of the implementation of the job persistence in the driver. */
  public static final JPPFProperty<String[]> JOB_PERSISTENCE = new StringArrayProperty("jppf.job.persistence", " ", new String[] {DefaultFilePersistence.class.getName()});
  /** Whether object graphs should be serialized or deserialized sequentially instead of in parallel. */
  public static final JPPFProperty<Boolean> SEQUENTIAL_SERIALiZATION = new BooleanProperty("jppf.sequential.serialization", false, "jppf.sequential.deserialization");
  /** Whether bias towards local node for scheduling is enabled in the driver. */
  public static final JPPFProperty<Boolean> LOCAL_NODE_BIAS = new BooleanProperty("jppf.local.node.bias", true);
  /** Location of the file that contains the DDL statements to create the database table for the databse job persistence. */
  public static final JPPFProperty<String> JOB_PERSISTENCE_DDL_LOCATION = new StringProperty("jppf.job.persistence.ddl.location", "org/jppf/job/persistence/impl/job_persistence.sql");
  /** Percentage of used heap above which async job persistence switches to sequential mode, until the used heap decreases below the threshold. */
  public static final JPPFProperty<Double> JOB_PERSISTENCE_MEMORY_THRESHOLD = new DoubleProperty("jppf.job.persistence.memory.threshold", 70d);
  /** Location of the file that contains the DDL statements to create the database table for the laod-balancers persistence. */
  public static final JPPFProperty<String> LOAD_BALANCING_PERSISTENCE_DDL_LOCATION = new StringProperty("jppf.load.balancing.persistence.ddl.location", "org/jppf/load/balancer/persistence/load_balancer_persistence.sql");
  /** Enable / disable a pluggable view. */
  public static final JPPFProperty<Boolean> ADMIN_CONSOLE_VIEW_ENABLED = new BooleanProperty("jppf.admin.console.view.<view_name>.enabled", true);
  /** Name of a pluggable view class, extending org.jppf.ui.plugin.PluggableView. */
  public static final JPPFProperty<String> ADMIN_CONSOLE_VIEW_CLASS = new StringProperty("jppf.admin.console.view.<view_name>.class", null);
  /** The title for the view, seen as the tab label. */
  public static final JPPFProperty<String> ADMIN_CONSOLE_VIEW_TITLE = new StringProperty("jppf.admin.console.view.<view_name>.title", null);
  /** Path to the icon for a pluggable view, seen as the tab icon. */
  public static final JPPFProperty<String> ADMIN_CONSOLE_VIEW_ICON = new StringProperty("jppf.admin.console.view.<view_name>.icon", null);
  /** The built-in view a pluggable view is attached to. It must be one of the tabbed panes of the admin console. Possible values: Main | Topology | Charts. */
  public static final JPPFProperty<String> ADMIN_CONSOLE_VIEW_ADD_TO = new StringProperty("jppf.admin.console.view.<view_name>.addto", "Main").setPossibleValues("Main", "Topology", "Charts");
  /** The position at which a pluggable view is inserted withing the enclosing tabbed pane. A negative value means insert at the end. */
  public static final JPPFProperty<Integer> ADMIN_CONSOLE_VIEW_POSITION = new IntProperty("jppf.admin.console.view.<view_name>.position", -1);
  /** Whether to automatically select the pluggable view. */
  public static final JPPFProperty<Boolean> ADMIN_CONSOLE_VIEW_AUTOSELECT = new BooleanProperty("jppf.admin.console.view.<view_name>.autoselect", false);
  /** The list of all predefined properties. */
  private static List<JPPFProperty<?>> properties;

  /**
   * Get the list of all predefined configuration properties.
   * @return A list of {@link JPPFProperty} instances.
  */
  public synchronized static List<JPPFProperty<?>> allProperties() {
    if (properties == null) properties = ConfigurationUtils.allProperties(JPPFProperties.class);
    return properties;
  }
}
