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

package org.jppf.node.provisioning;

import static org.jppf.node.provisioning.NodeProvisioningConstants.*;

import java.io.*;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class manages the slave nodes.
 * @author Laurent Cohen
 * @since 4.1
 * @exclude
 */
public final class SlaveNodeManager implements SlaveNodeLauncherListener {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(SlaveNodeManager.class);
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
   * The directpry where the slave's config files are located, relative to its root folder.
   */
  static final String SLAVE_LOCAL_CONFIG_DIR = "config";
  /**
   * The name of the slave's JPPF config file.
   */
  static final String SLAVE_LOCAL_CONFIG_FILE = "jppf-node.properties";
  /**
   * Singleton instance of this class.
   */
  static final SlaveNodeManager INSTANCE = new SlaveNodeManager();
  /**
   * A mapping of the slave processes to their internal name.
   */
  private final SortedMap<Integer, SlaveNodeLauncher> slaves = new TreeMap<>();
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
   * Initialize this manager.
   */
  private SlaveNodeManager() {
    masterDir = new File(System.getProperty("user.dir"));
    log.debug("masterDir = {}", masterDir);
    computeSlaveClasspath();
    /*
    int n = JPPFConfiguration.getProperties().getInt(STARTUP_SLAVES_PROPERTY, 0);
    if (n > 0) shrinkOrGrowSlaves(n, null);
    */
  }

  /**
   * Start or stop the required number of slaves to reach the specified number,
   * using the specified config overrides. If {@code configOverrides} is null, then previous overrides are applied.
   * @param requestedSlaves the number of slaves to reach.
   * @param configOverrides a set of overrides to the slave's configuration.
   */
  synchronized void shrinkOrGrowSlaves(final int requestedSlaves, final TypedProperties configOverrides) {
    // if new config overides, stop all the slaves and restart new ones
    if (configOverrides != null) {
      log.debug("stopping all processes");
      this.configOverrides = configOverrides;
      for (SlaveNodeLauncher slave: slaves.values()) {
        slave.removeProcessLauncherListener(this);
        slave.stopProcess();
      }
      slaves.clear();
      reservedIds.clear();
    }
    int size = slaves.size();
    int diff = size - requestedSlaves;
    // if running slaves > requested ones, stop those not needed 
    if (diff > 0) {
      log.debug("stopping " + diff + " processes");
      for (int i=requestedSlaves; i<size; i++) {
        int id = slaves.lastKey();
        SlaveNodeLauncher slave = slaves.remove(id);
        log.debug("stopping {}", slave.getName());
        reservedIds.remove(id);
        slave.removeProcessLauncherListener(this);
        slave.stopProcess();
      }
    } else {
      // otherwise start the missing number of slaves
      log.debug("starting " + -diff + " processes");
      for (int i=size; i<requestedSlaves; i++) {
        int id = reserveNextAvailableId();
        String slaveDirPath = SLAVE_PATH_PREFIX + id;
        try {
          log.debug("starting {}", slaveDirPath);
          setupSlaveNodeFiles(slaveDirPath, this.configOverrides);
          final SlaveNodeLauncher slave = new SlaveNodeLauncher(id, slaveDirPath, slaveClasspath);
          slave.addProcessLauncherListener(this);
          new Thread(slave, slaveDirPath).start();
        } catch(Exception e) {
          log.error("error trying to start '{}' : {}", slaveDirPath, e);
        }
      }
    }
  }

  /**
   * Get the number of running slave nodes.
   * @return the number of slaves as an int.
   */
  public synchronized int nbSlaves() {
    return slaves.size();
  }

  /**
   * Copy the master's configuration files from the master's install folder into the slave's root folder.
   * Configuration overrides are applied here.
   * @param slaveDirPath name of the slave node.
   * @param configOverrides the overrides to apply to the slave's configuration.
   * @throws Exception if any error occurs.
   */
  private void setupSlaveNodeFiles(final String slaveDirPath, final TypedProperties configOverrides) throws Exception {
    File slaveDir = new File(slaveDirPath);
    if (!slaveDir.exists()) slaveDir.mkdirs();
    File slaveConfigSrc = new File(SLAVE_CONFIG_PATH);
    File slaveConfigDest = new File(slaveDir, SLAVE_LOCAL_CONFIG_DIR);
    if (!slaveConfigDest.exists()) slaveConfigDest.mkdirs();
    if (slaveConfigSrc.exists()) FileUtils.copyDirectory(slaveConfigSrc, slaveConfigDest);
    // get the JPPF config, apply the overrides, then save it to the slave's folder
    TypedProperties props = new TypedProperties(JPPFConfiguration.getProperties());
    for (String key: configOverrides.stringPropertyNames()) props.setProperty(key, configOverrides.getProperty(key));
    props.setBoolean(MASTER_PROPERTY, false);
    props.setBoolean(SLAVE_PROPERTY, true);
    props.setProperty("jppf.redirect.out", "system_out.log");
    props.setProperty("jppf.redirect.err", "system_err.log");
    try (Writer writer = new BufferedWriter(new FileWriter(new File(slaveConfigDest, SLAVE_LOCAL_CONFIG_FILE)))) {
      props.store(writer, "generated jppf configuration");
    }
  }

  @Override
  public synchronized void processStarted(final SlaveNodeLauncherEvent event) {
    SlaveNodeLauncher slave = event.getProcessLauncher();
    slaves.put(slave.getId(), slave);
  }

  @Override
  public synchronized void processStopped(final SlaveNodeLauncherEvent event) {
    SlaveNodeLauncher slave = event.getProcessLauncher();
    slaves.remove(slave.getId());
    reservedIds.remove(slave.getId());
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
    if (n > 0) INSTANCE.shrinkOrGrowSlaves(n, null);
  }

  /**
   * Get the next available slavez id.
   * @return the next id as an int value.
   * @since 4.2.2
   */
  private int nextAvailableId() {
    int count = 0;
    while (reservedIds.contains(count)) count++;
    return count;
  }

  /**
   * Get the next available slavez id.
   * @return the next id as an int value.
   * @since 4.2.2
   */
  private synchronized int reserveNextAvailableId() {
    int count = nextAvailableId();
    reservedIds.add(count);
    return count;
  }
}
