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
package org.jppf.process;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.node.idle.*;
import org.jppf.utils.*;
import org.jppf.utils.configuration.*;
import org.slf4j.*;

/**
 * <p>This class is intended as a controller for a subprocess, to enable stopping and restarting it when requested.
 * <p>It performs the following operations:
 * <ul>
 * <li>open a server socket the driver will listen to (port number is dynamically attributed)</li>
 * <li>Start the subprocess, sending the server socket port number as an argument</li>
 * <li>Wait for the subprocess to exit</li>
 * <li>If the subprocess exit code is equal to 2, the subprocess is restarted, otherwise this process exits as well</li>
 * </ul>
 * @author Laurent Cohen
 */
public class ProcessLauncher extends AbstractProcessLauncher implements ProcessWrapperEventListener, IdleStateListener {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ProcessLauncher.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The fully qualified name of the main class of the subprocess to launch.
   */
  private final String mainClass;
  /**
   * Either "node" or "driver".
   */
  private final String jppfType;
  /**
   * Determines whether the process was stopped because the system went into "busy state".
   */
  private final AtomicBoolean stoppedOnBusyState = new AtomicBoolean(false);
  /**
   * Determines whether the system is in "idle state".
   */
  private final AtomicBoolean idle = new AtomicBoolean(true);
  /**
   * Whether idle mode can be used on the process.
   */
  private final boolean idleModeSupported;
  /**
   * Specifies whether the subprocess is launched only when the system is idle.
   */
  private boolean idleMode;
  /**
   * Detects system idle state changes.
   */
  private IdleDetector idleDetector;
  /**
   * Whether the node is configured for immediate shutdown.
   */
  private boolean idleModeImmediateShutdown = true;

  /**
   * Initialize this process launcher.
   * @param mainClass the fully qualified name of the main class of the sub process to launch.
   * @param idleModeSupported whether idle mode can be used on the process.
   */
  public ProcessLauncher(final String mainClass, final boolean idleModeSupported) {
    if (mainClass == null) throw new IllegalArgumentException("the main class name cannot be null");
    this.mainClass = mainClass;
    jppfType = mainClass.toLowerCase().contains("driver") ? "driver" : "node";
    this.idleModeSupported = idleModeSupported;
    final int idx = mainClass.lastIndexOf('.');
    this.name = (idx < 0) ? mainClass : mainClass.substring(idx);
  }

  /**
   * Start the socket listener and the subprocess.
   */
  @Override
  public void run() {
    if (idleModeSupported) {
      final TypedProperties config = JPPFConfiguration.getProperties();
      idleMode = config.get(JPPFProperties.IDLE_MODE_ENABLED);
      idleModeImmediateShutdown = config.get(JPPFProperties.IDLE_INTERRUPT_IF_RUNNING);
    }
    boolean end = false;
    try {
      createShutdownHook();
      if (idleMode) {
        idleDetector = new IdleDetector(this);
        System.out.printf("Node running in \"Idle Host\" mode with %s shutdown%n", (idleModeImmediateShutdown ? "immediate" : "deferred"));
        idleDetector.run();
      }
      while (!end) {
        startSocketListener();
        if (idleMode) {
          while (idle.get()) goToSleep();
        }
        startProcess();
        final int n = process.waitFor();
        end = onProcessExit(n);
        if (process != null) process.destroy();
        if (!end) {
          JPPFConfiguration.reset();
          final TypedProperties overrides = new ConfigurationOverridesHandler().load(false);
          if (overrides != null) JPPFConfiguration.getProperties().putAll(overrides);
        }
      }
    } catch (final Exception e) {
      e.printStackTrace();
    }
    System.exit(0);
  }

  /**
   * Start the sub-process.
   * @throws Exception if any error occurs.
   */
  public void startProcess() throws Exception {
    stoppedOnBusyState.set(false);
    process = buildProcess();
    createProcessWrapper(process);
    if (debugEnabled) log.debug("started {} process [{}]", jppfType, process);
  }

