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

import java.io.EOFException;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.*;

import org.jppf.comm.socket.*;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.jppf.utils.streams.StreamUtils;
import org.slf4j.*;

/**
 * This class wraps a single slave node process and provides an API to start, stop and monitor it.
 * @author Laurent Cohen
 * @since 5.0
 * @exclude
 */
public abstract class AbstractProcessLauncher extends ThreadSynchronization implements Runnable {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AbstractProcessLauncher.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   *
   */
  private static final Pattern JVM_OPTIONS_PATTERN = Pattern.compile("([^\"\\s]*\"[^\"]*\"[^\"\\s]*|[^\"\\s]*)\\s*");
  /**
   * Constant for no JVM options.
   */
  private static final Pair<List<String>, List<String>> NO_OPTIONS = new Pair<>(new ArrayList<String>(), new ArrayList<String>());
  /**
   * A reference to the JPPF subprocess, used to kill it when tis launcher exits.
   */
  protected Process process;
  /**
   * The server socket the node listens to.
   */
  protected ServerSocket processServer;
  /**
   * The port number the server socket listens to.
   */
  protected int processPort;
  /**
   * Wraps the socket accepted from the node process.
   * @since 5.0
   */
  protected SlaveSocketWrapper slaveSocketWrapper;
  /**
   * The registered listeners.
   */
  protected final List<ProcessLauncherListener> listeners = new CopyOnWriteArrayList<>();
  /**
   * Name given to this process, also used as root installation directory.
   */
  protected String name;
  /**
   * If {@code false} then process is to be restarted.
   */
  protected boolean end = true;

  /**
   * Parse the specified String as a list of JVM options.
   * @param source the string to parse.
   * @return A list of jvm options.
   */
  protected Pair<List<String>, List<String>> parseJvmOptions(final String source) {
    if (source == null) return NO_OPTIONS;
    final List<String> options = new ArrayList<>();
    final Matcher matcher = JVM_OPTIONS_PATTERN.matcher(source);
    while (matcher.find()) {
      String s = matcher.group(1);
      if (s != null) {
        s = s.trim();
        if (!s.isEmpty()) options.add(s);
      }
    }
    if (debugEnabled) log.debug("options={}", options);
    final List<String> jvmOptions = new ArrayList<>();
    final List<String> cpElements = new ArrayList<>();
    int count = 0;
    while (count < options.size()) {
      final String option = options.get(count++);
      if ("-cp".equalsIgnoreCase(option) || "-classpath".equalsIgnoreCase(option)) cpElements.add(options.get(count++));
      else jvmOptions.add(option);
    }
    if (debugEnabled) log.debug("jvm options={}, cp elements={}", jvmOptions, cpElements);
    return new Pair<>(jvmOptions, cpElements);
  }

  /**
   * Build the ful classpath based onthe current process's classpath and the provided classpath elements.
   * @param cpElements the classpath elements to add to the classpath of the current process
   * @return a classpath string using the platform's path separator, eventually anclosed within double quotes if
   * any class path element contains one or more spaces.
   */
  protected String buildClasspath(final List<String> cpElements) {
    final StringBuilder sb = new StringBuilder();
    final String sep = System.getProperty("path.separator");
    for (int i=0; i<cpElements.size(); i++) {
      if (i > 0) sb.append(sep);
      sb.append(cpElements.get(i));
    }
    String cp = sb.toString().replace("\"", "");
    if (RegexUtils.SPACES_PATTERN.matcher(cp).find()) cp = "\"" + cp + "\"";
    return cp;
  }

  /**
   * Forcibly terminate the process.
   */
  public void tearDown() {
    if (process != null) {
      process.destroy();
      process = null;
    }
    if (processServer != null) {
      StreamUtils.closeSilent(processServer);
      processServer = null;
    }
  }

