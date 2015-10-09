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

package org.jppf.node.provisioning;

import static org.jppf.node.provisioning.NodeProvisioningConstants.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.io.FileUtils;
import org.jppf.node.NodeRunner;
import org.jppf.process.*;
import org.jppf.utils.*;
import org.jppf.utils.ConcurrentUtils.Condition;
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
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Path prefix used for the root directory of each slave node.
   * The provisioning facility will then add a sequence number as suffix, to distinguish between slave nodes.
   */
  private static final String SLAVE_PATH_PREFIX = JPPFConfiguration.getProperties().getString(SLAVE_PATH_PREFIX_PROPERTY, "slave_nodes/node_");
  /**
   * Directory where configuration files, other than the jppf configuration, are located.
   * The files in this folder will be copied into each slave node's 'config' directory.
   */
  private static final String SLAVE_CONFIG_PATH = JPPFConfiguration.getProperties().getString(SLAVE_CONFIG_PATH_PROPERTY, "config");
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
  static final long REQUEST_CHECK_TIMEOUT = JPPFConfiguration.getProperties().getLong("jppf.provisioning.request.check.timeout", 15_000L);
  /**
   * Singleton instance of this class.
   */
  static final SlaveNodeManager INSTANCE = new SlaveNodeManager();
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
   * Initialize this manager.
   */
  private SlaveNodeManager() {
    masterDir = new File(System.getProperty("user.dir"));
    if (debugEnabled) log.debug("masterDir = {}, request check timeout = {} ms", masterDir, REQUEST_CHECK_TIMEOUT);
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
    executor.submit(new Runnable() {
      @Override
      public void run() {
        shrinkOrGrowSlaves(requestedSlaves, interruptIfRunning, configOverrides);
      }
    });
  }

  /**
   * Start or stop the required number of slaves to reach the specified number,
   * using the specified config overrides. If {@code configOverrides} is null, then previous overrides are applied.
   * @param requestedSlaves the number of slaves to reach.
   * @param interruptIfRunning if true then nodes can only be stopped once they are idle. 
   * @param configOverrides a set of overrides to the slave's configuration.
   */
  private void shrinkOrGrowSlaves(final int requestedSlaves, final boolean interruptIfRunning, final TypedProperties configOverrides) {
    if (debugEnabled) log.debug(String.format("provisioning request for %d slaves, interruptIfRunning=%b, configOverrides=%s", requestedSlaves, interruptIfRunning, configOverrides));
    int action = interruptIfRunning ? ProcessCommands.SHUTDOWN_INTERRUPT : ProcessCommands.SHUTDOWN_NO_INTERRUPT;
    // if new config overides, stop all the slaves and restart new ones
    if (configOverrides != null) {
      if (debugEnabled) log.debug("stopping all processes");
      this.configOverrides = configOverrides;
      synchronized(slaves) {
        for (SlaveNodeLauncher slave: slaves.values()) {
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
    int size = nbSlaves();
    int diff = size - requestedSlaves;
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
      for (int i=size; i<requestedSlaves; i++) {
        id = reserveNextAvailableId();
        String slaveDirPath = SLAVE_PATH_PREFIX + id;
        try {
          log.debug("starting slave at {}", slaveDirPath);
          setupSlaveNodeFiles(slaveDirPath, this.configOverrides, id);
          final SlaveNodeLauncher slave = new SlaveNodeLauncher(id, slaveDirPath, slaveClasspath);
          slave.addProcessLauncherListener(this);
          new Thread(slave, slaveDirPath).start();
        } catch(Exception|Error e) {
          log.error("error trying to start '{}' : {}", slaveDirPath, ExceptionUtils.getStackTrace(e));
          if (e instanceof Error) throw (Error) e;
        }
      }
    }
    if (REQUEST_CHECK_TIMEOUT > 0) {
      long start = System.nanoTime();
      boolean check = ConcurrentUtils.awaitCondition(new Condition() {
        @Override public boolean evaluate() {
          return nbSlaves() == requestedSlaves;
        }
      }, REQUEST_CHECK_TIMEOUT);
      long elapsed = (System.nanoTime() - start) / 1_000_000L;
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
    File slaveDir = new File(slaveDirPath);
    if (!slaveDir.exists()) slaveDir.mkdirs();
    File slaveConfigSrc = new File(SLAVE_CONFIG_PATH);
    File slaveConfigDest = new File(slaveDir, SLAVE_LOCAL_CONFIG_DIR);
    if (!slaveConfigDest.exists()) slaveConfigDest.mkdirs();
    if (slaveConfigSrc.exists()) {
      FileUtils.copyDirectory(slaveConfigSrc, slaveConfigDest);
      if (debugEnabled) log.debug("copied files from {} to {}", slaveConfigSrc, slaveConfigDest);
    } else {
      if (debugEnabled) log.debug("config source dir '{}' does not exist", slaveConfigSrc);
    }
    // get the JPPF config, apply the overrides, then save it to the slave's folder
    TypedProperties config = JPPFConfiguration.getProperties();
    TypedProperties props = new TypedProperties(config);
    for (String key: configOverrides.stringPropertyNames()) props.setProperty(key, configOverrides.getProperty(key));
    props.setBoolean(MASTER_PROPERTY, false);
    props.setBoolean(SLAVE_PROPERTY, true);
    props.setInt(SLAVE_ID_PROPERTY, id);
    props.setString(MASTER_UUID_PROPERTY, NodeRunner.getUuid());
    try (Writer writer = new BufferedWriter(new FileWriter(new File(slaveConfigDest, SLAVE_LOCAL_CONFIG_FILE)))) {
      props.store(writer, "generated jppf configuration");
    }
  }

  @Override
  public void processStarted(final ProcessLauncherEvent event) {
    SlaveNodeLauncher slave = (SlaveNodeLauncher) event.getProcessLauncher();
    synchronized(slaves) {
      slaves.put(slave.getId(), slave);
    }
    if (nbSlaves() <= 0) log.warn("received processStarted() for slave id = {}, but nbSlaves is zero", slave.getId());
    else if (debugEnabled) log.debug("received processStarted() for slave id = {}", slave.getId());
  }

  @Override
  public void processStopped(final ProcessLauncherEvent event) {
    SlaveNodeLauncher slave = (SlaveNodeLauncher) event.getProcessLauncher();
    if (debugEnabled) log.debug("received processStopped() for slave id = {}, exitCode = {}", slave.getId(), slave.exitCode);
    if (slave.exitCode != 2) removeSlave(slave);
    else new Thread(slave, slave.getName()).start();
  }

  /**
   * Compute the classpath for the slave nodes.
   */
  private void computeSlaveClasspath() {
    String separator = System.getProperty("path.separator");
    String cp = System.getProperty("java.class.path");
    String[] paths = cp.split(separator);
    for (String path: paths) {
      if (path != null) slaveClasspath.add(new File(path).getAbsolutePath());
    }
    slaveClasspath.add(".");
    slaveClasspath.add(SLAVE_LOCAL_CONFIG_DIR);
  }

  /**
   * Automatically start slaves if specified in the configuration.
   */
  public static void handleStartup() {
    int n = JPPFConfiguration.getProperties().getInt(STARTUP_SLAVES_PROPERTY, 0);
    if (n > 0) {
      String msg = "starting " + n + " slave nodes";
      log.info(msg);
      System.out.println(msg);
      INSTANCE.shrinkOrGrowSlaves(n, true, null);
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
    int count = nextAvailableId();
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