  /**
   * Start the JPPF driver subprocess.
   * @return A reference to the {@link Process} object representing the JPPF driver subprocess.
   * @throws Exception if the process failed to start.
   */
  private Process buildProcess() throws Exception {
    final TypedProperties config = JPPFConfiguration.getProperties();
    final String s = config.get(JPPFProperties.JVM_OPTIONS);
    if (debugEnabled) log.debug("jppf.jvm.options=" + s);
    final Pair<List<String>, List<String>> parsed = parseJvmOptions(s);
    final List<String> jvmOptions = parsed.first();
    final List<String> cpElements = parsed.second();
    cpElements.add(0, System.getProperty("java.class.path"));
    final List<String> command = new ArrayList<>();
    command.add(computeJavaExecPath(config));
    command.add("-cp");
    command.add(buildClasspath(cpElements));
    for (final Map.Entry<Object, Object> entry: System.getProperties().entrySet()) {
      final Object key = entry.getKey();
      if ((key instanceof String) && (entry.getValue() instanceof String)) {
        if (((String) key).startsWith("jppf.")) command.add(String.format("-D%s=%s", key, entry.getValue()));
      }
    }
    for (String opt: jvmOptions) command.add(opt);
    final String log4j = System.getProperty("log4j.configuration");
    if (log4j != null) command.add("-Dlog4j.configuration=" + log4j);
    command.add(mainClass);
    command.add(Integer.toString(processPort));
    if (debugEnabled) log.debug("process command:\n" + command);
    final ProcessBuilder builder = new ProcessBuilder(command);
    return builder.start();
  }

  /**
   * Create a process wrapper around the specified process, to capture its console output
   * and prevent it from blocking.
   * @param p the process whose output is to be captured.
   * @return a <code>ProcessWrapper</code> instance.
   */
  protected ProcessWrapper createProcessWrapper(final Process p) {
    final ProcessWrapper wrapper = new ProcessWrapper(process);
    wrapper.addListener(this);
    return wrapper;
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
    String s = getOutput(process, "std").trim();
    if (s.length() > 0) {
      System.out.println("\nstandard output:\n" + s);
      log.info("standard output:\n" + s);
    }
    s = getOutput(process, "err").trim();
    if (s.length() > 0) {
      System.out.println("\nerror output:\n" + s);
      log.info("error output:\n" + s);
    }
    System.out.println("process exited with code " + n);
    return (n != 2) && !stoppedOnBusyState.get();
  }

  /**
   * Get the output of the driver process.
   * @param process the process to get the standard or error output from.
   * @param streamType determines whether to obtain the standard or error output.
   * @return the output as a string.
   */
  public String getOutput(final Process process, final String streamType) {
    final StringBuilder sb = new StringBuilder();
    try {
      final InputStream is = "std".equals(streamType) ? process.getInputStream() : process.getErrorStream();
      final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      try {
        String s = "";
        while (s != null) {
          s = reader.readLine();
          if (s != null) sb.append(s).append('\n');
        }
      } finally {
        reader.close();
      }
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
    }
    return sb.toString();
  }

  /**
   * Notification that the process has written to its error stream.
   * @param event encapsulate the error stream's content.
   */
  @Override
  public void errorStreamAltered(final ProcessWrapperEvent event) {
    System.err.print(event.getContent());
  }

  /**
   * Notification that the process has written to its output stream.
   * @param event encapsulate the output stream's content.
   */
  @Override
  public void outputStreamAltered(final ProcessWrapperEvent event) {
    System.out.print(event.getContent());
  }

  @Override
  public void idleStateChanged(final IdleStateEvent event) {
    final IdleState state = event.getState();
    if (debugEnabled) log.debug("idle state changed to {}", state);
    if ((state == IdleState.IDLE) && !idle.get()) {
      if (idleMode && (process != null)) {
        idle.set(true);
        stoppedOnBusyState.set(true);
        final int action = idleModeImmediateShutdown ? ProcessCommands.SHUTDOWN_INTERRUPT : ProcessCommands.SHUTDOWN_NO_INTERRUPT;
        if (debugEnabled) log.debug("sending command {}", ProcessCommands.getCommandName(action));
        sendActionCommand(action);
      }
    } else if ((state == IdleState.BUSY) && idle.get()) {
      idle.set(false);
      wakeUp();
    }
  }
}
