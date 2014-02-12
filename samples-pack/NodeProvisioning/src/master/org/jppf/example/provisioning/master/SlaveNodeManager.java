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

package org.jppf.example.provisioning.master;

import java.io.*;
import java.util.*;

import org.jppf.node.protocol.*;
import org.jppf.utils.TypedProperties;
import org.slf4j.*;

/**
 * This class manages the slave nodes.
 * @author Laurent Cohen
 */
public class SlaveNodeManager implements SlaveNodeLauncherListener {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(SlaveNodeManager.class);
  /**
   * Name of the property which defines a node as master.
   */
  private static final String MASTER_PROPERTY = "jppf.node.master";
  /**
   * Prefix used for the slave node names, followed by a number.
   */
  private static final String SLAVE_PREFIX = "slave_node_";
  /**
   * A mapping of the slave processes to their internal name.
   */
  private final Map<String, SlaveNodeLauncher> slaves = new HashMap<>();
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
  public SlaveNodeManager() {
    masterDir = new File(System.getProperty("user.dir"));
    log.info("masterDir = {}", masterDir);
    addToClassPath("lib");
    addToClassPath("lib-slave");
    slaveClasspath.add(".");
  }

  /**
   * Start or stop the required number of slaves to reach the specified number,
   * using the specified config overrides. If {@code configOverrides} is null, then previous overrides are applied.
   * @param requestedSlaves the number of slaves to reach.
   * @param configOverrides a set of overrides to the slave's configuration.
   */
  public synchronized void shrinkOrGrowSlaves(final int requestedSlaves, final TypedProperties configOverrides) {
    // if new config ovverides, stop all the slaves and restart new ones
    if (configOverrides != null) {
      log.info("stopping all processes");
      this.configOverrides = configOverrides;
      for (Map.Entry<String, SlaveNodeLauncher> entry: slaves.entrySet()) {
        SlaveNodeLauncher slave = entry.getValue();
        slave.removeProcessLauncherListener(this);
        slave.stopProcess();
      }
      slaves.clear();
    }
    int size = slaves.size();
    int diff = size - requestedSlaves;
    // if running slaves > requested ones, stop those not needed 
    if (diff > 0) {
      log.debug("stopping " + diff + " processes");
      for (int i=requestedSlaves; i<size; i++) {
        String name = SLAVE_PREFIX + i;
        log.debug("stopping {}", name);
        SlaveNodeLauncher slave = slaves.remove(name);
        slave.removeProcessLauncherListener(this);
        slave.stopProcess();
      }
    } else {
      // otherwise start the missing number of slaves
      log.debug("starting " + -diff + " processes");
      for (int i=size; i<requestedSlaves; i++) {
        String name = SLAVE_PREFIX + i;
        try {
          log.debug("starting {}", name);
          setupSlaveNodeFiles(name, this.configOverrides);
          final SlaveNodeLauncher slave = new SlaveNodeLauncher(name, slaveClasspath);
          slave.addProcessLauncherListener(this);
          new Thread(slave, name).start();
        } catch(Exception e) {
          log.error("error trying to start '{}' : {}", name, e);
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
   * Add the jars in the specified folder to the slave's classpath.
   * @param libPath path to a directory containing jar files.
   */
  private void addToClassPath(final String libPath) {
    try {
      File libDir = new File(libPath);
      if (!libDir.exists()) {
        log.warn("specified lib dir '{}' does not exist", libPath);
        return;
      }
      // list all the jar files in input folder
      String[] files = libDir.list(new FilenameFilter() {
        @Override
        public boolean accept(final File dir, final String name) {
          return (name != null) && name.toLowerCase().endsWith(".jar");
        }
      });
      if ((files == null) || (files.length <= 0)) return;
      for (String file: files) slaveClasspath.add(new File(libPath, file).getAbsolutePath());
    } catch (Exception e) {
      log.error("error adding the jars in {} : {}", libPath, e);
    }
  }

  /**
   * Copy the master's configuration files from the master's install folder into the slave's root folder.
   * Configuration overrides are applied here.
   * @param name name of the slave node.
   * @param configOverrides the overrides to apply to the slave's configuration.
   * @throws Exception if any error occurs.
   */
  private void setupSlaveNodeFiles(final String name, final TypedProperties configOverrides) throws Exception {
    //File slaveDir = new File(masterDir, "../" + name);
    File slaveDir = new File(masterDir, name);
    if (!slaveDir.exists()) slaveDir.mkdirs();
    // copy the logging config files
    copyConfigFile(slaveDir, "log4j-node.properties");
    copyConfigFile(slaveDir, "logging-node.properties");
    // load the JPPF config, apply the overrides, then save it to the slave's folder
    File jppfConfigPath = new File(masterDir, "config/jppf-node.properties");
    TypedProperties props = null;
    try (Reader reader = new BufferedReader(new FileReader(jppfConfigPath))) {
      props = TypedProperties.loadAndResolve(reader);
    }
    for (String key: configOverrides.stringPropertyNames()) props.setProperty(key, configOverrides.getProperty(key));
    props.remove(MASTER_PROPERTY);
    try (Writer writer = new BufferedWriter(new FileWriter(new File(slaveDir, "jppf-node.properties")))) {
      props.store(writer, "generated");
    }
  }

  /**
   * Copy a config file from the master's 'config' folder to the slave's root folder.
   * @param slaveDir the root folder of the slave node.
   * @param filename the name of the file to copy.
   * @throws Exception if any error occurs.
   */
  private void copyConfigFile(final File slaveDir, final String filename) throws Exception {
    Location<?> src = new FileLocation(new File(masterDir, "config/" + filename));
    Location<?> dest = new FileLocation(new File(slaveDir, filename));
    src.copyTo(dest);
  }

  @Override
  public synchronized void processStarted(final SlaveNodeLauncherEvent event) {
    SlaveNodeLauncher slave = event.getProcessLauncher();
    slaves.put(slave.getName(), slave);
  }

  @Override
  public synchronized void processStopped(final SlaveNodeLauncherEvent event) {
    SlaveNodeLauncher slave = event.getProcessLauncher();
    slaves.remove(slave.getName());
  }
}
