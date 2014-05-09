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

package test.org.jppf.test.setup;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.comm.socket.*;
import org.jppf.process.ProcessWrapper;
import org.jppf.process.event.*;
import org.jppf.utils.TypedProperties;
import org.jppf.utils.streams.StreamUtils;
import org.slf4j.*;

/**
 * Super class for launching a JPPF driver or node.
 * @author Laurent Cohen
 */
public class GenericProcessLauncher implements Runnable {
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
  protected String jppfConfig = null;
  /**
   * The program arguments.
   */
  protected List<String> arguments = new ArrayList<>();
  /**
   * Path to the log4j configuration file.
   */
  protected String log4j = null;
  /**
   * Path to the JDK logging configuration file.
   */
  protected String logging = null;
  /**
   * Directory in which the program is started.
   */
  protected String dir = DEFAULT_DIR;
  /**
   * The process started by this process launcher.
   */
  protected Process process = null;
  /**
   * Wrapper around the process.
   */
  protected ProcessWrapper wrapper = null;
  /**
   * Fully qualified name of the main class.
   */
  protected String mainClass = null;
  /**
   * The server socket the driver listens to.
   */
  protected ServerSocket processServer = null;
  /**
   * The port number the server socket listens to.
   */
  protected int processPort = -1;
  /**
   * The name given to this process launcher.
   */
  protected String name = "";
  /**
   * The driver or node number
   */
  protected int n = 0;
  /**
   * The output stream where the stdout of the started process is to be redirected.
   */
  protected static PrintStream stdout = System.out;
  /**
   * The output stream where the stdout of the started process is to be redirected.
   */
  protected static PrintStream stderr = System.out;
  /**
   * 
   */
  protected static AtomicBoolean streamsConfigured = new AtomicBoolean(false);
  /**
   * 
   */
  protected SocketWrapper socketClient = null;
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
    addClasspathElement("../node/classes");
    String libDir = "../JPPF/lib/";
    addClasspathElement(libDir + "slf4j/slf4j-api-1.6.1.jar");
    addClasspathElement(libDir + "slf4j/slf4j-log4j12-1.6.1.jar");
    addClasspathElement(libDir + "log4j/log4j-1.2.15.jar");
    addClasspathElement(libDir + "jmxremote/" + BaseSetup.JMX_REMOTE_JAR);
  }

  /**
   * Default constructor.
   * @param n a number ssigned to this process.
   * @param processType the type of process (node or driver).
   * @param jppfTemplate the path to the JPPF configuration template file.
   * @param log4jTemplate the path to the log4j template file.
   */
  public GenericProcessLauncher(final int n, final String processType, final String jppfTemplate, final String log4jTemplate) {
    this.n = n;
    this.name = "[" + processType + '-' + n + "] ";
    addClasspathElement("../node/classes");
    String libDir = "../JPPF/lib/";
    jppfConfig = ConfigurationHelper.createTempConfigFile(ConfigurationHelper.createConfigFromTemplate(jppfTemplate, n));
    log4j = getFileURL(ConfigurationHelper.createTempConfigFile(ConfigurationHelper.createConfigFromTemplate(log4jTemplate, n)));
    addClasspathElement(libDir + "slf4j/slf4j-api-1.6.1.jar");
    addClasspathElement(libDir + "slf4j/slf4j-log4j12-1.6.1.jar");
    addClasspathElement(libDir + "log4j/log4j-1.2.15.jar");
    addClasspathElement(libDir + "jmxremote/" + BaseSetup.JMX_REMOTE_JAR);
    updateJvmOptionsFromConfig();
  }

  /**
   * Default constructor.
   * @param n a number ssigned to this process.
   * @param processType the type of process (node or driver).
   * @param jppfTemplate the path to the JPPF configuration template file.
   * @param log4jTemplate the path to the log4j template file.
   * @param classpath the classpath elements for the driver.
   * @param jvmOptions additional JVM options for the driver.
   */
  public GenericProcessLauncher(final int n, final String processType, final String jppfTemplate, final String log4jTemplate, final List<String> classpath, final List<String> jvmOptions) {
    this.n = n;
    this.name = "[" + processType + '-' + n + "] ";
    jppfConfig = ConfigurationHelper.createTempConfigFile(ConfigurationHelper.createConfigFromTemplate(jppfTemplate, n));
    log4j = getFileURL(ConfigurationHelper.createTempConfigFile(ConfigurationHelper.createConfigFromTemplate(log4jTemplate, n)));
    for (String elt: classpath) addClasspathElement(elt);
    for (String option: jvmOptions) addJvmOption(option);
    updateJvmOptionsFromConfig();
  }

  /**
   * 
   */
  private void updateJvmOptionsFromConfig() {
    TypedProperties config = ConfigurationHelper.loadProperties(new File(jppfConfig));
    String s = config.getString("jppf.jvm.options", null);
    if (s != null) {
      String[] options = s.split("\\s");
      for (String opt: options) addJvmOption(opt);
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
   */
  public void addClasspathElement(final String element) {
    classpath.add(element);
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

  @Override
  public void run() {
    boolean end = false;
    try {
      while (!end) {
        if (debugEnabled) log.debug(name + "starting process");
        startProcess();
        int exitCode = process.waitFor();
        if (debugEnabled) log.debug(name + "exited with code " + exitCode);
        end = onProcessExit(exitCode);
      }
    } catch (Exception e) {
      e.printStackTrace();
    } catch (Error e) {
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
  private boolean onProcessExit(final int exitCode) {
    return exitCode != 2;
  }

  /**
   * Start the process.
   * @throws IOException if the process fails to start.
   */
  public void startProcess() throws IOException {
    startDriverSocket();
    List<String> command = new ArrayList<>();
    command.add(System.getProperty("java.home")+"/bin/java");
    command.add("-cp");
    StringBuilder sb = new StringBuilder();
    for (int i=0; i<classpath.size(); i++) {
      if (i > 0) sb.append(PATH_SEPARATOR);
      sb.append(classpath.get(i));
    }
    command.add(sb.toString());
    command.addAll(jvmOptions);
    command.add("-Djppf.config=" + jppfConfig);
    command.add("-Dlog4j.configuration=" + log4j);
    if (logging != null) command.add("-Djava.util.logging.config.file=" + logging);
    //command.add("-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Log4JLogger");
    command.add(mainClass);
    if (processPort > 0) command.add(Integer.toString(processPort));
    else command.add("noLauncher");
    ProcessBuilder builder = new ProcessBuilder();
    builder.command(command);
    if (dir != null) builder.directory(new File(dir));
    wrapper = new ProcessWrapper();
    wrapper.addListener(new ProcessWrapperEventListener() {
      @Override
      public void outputStreamAltered(final ProcessWrapperEvent event) {
        if (stdout != null) stdout.print(formatPrologue() + event.getContent());
      }
      @Override
      public void errorStreamAltered(final ProcessWrapperEvent event) {
        if (stderr != null) stderr.print(formatPrologue() + event.getContent());
      }
    });
    wrapper.setProcess(builder.start());
    process = wrapper.getProcess();
    if (debugEnabled) log.debug(name + "starting process " + process);
  }

  /**
   * Stop the process.
   */
  public void stopProcess() {
    if ((wrapper != null) && (wrapper.getProcess() != null)) {
      Process process = wrapper.getProcess();
      if (debugEnabled) log.debug(name + "stopping process " + process);
      if (socketClient != null) {
        try {
            socketClient.close();
          } catch (Exception ignore) {
          }
      }
      synchronized(this) {
        try {
          wait(100L);
        } catch (Exception e) {
        }
      }
      process.destroy();
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
      if (processServer == null) processServer = new ServerSocket(0);
      processPort = processServer.getLocalPort();
      Runnable r = new Runnable() {
        @Override
        public void run() {
          try {
            socketClient = new SocketClient(processServer.accept());
            int n = socketClient.readInt();
            if (n == -1) throw new EOFException();
          } catch(Exception ioe) {
            if (debugEnabled) log.debug(name, ioe);
            if (socketClient != null) StreamUtils.closeSilent(socketClient);
          }
        }
      };
      Thread thread = new Thread(r, name + "ServerSocket");
      thread.setDaemon(true);
      thread.start();
    } catch(Exception e) {
      if (processServer != null) StreamUtils.closeSilent(processServer);
      processServer = null;
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
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
    return url.toString();
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

  /**
   * Return a formatted time stamp.
   * @return the current timestamp formatted as a string.
   */
  protected String formatPrologue() {
    StringBuilder sb = new StringBuilder(name).append('[');
    synchronized(sdf) {
      return sb.append(sdf.format(new Date())).append("] ").toString();
    }
  }
}
