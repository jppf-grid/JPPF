/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
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
   *
   */
  private boolean started = false;
  /**
   *
   */
  private boolean stopped = false;
  /**
   * Captures the exit code of the slave node process.
   */
  int exitCode = -1;

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
    Thread hookThread = null;
    try {
      hookThread = createShutdownHook();
      startSocketListener();
      synchronized(this) {
        if (isStopped()) return;
        process = startProcess();
        setStarted(true);
        fireProcessStarted();
      }
      exitCode = process.waitFor();
      end = onProcessExit(exitCode);
      fireProcessStopped(false);
      tearDown();
    } catch (Exception e) {
      e.printStackTrace();
      fireProcessStopped(false);
    } finally {
      setStarted(false);
      try {
        if (hookThread != null) Runtime.getRuntime().removeShutdownHook(hookThread);
      } catch (Exception ignore) {
      }
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
    List<String> cpElements = new ArrayList<>(classpath);
    String s = config.get(JPPFProperties.JVM_OPTIONS);
    Pair<List<String>, List<String>> parsed = parseJvmOptions(s);
    jvmOptions.addAll(parsed.first());
    cpElements.addAll(parsed.second());
    s = config.get(JPPFProperties.PROVISIONING_SLAVE_JVM_OPTIONS);
    parsed = parseJvmOptions(s);
    jvmOptions.addAll(parsed.first());
    cpElements.addAll(parsed.second());
    if (log.isDebugEnabled()) log.debug("JVM options: " + jvmOptions);
    List<String> command = new ArrayList<>();
    command.add(computeJavaExecPath(config));
    command.add("-cp");
    command.add(buildClasspath(cpElements));
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

  /**
   * Determine whether the slave node is started.
   * @return {@code true} if the slave is started, {@code false} otherwise.
   */
  public synchronized boolean isStarted() {
    return started;
  }

  /**
   * Specify whether the slave node is started.
   * @param started {@code true} if the slave is started, {@code false} otherwise.
   */
  public synchronized void setStarted(final boolean started) {
    this.started = started;
  }

  /**
   * Determine whether the slave node is stopped.
   * @return {@code true} if the slave is stopped, {@code false} otherwise.
   */
  @Override
  public boolean isStopped() {
    return stopped;
  }

  /**
   * Specify whether the slave node is stopped.
   * @param stopped {@code true} if the slave is stopped, {@code false} otherwise.
   */
  @Override
  public void setStopped(final boolean stopped) {
    this.stopped = stopped;
  }
}
