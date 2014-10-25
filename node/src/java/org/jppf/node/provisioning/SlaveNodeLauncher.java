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

import java.io.*;
import java.util.*;

import org.jppf.process.AbstractProcessLauncher;
import org.jppf.utils.TypedProperties;
import org.slf4j.*;

/**
 * This class wraps a single slave node process and provides an API to start, stop and monitor it.
 * @author Laurent Cohen
 * @since 4.1
 * @exclude
 */
public class SlaveNodeLauncher extends AbstractProcessLauncher {
  /**
   * The fully qualified name of the main class of the subprocess to launch.
   */
  private static final String MAIN_CLASS = "org.jppf.node.NodeRunner";
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(SlaveNodeLauncher.class);
  /**
   * The location of the slave's JPPF config file, relative to its root folder.
   */
  static final String SLAVE_LOCAL_CONFIG_PATH = SlaveNodeManager.SLAVE_LOCAL_CONFIG_DIR + "/" + SlaveNodeManager.SLAVE_LOCAL_CONFIG_FILE;
  /**
   * Id given to this process.
   * @since 4.2.2
   */
  private final int id;
  /**
   * Root dir for the new created node.
   */
  private final File slaveDir;
  /**
   * The classpath for the slave node.
   */
  private final List<String> classpath;

  /**
   * Initialize this process launcher.
   * @param id the id as an int.
   * @param name internal name given tot he process.
   * @param classpath the slave node's classpath.
   */
  public SlaveNodeLauncher(final int id, final String name, final List<String> classpath) {
    this.id = id;
    this.name = name;
    this.classpath = classpath;
    slaveDir = new File(name);
    if (log.isDebugEnabled()) log.debug("slaveDir = " + slaveDir);
  }

  /**
   * Start the subprocess. If exit code is 2, then the subprocess is restarted.
   */
  @Override
  public void run() {
    boolean end = false;
    try {
      createShutdownHook();
      startSocketListener();
      while (!end) {
        process = startProcess();
        fireProcessStarted();
        int n = process.waitFor();
        fireProcessStopped();
        end = onProcessExit(n);
        if (process != null) process.destroy();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Start the JPPF node subprocess.
   * @return A reference to the {@link Process} object representing the JPPF driver subprocess.
   * @throws Exception if the process failed to start.
   */
  private Process startProcess() throws Exception {
    File configFile = new File(slaveDir, SLAVE_LOCAL_CONFIG_PATH);
    TypedProperties config = null;
    try (Reader reader = new BufferedReader(new FileReader(configFile))) {
      config = new TypedProperties().loadAndResolve(reader);
    }
    if (log.isDebugEnabled()) log.debug("{} read config {} : {}", new Object[] {name, configFile, config});
    List<String> jvmOptions = new ArrayList<>();
    String s = config.getString("jppf.jvm.options");
    jvmOptions.addAll(parseJvmOptions(s).first());
    s = config.getString(NodeProvisioningConstants.SLAVE_JVM_OPTIONS_PROPERTY);
    jvmOptions.addAll(parseJvmOptions(s).first());
    if (log.isDebugEnabled()) log.debug("JVM options: " + jvmOptions);
    List<String> command = new ArrayList<>();
    command.add(System.getProperty("java.home")+"/bin/java");
    command.add("-cp");
    String pathSeparator = System.getProperty("path.separator");
    StringBuilder sb = new StringBuilder();
    for (int i=0; i<classpath.size(); i++) {
      if (i > 0) sb.append(pathSeparator);
      sb.append(classpath.get(i));
    }
    command.add(sb.toString());
    for (String opt: jvmOptions) command.add(opt);
    command.add("-Djppf.config=" + SLAVE_LOCAL_CONFIG_PATH);
    command.add(MAIN_CLASS);
    command.add("" + processPort);
    if (log.isDebugEnabled()) log.debug("process command for {}:\n{}", name, command);
    ProcessBuilder builder = new ProcessBuilder(command);
    builder.directory(slaveDir);
    builder.redirectOutput(new File(slaveDir, "system_out.log"));
    builder.redirectError(new File(slaveDir, "system_err.log"));
    return builder.start();
  }

  /**
   * Called when the subprocess has exited with exit value n.
   * This allows for printing the residual output (both standard and error) to this pJVM's console and log file,
   * in order to get additional information if a problem occurred.
   * @param n the exit value of the subprocess.
   * @return true if this launcher is to be terminated, false if it should re-launch the subprocess.
   */
  @Override
  protected boolean onProcessExit(final int n) {
    return (n != 2);
  }

  /**
   * Get the id given to this process.
   * @return the id as an int.
   */
  public int getId() {
    return id;
  }
}
