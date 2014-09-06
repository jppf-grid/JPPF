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
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jppf.comm.socket.*;
import org.jppf.utils.*;
import org.jppf.utils.streams.StreamUtils;
import org.slf4j.*;

/**
 * This class wraps a single slave node process and provides an API to start, stop and monitor it.
 * @author Laurent Cohen
 * @since 4.1
 * @exclude
 */
public class SlaveNodeLauncher implements Runnable {
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
   * A reference to the JPPF driver subprocess, used to kill it when the driver launcher exits.
   */
  private Process process = null;
  /**
   * Id given to this process.
   * @since 4.2.2
   */
  private final int id;
  /**
   * Name given to this process, also used as root installation directory.
   */
  private final String name;
  /**
   * Root dir for the new created node.
   */
  private final File slaveDir;
  /**
   * The classpath for the slave node.
   */
  private final List<String> classpath;
  /**
   * The registered listeners.
   */
  private final List<SlaveNodeLauncherListener> listeners = new CopyOnWriteArrayList<>();
  /**
   * The server socket the node listens to.
   */
  private ServerSocket processServer = null;
  /**
   * The port number the server socket listens to.
   */
  private int processPort = 0;
  /**
   * 
   */
  private SlaveSocketWrapper slaveSocketWrapper = null;

  /**
   * Initialize this process launcher.
   * @param name internal name given tot he process.
   * @param classpath the slave node's classpath.
   */
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
      while (!end) {
        startSocketListener();
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
    jvmOptions.addAll(parseJvmOptions(s));
    s = config.getString(NodeProvisioningConstants.SLAVE_JVM_OPTIONS_PROPERTY);
    jvmOptions.addAll(parseJvmOptions(s));
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
    return builder.start();
  }

  /**
   * Parse the specified String as a list of JVM options.
   * @param s the string to parse.
   * @return A list of jvm options.
   */
  private List<String> parseJvmOptions(final String s) {
    List<String> jvmOptions = new ArrayList<>();
    if (s != null) {
      String[] options = s.split("\\s");
      int count = 0;
      while (count < options.length) {
        String option = options[count++];
        // ignore the classpath elements
        if ("-cp".equalsIgnoreCase(option) || "-classpath".equalsIgnoreCase(option)) count++;
        else jvmOptions.add(option);
      }
    }
    return jvmOptions;
  }

  /**
   * Forcibly terminate the process.
   */
  public void stopProcess() {
    if (process != null) process.destroy();
  }

  /**
   * Called when the subprocess has exited with exit value n.
   * This allows for printing the residual output (both standard and error) to this pJVM's console and log file,
   * in order to get additional information if a problem occurred.
   * @param n the exit value of the subprocess.
   * @return true if this launcher is to be terminated, false if it should re-launch the subprocess.
   */
  private boolean onProcessExit(final int n) {
    return (n != 2);
  }

  /**
   * Start a server socket that will accept one connection at a time with the JPPF driver, so the server can shutdown properly,
   * when this driver is killed, by a way other than the API (ie CTRL-C or killing the process through the OS shell).<br>
   * The port the server socket listens to is dynamically attributed, which is obtained by using the constructor
   * <code>new ServerSocket(0)</code>.<br>
   * The driver will connect and listen to this port, and exit when the connection is broken.<br>
   * The single connection at a time is obtained by doing the <code>ServerSocket.accept()</code> and the
   * <code>Socket.getInputStream().read()</code> in the same thread.
   * @return the port number on which the server socket is listening.
   */
  protected int startSocketListener() {
    try {
      if (processServer == null) {
        processServer = new ServerSocket(0);
        processPort = processServer.getLocalPort();
      }
      slaveSocketWrapper = new SlaveSocketWrapper();
      Thread thread = new Thread(slaveSocketWrapper, name + "ServerSocket");
      thread.setDaemon(true);
      thread.start();
    } catch(Exception e) {
      if (processServer != null) StreamUtils.closeSilent(processServer);
    }
    return processPort;
  }

  /**
   * Create a shutdown hook that is run when this JVM terminates.<br>
   * This is normally used to ensure the subprocess is terminated as well.
   */
  protected void createShutdownHook() {
    Runnable hook = new Runnable() {
      @Override
      public void run() {
        stopProcess();
      }
    };
    Runtime.getRuntime().addShutdownHook(new Thread(hook));
  }

  /**
   * Get the name given to this process.
   * @return the name as a string.
   */
  public String getName() {
    return name;
  }

  /**
   * Add a listener to the list of listeners.
   * @param listener the listener to add.
   */
  public void addProcessLauncherListener(final SlaveNodeLauncherListener listener) {
    if (listener == null) return;
    listeners.add(listener);
  }

  /**
   * Remove a listener from the list of listeners.
   * @param listener the listener to remove.
   */
  public void removeProcessLauncherListener(final SlaveNodeLauncherListener listener) {
    if (listener == null) return;
    listeners.remove(listener);
  }

  /**
   * Notify all listeners that the process has started.
   */
  private void fireProcessStarted() {
    if (log.isDebugEnabled()) log.debug("process [{}:{}] has started", name, process);
    SlaveNodeLauncherEvent event = new SlaveNodeLauncherEvent(this);
    for (SlaveNodeLauncherListener listener: listeners) listener.processStarted(event);
  }

  /**
   * Notify all listeners that the process has stopped.
   */
  private void fireProcessStopped() {
    if (log.isDebugEnabled()) log.debug("process [{}:{}] has stopped", name, process);
    SlaveNodeLauncherEvent event = new SlaveNodeLauncherEvent(this);
    for (SlaveNodeLauncherListener listener: listeners) listener.processStopped(event);
  }

  /**
   * Get the id given to this process.
   * @return the id as an int.
   */
  public int getId() {
    return id;
  }

  /**
   * Send an action command to the slave node.
   * @param action the code of the action command to send.
   */
  public void sendActionCommand(final int action) {
    if (slaveSocketWrapper != null) slaveSocketWrapper.sendActionCommand(action);
  }

  /**
   *
   */
  private class SlaveSocketWrapper implements Runnable {
    /** Wrapper for the accepted socket. */
    private SocketWrapper socketClient = null;

    @Override
    public void run() {
      try {
        socketClient = new BootstrapSocketClient(processServer.accept());
        int n = socketClient.readInt();
        if (n == -1) throw new EOFException();
      } catch(Exception ioe) {
        if (log.isDebugEnabled()) log.debug(name, ioe);
        if (socketClient != null) StreamUtils.closeSilent(socketClient);
      }
    }

    /**
     * Send an action command to the slave node.
     * @param action the code of the action command to send.
     */
    private void sendActionCommand(final int action) {
      if (socketClient == null) return;
      try {
        socketClient.writeInt(action);
      } catch (Exception e) {
        if (log.isDebugEnabled()) log.debug("could not send command to slave process {} : {}", name, ExceptionUtils.getStackTrace(e));
      }
    }
  }
}