  /**
   * Called when the subprocess has exited with exit value n.
   * This allows for printing the residual output (both standard and error) to this pJVM's console and log file,
   * in order to get additional information if a problem occurred.
   * @param n the exit value of the subprocess.
   * @return true if this launcher is to be terminated, false if it should re-launch the subprocess.
   */
  protected abstract boolean onProcessExit(final int n);

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
      if (debugEnabled) log.debug("starting with {}", processServer);
      slaveSocketWrapper = new SlaveSocketWrapper();
      ThreadUtils.startDaemonThread(slaveSocketWrapper, getName() + "ServerSocket");
    } catch(@SuppressWarnings("unused") final Exception e) {
      if (processServer != null) StreamUtils.closeSilent(processServer);
      if (slaveSocketWrapper != null) StreamUtils.closeSilent(slaveSocketWrapper);
    }
    return processPort;
  }

  /**
   * Create a shutdown hook that is run when this JVM terminates.<br>
   * This is normally used to ensure the subprocess is terminated as well.
   * @return the created shutdown hook.
   */
  protected Thread createShutdownHook() {
    final Thread hookThread = new Thread(() -> tearDown(), getName());
    Runtime.getRuntime().addShutdownHook(hookThread);
    return hookThread;
  }

  /**
   * Get the name given to this process.
   * @return the name as a string.
   */
  public String getName() {
    return name == null ? getClass().getSimpleName() : name;
  }

  /**
   * Add a listener to the list of listeners.
   * @param listener the listener to add.
   */
  public void addProcessLauncherListener(final ProcessLauncherListener listener) {
    if (listener == null) return;
    listeners.add(listener);
  }

  /**
   * Remove a listener from the list of listeners.
   * @param listener the listener to remove.
   */
  public void removeProcessLauncherListener(final ProcessLauncherListener listener) {
    if (listener == null) return;
    listeners.remove(listener);
  }

  /**
   * Notify all listeners that the process has started.
   */
  protected void fireProcessStarted() {
    if (log.isDebugEnabled()) log.debug("process [{}:{}] has started", getName(), process);
    final ProcessLauncherEvent event = new ProcessLauncherEvent(this);
    for (ProcessLauncherListener listener: listeners) listener.processStarted(event);
  }

  /**
   * Notify all listeners that the process has stopped.
   * @param clearListeners {@code true} to remove all listeners, {@code false} to keep them.
   */
  protected void fireProcessStopped(final boolean clearListeners) {
    if (log.isDebugEnabled()) log.debug("process [{}:{}] has stopped", getName(), process);
    final ProcessLauncherEvent event = new ProcessLauncherEvent(this);
    for (ProcessLauncherListener listener: listeners) listener.processStopped(event);
    if (clearListeners) listeners.clear();
  }

  /**
   * Send an action command to the slave node.
   * @param action the code of the action command to send.
   */
  public void sendActionCommand(final int action) {
    if (slaveSocketWrapper != null) slaveSocketWrapper.sendActionCommand(action);
  }

  /**
   * Compute the path to the Java executable, based on the JPPF configuration.
   * <p>if the property "{@code jppf.java.path}" is defined, then it will be used,
   * otherwise the value of the system property "java.home" will be used with a "/bin/java" suffix.
   * @param config the configuration to use.
   * @return the full path to the java executable.
   */
  protected String computeJavaExecPath(final TypedProperties config) {
    String path = config.get(JPPFProperties.JAVA_PATH);
    if ((path == null) || path.trim().isEmpty()) path = System.getProperty("java.home") + "/bin/java";
    return path;
  }

  /**
   * Accepts a connection from the child process and sends process commands as int values via the connection.
   * <p>The protocol is very simple: a single int command with no parameter is sent to the child process, which interprets it as it wishes.
   * No acknowledgement or response is expected.
   * @since 5.0
   * @exclude
   */
  protected class SlaveSocketWrapper implements Runnable, AutoCloseable {
    /**
     * Wrapper for the accepted socket.
     */
    private SocketWrapper socketClient = null;

    @Override
    public void run() {
      try {
        socketClient = new BootstrapSocketClient(processServer.accept());
        if (log.isDebugEnabled()) log.debug("initialized {}", socketClient);
        final int n = socketClient.readInt();
        if (n == -1) throw new EOFException();
        if (log.isDebugEnabled()) log.debug("received {}", n);
      } catch(final Exception e) {
        if (log.isDebugEnabled()) log.debug(getName(), e);
        close();
      }
    }

    /**
     * Send an action command to the slave node.
     * @param action the code of the action command to send.
     */
    protected void sendActionCommand(final int action) {
      if (log.isDebugEnabled()) log.debug("{} sending command {} to slave process", getName(), action);
      if (socketClient == null) return;
      try {
        socketClient.writeInt(action);
      } catch (final Exception e) {
        if (log.isDebugEnabled()) log.debug("could not send command to slave process {} : {}", getName(), ExceptionUtils.getStackTrace(e));
      }
    }

    @Override
    public synchronized void close() {
      if (socketClient != null) {
        StreamUtils.closeSilent(socketClient);
        socketClient = null;
      }
    }
  }

  /**
   * @return a reference to the JPPF subprocess, used to kill it when tis launcher exits.
   */
  public Process getProcess() {
    return process;
  }
}
