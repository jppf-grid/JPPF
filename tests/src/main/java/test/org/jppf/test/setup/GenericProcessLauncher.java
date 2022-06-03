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

package test.org.jppf.test.setup;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

import org.jppf.comm.socket.*;
import org.jppf.process.*;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.jppf.utils.streams.StreamUtils;
import org.slf4j.*;

/**
 * Super class for launching a JPPF driver or node.
 * @author Laurent Cohen
 */
public class GenericProcessLauncher extends ThreadSynchronization implements Runnable {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(GenericProcessLauncher.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * System path separator.
   */
  public static final String PATH_SEPARATOR = System.getProperty("path.separator");
  /**
   * Default directory.
   */
  public static final String DEFAULT_DIR = System.getProperty("user.dir");
  /**
   * Maximum time to wait in millis for a driver or node process to be terminated.
   */
  private static final long TERMINATION_TIMEOUT = JPPFConfiguration.getProperties().getLong("jppf.test.process.termination.timeout", 10_000L);
  /**
   * List of files to have in the classpath.
   */
  protected List<String> classpath = new ArrayList<>();
  /**
   * The JVM options to set.
   */
  protected List<String> jvmOptions = new ArrayList<>();
  /**
   * Path to the JPPF configuration file.
   */
  protected String jppfConfig;
  /**
   * The program arguments.
   */
  protected List<String> arguments = new ArrayList<>();
  /**
   * Path to the log4j configuration file.
   */
  protected String log4j;
  /**
   * Path to the JDK logging configuration file.
   */
  protected String logging;
  /**
   * Directory in which the program is started.
   */
  protected String dir = DEFAULT_DIR;
  /**
   * The process started by this process launcher.
   */
  protected Process process;
  /**
   * Wrapper around the process.
   */
  protected ProcessWrapper wrapper;
  /**
   * Fully qualified name of the main class.
   */
  protected String mainClass;
  /**
   * The server socket the driver listens to.
   */
  protected ServerSocket processServer;
  /**
   * The port number the server socket listens to.
   */
  protected int processPort = -1;
  /**
   * The name given to this process launcher.
   */
  protected String name = "";
  /**
   * The driver or node number.
   */
  protected int n;
  /**
   * 
   */
  protected SocketWrapper socketClient;
  /**
   * Used to format timestamps in the std and err outputs.
   */
  protected final SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss.SSS");

  /**
   * Default constructor.
   * @param n a number ssigned to this process.
   * @param processType the type of process (node or driver).
   */
  public GenericProcessLauncher(final int n, final String processType) {
    this.n = n;
    this.name = "[" + processType + '-' + n + "] ";
    addCP("../jmxremote-nio/classes").addCP("../common/classes").addCP("../node/classes");
    addCP("lib/xstream.jar").addCP("lib/xpp3_min.jar").addCP("lib/xmlpull.jar");
    final String libDir = "../JPPF/lib/";
    addCP(libDir + "slf4j/slf4j-api-" + BaseSetup.SLF4J_VERSION + ".jar");
    addCP(libDir + "slf4j/slf4j-log4j12-" + BaseSetup.SLF4J_VERSION + ".jar");
    addCP(libDir + "log4j/log4j-1.2.15.jar");
    addCP(libDir + "LZ4/lz4-java-1.6.0.jar");
    addCP(libDir + "ApacheCommons/commons-io-2.4.jar");
  }

  /**
   * Default constructor.
   * @param n a number ssigned to this process.
   * @param processType the type of process (node or driver).
   * @param config the process configuration.
   * @param bindings variable bindings used in 'expr:' script expressions.
   */
  public GenericProcessLauncher(final int n, final String processType, final TestConfiguration.ProcessConfig config, final Map<String, Object> bindings) {
    this.n = n;
    this.name = "[" + processType + '-' + n + "] ";
    if (debugEnabled) log.debug("creating {} with config=[{}]", name, config);
    bindings.put("$n", n);
    final TypedProperties props = ConfigurationHelper.createConfigFromTemplate(config.jppf, bindings);
    if (debugEnabled) log.debug("{} properties={}", name, props);
    jppfConfig = ConfigurationHelper.createTempConfigFile(props);
    log4j = getFileURL(ConfigurationHelper.createTempConfigFile(ConfigurationHelper.createConfigFromTemplate(config.log4j, bindings)));
    for (final String elt: config.classpath) addClasspathElement(elt);
    for (final String option: config.jvmOptions) addJvmOption(option);
    updateJvmOptionsFromConfig();
    if (debugEnabled) log.debug("{} jppfConfig={}, log4j={}, jvmOptions={}, classpath={}, bindings={}", name, jppfConfig, log4j, jvmOptions, classpath, bindings);
  }
    
  /**
   * 
   */
  private void updateJvmOptionsFromConfig() {
    final TypedProperties config = ConfigurationHelper.loadProperties(new File(jppfConfig));
    final String s = config.get(JPPFProperties.JVM_OPTIONS);
    if (s != null) {
      final String[] options = RegexUtils.SPACES_PATTERN.split(s);
      for (final String opt: options) addJvmOption(opt);
    }
  }

  /**
   * Get the path to the JPPF configuration file.
   * @return the path as a string.
   */
  public String getJppfConfig() {
    return jppfConfig;
  }

  /**
   * Set the path to the JPPF configuration file.
   * @param jppfConfig the path as a string.
   */
  public void setJppfConfig(final String jppfConfig) {
    this.jppfConfig = jppfConfig;
  }

  /**
   * Get the path to the log4j configuration file.
   * @return the path as a string.
   */
  public String getLog4j() {
    return log4j;
  }

  /**
   * Set the path to the log4j configuration file.
   * @param log4j the path as a string.
   */
  public void setLog4j(final String log4j) {
    this.log4j = log4j;
  }

  /**
   * Get the directory in which the program runs.
   * @return the directory as a string.
   */
  public String getDir() {
    return dir;
  }

  /**
   * Set the directory in which the program runs.
   * @param dir the directory as a string.
   */
  public void setDir(final String dir) {
    this.dir = dir;
  }

  /**
   * Get the main class.
   * @return the main class as a string.
   */
  public String getMainClass() {
    return mainClass;
  }

  /**
   * Set the main class.
   * @param mainClass the main class as a string.
   */
  public void setMainClass(final String mainClass) {
    this.mainClass = mainClass;
  }

  /**
   * Add an element (jar or folder) to the classpath.
   * @param element the classpath element to add.
   * @return this launcher, for method call chaining.
   */
  public GenericProcessLauncher addClasspathElement(final String element) {
    classpath.add(element);
    return this;
  }

  /**
   * Add an element (jar or folder) to the classpath.
   * @param element the classpath element to add.
   * @return this launcher, for method call chaining.
   */
  public GenericProcessLauncher addCP(final String element) {
    return addClasspathElement(element);
  }

  /**
   * Add a JVM option (including system property definitions).
   * @param option the option to add.
   */
  public void addJvmOption(final String option) {
    jvmOptions.add(option);
  }

  /**
   * Add a program argument.
   * @param arg the argument to add.
   */
  public void addArgument(final String arg) {
    arguments.add(arg);
  }

  /**
   * Get the path to the JDK logging configuration file.
   * @return the path as a string.
   */
  public String getLogging() {
    return logging;
  }

  /**
   * Set the path to the JDK logging configuration file.
   * @param logging the path as a string.
   */
  public void setLogging(final String logging) {
    this.logging = logging;
  }

  /**
   * Get the name given to this process launcher.
   * @return the name as a string
   */
  public String getName() {
    return name;
  }

  @Override
  public void run() {
    boolean end = false;
    try {
      while (!end) {
        if (debugEnabled) log.debug(name + "starting process");
        startProcess();
        final int exitCode = process.waitFor();
        if (debugEnabled) log.debug(name + "exited with code " + exitCode);
        end = onProcessExit(exitCode);
      }
    } catch (final Exception e) {
      e.printStackTrace();
    } catch (final Error e) {
      e.printStackTrace();
    }
    if (process != null) process.destroy();
  }

  /**
   * Called when the subprocess has exited with exit value n.
   * This allows for printing the residual output (both standard and error) to this pJVM's console and log file,
   * in order to get additional information if a problem occurred.
   * @param exitCode the exit value of the subprocess.
   * @return true if this launcher is to be terminated, false if it should re-launch the subprocess.
   */
  private static boolean onProcessExit(final int exitCode) {
    return exitCode != 2;
  }

  /**
   * Start the process.
   * @throws IOException if the process fails to start.
   */
  public void startProcess() throws IOException {
    final int processPort = startDriverSocket();
    if (debugEnabled) log.debug("process port = {}", processPort);
    final List<String> command = new ArrayList<>();
    command.add(System.getProperty("java.home")+"/bin/java");
    command.add("-cp");
    final StringBuilder sb = new StringBuilder();
    for (int i=0; i<classpath.size(); i++) {
      if (i > 0) sb.append(PATH_SEPARATOR);
      sb.append(classpath.get(i));
    }
    command.add(sb.toString());
    command.addAll(jvmOptions);
    command.add("-Djppf.config=" + jppfConfig);
    command.add("-Dlog4j.configuration=" + log4j);
    if (logging != null) command.add("-Djava.util.logging.config.file=" + logging);
    command.add(mainClass);
    if (processPort > 0) command.add(Integer.toString(processPort));
    else command.add("noLauncher");
    final ProcessBuilder builder = new ProcessBuilder();
    builder.command(command);
    if (dir != null) builder.directory(new File(dir));
    wrapper = new ProcessWrapper();
    wrapper.setName(name + "-pw");
    wrapper.addListener(new ProcessWrapperEventListener() {
      @Override
      public void outputStreamAltered(final ProcessWrapperEvent event) {
        printStreamEvent(System.out, event);
      }
      @Override
      public void errorStreamAltered(final ProcessWrapperEvent event) {
        printStreamEvent(System.err, event);
      }
    });
    wrapper.setProcess(process = builder.start());
    if (debugEnabled) log.debug(name + "starting process " + process);
  }

  /**
   * 
   * @param stream the stream to print to.
   * @param event the event that holds the content to print.
   */
  private void printStreamEvent(final PrintStream stream, final ProcessWrapperEvent event) {
    synchronized(stream) {
      if (stream instanceof TestPrintStream) ((TestPrintStream) stream).printNoHeader(TestPrintStream.getHeader(name) + event.getContent());
      else stream.print(event.getContent());
    }
  }

  /**
   * Stop the process.
   */
  public void stopProcess() {
    if ((wrapper != null) && (wrapper.getProcess() != null)) {
      final Process process = wrapper.getProcess();
      if (debugEnabled) log.debug(name + "stopping process " + process);
      if (socketClient != null) {
        try {
            socketClient.close();
          } catch (@SuppressWarnings("unused") final Exception ignore) {
          }
      }
      //process.destroy();
      boolean terminated = false;
      final long start = System.nanoTime();
      while (!terminated && ((System.nanoTime() - start) / 1_000_000L < TERMINATION_TIMEOUT)) {
        try {
          process.exitValue();
          terminated = true;
        } catch (@SuppressWarnings("unused") final Exception ignore) {
          goToSleep(50L);
        }
      }
      if (!terminated) log.warn(String.format("%s did not terminate in the %,d ms timeout. Call stack:%n%s", name, TERMINATION_TIMEOUT, ExceptionUtils.getCallStack()));
    }
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
  protected int startDriverSocket() {
    try {
      if (processServer != null) {
        StreamUtils.closeSilent(processServer);
        processServer = null;
      }
      if (socketClient != null) StreamUtils.closeSilent(socketClient);
      processServer = new ServerSocket(0);
      processPort = processServer.getLocalPort();
      /** */
      class MyRunnable extends ThreadSynchronization implements Runnable {
        /** */
        private boolean started = false;

        @Override
        public void run() {
          try {
            synchronized(this) {
              started = true;
              wakeUp();
            }
            socketClient = new SocketClient(processServer.accept());
            final int n = socketClient.readInt();
            if (n == -1) throw new EOFException();
          } catch(final Exception e) {
            if (debugEnabled) log.debug(name, e);
          }
        }

        /** */
        public synchronized void await() {
          if (started) return;
          goToSleep();
        }
      };
      final MyRunnable r = new MyRunnable();
      ThreadUtils.startDaemonThread(r, name + "ServerSocket");
      r.await();
    } catch(@SuppressWarnings("unused") final Exception e) {
      if (processServer != null) {
        StreamUtils.closeSilent(processServer);
        processServer = null;
      }
    }
    return processPort;
  }

  /**
   * Get a url from a file path.
   * @param path the path to convert to a url.
   * @return a urm as a string.
   */
  public static String getFileURL(final String path) {
    URL url = null;
    try {
      url = new File(path).toURI().toURL();
    } catch (final MalformedURLException e) {
      throw new RuntimeException(e);
    }
    return url.toString();
  }

  /**
   * Return a formatted time stamp.
   * @return the current timestamp formatted as a string.
   */
  protected String formatPrologue() {
    final StringBuilder sb = new StringBuilder(name).append('[');
    synchronized(sdf) {
      return sb.append(sdf.format(new Date())).append("] ").toString();
    }
  }
}
