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

package org.jppf.node.provisioning;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.Notification;

import org.apache.commons.io.FileUtils;
import org.jppf.node.Node;
import org.jppf.process.*;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * This class manages the slave nodes.
 * @author Laurent Cohen
 * @since 4.1
 * @exclude
 */
public final class SlaveNodeManager implements ProcessLauncherListener {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(SlaveNodeManager.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Name of the property which defines the id of a slave node.
   */
  private static final String SLAVE_ID_PROPERTY = "jppf.node.provisioning.slave.id";
  /**
   * Path prefix used for the root directory of each slave node.
   * The provisioning facility will then add a sequence number as suffix, to distinguish between slave nodes.
   */
  private final String slavePathPrefix;
  /**
   * Directory where configuration files, other than the jppf configuration, are located.
   * The files in this folder will be copied into each slave node's 'config' directory.
   */
  private final String slaveConfigPath;
  /**
   * The directory where the slave's config files are located, relative to its root folder.
   */
  static final String SLAVE_LOCAL_CONFIG_DIR = "config";
  /**
   * The name of the slave's JPPF config file.
   */
  static final String SLAVE_LOCAL_CONFIG_FILE = "jppf-node.properties";
  /**
   * Max timeout in millis for checking the fulfillment of a provisioning request.
   */
  private final long requestCheckTimeout;
  /**
   * A mapping of the slave processes to their internal name.
   */
  private final TreeMap<Integer, SlaveNodeLauncher> slaves = new TreeMap<>();
  /**
   * The set of already reserved slave ids.
   * @since 4.2.2
   */
  private Set<Integer> reservedIds = new HashSet<>();
  /**
   * Master node root directory.
   */
  private final File masterDir;
  /**
   * List of jars to include in each slave's classpath.
   */
  private final List<String> slaveClasspath = new ArrayList<>();
  /**
   * A set of properties overriding those in the master node's configuration.
   */
  private TypedProperties configOverrides = new TypedProperties();
  /**
   * Use to sequentialize the provisioning requests.
   */
  private ExecutorService executor = Executors.newSingleThreadExecutor(new JPPFThreadFactory("SlaveNodeManager"));
  /**
   * The JPPF node that holds this manager.
   */
  private final Node node;
  /**
   * The provision MBean implementation.
   */
  JPPFNodeProvisioning mbean;
  /**
   * Sequence number provider for the the provisioning notifications.
   */
  private final AtomicLong notificationSequence = new AtomicLong(0L);
  /**
   * 
   */
  private final long startDelayIncrement;

  /**
   * Initialize this manager.
   * @param node the JPPF node that holds this manager.
   */
  public SlaveNodeManager(final Node node) {
    this.node = node;
    final TypedProperties config = node.getConfiguration();
    slavePathPrefix = config.get(JPPFProperties.PROVISIONING_SLAVE_PATH_PREFIX);
    slaveConfigPath = config.get(JPPFProperties.PROVISIONING_SLAVE_CONFIG_PATH);
    requestCheckTimeout = config.get(JPPFProperties.PROVISIONING_REQUEST_CHECK_TIMEOUT);
    masterDir = new File(System.getProperty("user.dir"));
    final long n = node.getConfiguration().getLong("jppf.node.provisioning.slave.start.delay.increment", 0L);
    startDelayIncrement = (n >= 0L) ? n : 0L;
    if (debugEnabled) log.debug("masterDir = {}, request check timeout = {} ms, slave start delay increment = {}", masterDir, requestCheckTimeout, startDelayIncrement);
    computeSlaveClasspath();
  }

  /**
   * Start or stop the required number of slaves to reach the specified number,
   * using the specified config overrides. If {@code configOverrides} is null, then previous overrides are applied.
   * @param requestedSlaves the number of slaves to reach.
   * @param interruptIfRunning if true then nodes can only be stopped once they are idle. 
   * @param configOverrides a set of overrides to the slave's configuration.
   */
  void submitProvisioningRequest(final int requestedSlaves, final boolean interruptIfRunning, final TypedProperties configOverrides) {
    if (requestedSlaves < 0) return;
    executor.execute(() -> shrinkOrGrowSlaves(requestedSlaves, interruptIfRunning, configOverrides));
  }

  /**
   * Stop all the slave nodes still running.
   */
  public void stopAllSlaves() {
    shrinkOrGrowSlaves(0, true, null);
  }

  /**
   * Start or stop the required number of slaves to reach the specified number,
   * using the specified config overrides. If {@code configOverrides} is null, then previous overrides are applied.
   * @param requestedSlaves the number of slaves to reach.
   * @param interruptIfRunning if true then nodes can only be stopped once they are idle. 
   * @param configOverrides a set of overrides to the slave's configuration.
   */
  private void shrinkOrGrowSlaves(final int requestedSlaves, final boolean interruptIfRunning, final TypedProperties configOverrides) {
    if (debugEnabled) log.debug("provisioning request for {} slaves, interruptIfRunning={}, configOverrides={}", requestedSlaves, interruptIfRunning, configOverrides);
    final int action = interruptIfRunning ? ProcessCommands.SHUTDOWN_INTERRUPT : ProcessCommands.SHUTDOWN_NO_INTERRUPT;
    // if new config overides, stop all the slaves and restart new ones
    if (configOverrides != null) {
      if (debugEnabled) log.debug("stopping all processes");
      this.configOverrides = configOverrides;
      synchronized(slaves) {
        for (final SlaveNodeLauncher slave: slaves.values()) {
          synchronized(slave) {
            if (slave.isStarted()) slave.sendActionCommand(action);
            else {
              slave.setStopped(true);
              removeSlave(slave);
            }
          }
        }
      }
    }
    final int size = nbSlaves();
    final int diff = size - requestedSlaves;
    // if running slaves > requested ones, stop those not needed
    int id = -1;
    if (diff > 0) {
      log.debug("stopping " + diff + " processes");
      for (int i=requestedSlaves; i<size; i++) {
        SlaveNodeLauncher slave = null;
        synchronized(slaves) {
          id = (id < 0) ? slaves.lastKey() : slaves.lowerKey(id);
          slave = slaves.get(id);
        }
        log.debug("stopping {}", slave.getName());
        synchronized(slave) {
          if (slave.isStarted()) slave.sendActionCommand(action);
          else {
            slave.setStopped(true);
            removeSlave(slave);
          }
        }
      }
    } else {
      // otherwise start the missing number of slaves
      if (debugEnabled) log.debug("starting " + -diff + " processes");
      for (int i=size, count=0; i<requestedSlaves; i++, count++) {
        id = reserveNextAvailableId();
        final String slaveDirPath = slavePathPrefix + id;
        try {
          log.debug("starting slave at {}", slaveDirPath);
          setupSlaveNodeFiles(slaveDirPath, this.configOverrides, id);
          final SlaveNodeLauncher slave = new SlaveNodeLauncher(id, slaveDirPath, slaveClasspath, startDelayIncrement * count);
          slave.addProcessLauncherListener(this);
          ThreadUtils.startDaemonThread(slave, slaveDirPath);
        } catch(final Exception|Error e) {
          log.error("error trying to start '{}' : {}", slaveDirPath, ExceptionUtils.getStackTrace(e));
          if (e instanceof Error) throw (Error) e;
        }
      }
    }
    if (requestCheckTimeout > 0) {
      final long start = System.nanoTime();
      final boolean check = ConcurrentUtils.awaitCondition(() -> nbSlaves() == requestedSlaves, requestCheckTimeout, 50L, false);
      final long elapsed = (System.nanoTime() - start) / 1_000_000L;
      if (debugEnabled) log.debug(String.format("fullfilment check for provisioning request for %d slaves %s after %,d ms", requestedSlaves, (check ? "succeeded" : "timed out"), elapsed));
    }
  }

  /**
   * Get the number of running slave nodes.
   * @return the number of slaves as an int.
   */
  public int nbSlaves() {
    synchronized(slaves) {
      return slaves.size();
    }
  }

  /**
   * Copy the master's configuration files from the master's install folder into the slave's root folder.
   * Configuration overrides are applied here.
   * @param slaveDirPath name of the slave node.
   * @param configOverrides the overrides to apply to the slave's configuration.
   * @param id the id assigned to the slave node.
   * @throws Exception if any error occurs.
   */
  private void setupSlaveNodeFiles(final String slaveDirPath, final TypedProperties configOverrides, final int id) throws Exception {
    final File slaveDir = new File(slaveDirPath);
    if (!slaveDir.exists()) slaveDir.mkdirs();
    final File slaveConfigSrc = new File(slaveConfigPath);
    final File slaveConfigDest = new File(slaveDir, SLAVE_LOCAL_CONFIG_DIR);
    if (!slaveConfigDest.exists()) slaveConfigDest.mkdirs();
    if (slaveConfigSrc.exists()) {
      FileUtils.copyDirectory(slaveConfigSrc, slaveConfigDest);
      if (debugEnabled) log.debug("copied files from {} to {}", slaveConfigSrc, slaveConfigDest);
    } else {
      if (debugEnabled) log.debug("config source dir '{}' does not exist", slaveConfigSrc);
    }
    // get the JPPF config, apply the overrides, then save it to the slave's folder
    final TypedProperties props = new TypedProperties(node.getConfiguration());
    props.remove("jppf.node.uuid");
    for (String key: configOverrides.stringPropertyNames()) props.setProperty(key, configOverrides.getProperty(key));
    props.set(JPPFProperties.PROVISIONING_MASTER, false);
    props.set(JPPFProperties.PROVISIONING_SLAVE, true);
    props.setInt(SLAVE_ID_PROPERTY, id);
    props.set(JPPFProperties.PROVISIONING_MASTER_UUID, node.getUuid());
    final int range = 65535 - 1024;
    final int mgtPort = 1024 + ((props.get(JPPFProperties.MANAGEMENT_PORT_NODE) + id - 1024) % range);
    props.set(JPPFProperties.MANAGEMENT_PORT_NODE, mgtPort);
    try (Writer writer = new BufferedWriter(new FileWriter(new File(slaveConfigDest, SLAVE_LOCAL_CONFIG_FILE)))) {
      props.store(writer, "generated jppf configuration");
    }
  }

  @Override
  public void processStarted(final ProcessLauncherEvent event) {
    final SlaveNodeLauncher slave = (SlaveNodeLauncher) event.getProcessLauncher();
    synchronized(slaves) {
      slaves.put(slave.getId(), slave);
    }
    if (nbSlaves() <= 0) log.warn("received processStarted() for slave id = {}, but nbSlaves is zero", slave.getId());
    else if (debugEnabled) log.debug("received processStarted() for slave id = {}", slave.getId());
    sendNotification(JPPFNodeProvisioningMBean.SLAVE_STARTED_NOTIFICATION_TYPE, slave);
  }

  @Override
  public void processStopped(final ProcessLauncherEvent event) {
    final SlaveNodeLauncher slave = (SlaveNodeLauncher) event.getProcessLauncher();
    if (debugEnabled) log.debug("received processStopped() for slave id = {}, exitCode = {}", slave.getId(), slave.exitCode);
    if (slave.exitCode != 2) removeSlave(slave);
    else ThreadUtils.startDaemonThread(slave, slave.getName());
    sendNotification(JPPFNodeProvisioningMBean.SLAVE_STOPPED_NOTIFICATION_TYPE, slave);
  }

  /**
   * Send a notification that a slave node has started or stopped.
   * @param type specifies whther the slave has started or stopped.
   * @param launcher the slave node laucnher.
   */
  private void sendNotification(final String type, final SlaveNodeLauncher launcher) {
    final Notification notification = new Notification(type, JPPFNodeProvisioningMBean.MBEAN_NAME, notificationSequence.incrementAndGet());
    notification.setUserData(
      new JPPFProvisioningInfo(node.getUuid(), launcher.getId(), (type == JPPFNodeProvisioningMBean.SLAVE_STARTED_NOTIFICATION_TYPE) ? -1 : launcher.exitCode, launcher.getLaunchCommand()));
    mbean.sendNotification(notification);
  }

  /**
   * Compute the classpath for the slave nodes.
   */
  private void computeSlaveClasspath() {
    final String separator = System.getProperty("path.separator");
    final String cp = System.getProperty("java.class.path");
    final String[] paths = cp.split(separator);
    for (final String path: paths) {
      if (path != null) slaveClasspath.add(new File(path).getAbsolutePath());
    }
    slaveClasspath.add(".");
    slaveClasspath.add(SLAVE_LOCAL_CONFIG_DIR);
  }

  /**
   * Automatically start slaves if specified in the configuration.
   */
  public void handleStartup() {
    final TypedProperties config =  node.getConfiguration();
    final int n = config.get(JPPFProperties.PROVISIONING_STARTUP_SLAVES);
    if (n > 0) {
      String msg = "starting " + n + " slave nodes";
      TypedProperties props = null;
      final File file = config.get(JPPFProperties.PROVISIONING_STARTUP_OVERRIDES_FILE);
      if ((file != null) && file.exists()) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
          props = new TypedProperties().loadAndResolve(reader);
        } catch(final Exception e) {
          log.error("slave startup config overrides file {} could not be loaded: {}", file, ExceptionUtils.getStackTrace(e));
        }
      } else {
        final String source = config.get(JPPFProperties.PROVISIONING_STARTUP_OVERRIDES_SOURCE);
        if ((source != null) && !source.trim().isEmpty()) {
          try {
            final Class<?> c = Class.forName(source);
            if (JPPFConfiguration.ConfigurationSource.class.isAssignableFrom(c)) {
              final JPPFConfiguration.ConfigurationSource configReader = (JPPFConfiguration.ConfigurationSource) c.newInstance();
              props = new TypedProperties().loadAndResolve(new InputStreamReader(configReader.getPropertyStream()));
            } else {
              final JPPFConfiguration.ConfigurationSourceReader configReader = (JPPFConfiguration.ConfigurationSourceReader) c.newInstance();
              props = new TypedProperties().loadAndResolve(configReader.getPropertyReader());
            }
          } catch (final Exception e) {
            log.error("slave startup config overrides source {} could not be instantiated: {}", source, ExceptionUtils.getStackTrace(e));
          }
        }
      }
      if (props != null) msg += " with config overrides = " + props;
      log.info(msg);
      System.out.println(msg);
      shrinkOrGrowSlaves(n, true, props);
    }
  }

  /**
   * Get the next available slavez id.
   * @return the next id as an int value.
   * @since 4.2.2
   */
  private int nextAvailableId() {
    int count = 0;
    synchronized(reservedIds) {
      while (reservedIds.contains(count)) count++;
    }
    return count;
  }

  /**
   * Get the next available slave id.
   * @return the next id as an int value.
   * @since 4.2.2
   */
  private int reserveNextAvailableId() {
    final int count = nextAvailableId();
    synchronized(reservedIds) {
      reservedIds.add(count);
    }
    return count;
  }

  /**
   * Remove the specified slave.
   * @param slave the slave to remove.
   */
  private void removeSlave(final SlaveNodeLauncher slave) {
    synchronized(slaves) {
      slaves.remove(slave.getId());
    }
    synchronized(reservedIds) {
      reservedIds.remove(slave.getId());
    }
  }
}
