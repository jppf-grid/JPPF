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

package org.jppf.test.setup;

import java.io.*;
import java.util.*;

import org.jppf.test.scenario.ScenarioConfiguration;
import org.jppf.utils.*;
import org.slf4j.*;

import test.org.jppf.test.setup.*;

/**
 * 
 * @author Laurent Cohen
 */
public class RestartableProcessLauncher extends GenericProcessLauncher {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(RestartableProcessLauncher.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * The scenario configuration.
   */
  private final ScenarioConfiguration config;
  /**
   * Caching of the created config files.
   */
  private List<String> tempFileCache = new ArrayList<>();
  /**
   * A map of variable names to their value, which can be used in a groovy expression.
   */
  protected final Map<String, Object> variables = new HashMap<>();

  /**
   * Default constructor.
   * @param n a number assigned to this process.
   * @param processType the type of process (node or driver).
   * @param config the scenario configuration.
   */
  public RestartableProcessLauncher(final int n, final String processType, final ScenarioConfiguration config) {
    super(n, processType);
    this.config = config;
    variables.put("$n", n);
    variables.put("$scenario_dir", config.getConfigDir().getPath());
    variables.put("$templates_dir", ScenarioConfiguration.TEMPLATES_DIR);
    variables.put("$nbDrivers", config.getNbDrivers());
    variables.put("$nbNodes", config.getNbNodes());
    if (streamsConfigured.compareAndSet(false, true)) {
      stdout = configureOutput(config.getStdoutFilename());
      stderr = configureOutput(config.getStderrFilename());
    }
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
        //if (process != null) process.destroy();
      }
    } catch (Exception|Error e) {
      e.printStackTrace();
    }
    //System.exit(0);
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
   * Create a config file from the template and override its entries with those in the specified override file.
   * @param template the name of the template file.
   * @param override the name of the override file.
   * @return the path of the created file.
   */
  protected String doConfigOverride(final String template, final String override) {
    File templateFile = new File(config.getConfigDir(), template);
    if (!templateFile.exists()) templateFile = new File(ScenarioConfiguration.TEMPLATES_DIR, template);
    File overrideFile = new File(config.getConfigDir(), override);
    TypedProperties config = ConfigurationHelper.createConfigFromTemplate(templateFile.getPath(), variables);
    if (overrideFile.exists()) ConfigurationHelper.overrideConfig(config, overrideFile);
    String path = ConfigurationHelper.createTempConfigFile(config);
    tempFileCache.add(path);
    return path;
  }

  @Override
  public void stopProcess() {
    super.stopProcess();
    for (String path: tempFileCache) {
      File file = new File(path);
      if (file.exists()) {
        if (!file.delete() && debugEnabled) log.debug("could not delete file '" + file + '\'');
      }
    }
    tempFileCache.clear();
  }

  /**
   * 
   */
  protected void setJVMOptions() {
    TypedProperties props = ConfigurationHelper.loadProperties(new File(jppfConfig));
    String opts = props.getString("jppf.jvm.options");
    if ((opts != null) && !"".equals(opts.trim())) {
      String[] options = opts.split("\\s");
      for (int i=0; i<options.length; i++) {
        if ("-cp".equals(options[i]) || "-classpath".equals(options[i])) addClasspathElement(options[++i]);
        else jvmOptions.add(options[i]);
      }
    }
  }

  /**
   * Create a print stream from a specified name or path.
   * @param outputName the path to create the stream from.
   * @return a <code>PrintStream</code> instance.
   */
  private PrintStream configureOutput(final String outputName) {
    PrintStream result = null;
    try {
      if ((outputName == null) || "out".equalsIgnoreCase(outputName)) result = System.out;
      else if ("err".equalsIgnoreCase(outputName)) result = System.err;
      else {
        File file = new File(outputName);
        result = new PrintStream(new FileOutputStream(file));
      }
    } catch (Exception e) {
    }
    return result;
  }
}
