/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

package org.jppf.test.scenario;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

import org.jppf.management.diagnostics.*;
import org.jppf.test.setup.*;
import org.jppf.utils.*;
import org.jppf.utils.streams.StreamUtils;

import test.org.jppf.test.setup.ConfigurationHelper;

/**
 * 
 * @author Laurent Cohen
 */
public class Scenario {
  /**
   * Path to where this scenario's config files are located.
   */
  private final File configDir;
  /**
   * The configuration for this scenario.
   */
  private final ScenarioConfiguration configuration;
  /**
   * The setup of nodes, drivers and client.
   */
  private Setup setup;

  /**
   * Execute the scenario whose root folder is specified as argument.
   * @param args the first argument contains the scenario's toor folder path.
   */
  public static void main(final String[] args) {
    try {
      final Scenario scenario = new Scenario(args[0]);
      scenario.execute();
    } catch (final Throwable e) {
      e.printStackTrace();
    }
  }

  /**
   * Initialize this scenario with the specified config directory.
   * @param configDir path to where this scenario's config files are located.
   */
  public Scenario(final String configDir) {
    if (configDir == null) throw new IllegalArgumentException("config directory cannot be null");
    final File file = new File(configDir);
    if (!file.exists()) throw new IllegalArgumentException("config directory '" + configDir + "' does not exist");
    if (!file.isDirectory()) throw new IllegalArgumentException("'" + configDir + "' is not a directory");
    this.configDir = file;
    configuration = new ScenarioConfigurationImpl(this.configDir);
  }

  /**
   * Execute this scenario.
   * @throws Exception if any error occurs.
   */
  public void execute() throws Exception {
    final int iterations = configuration.getNbIterations();
    for (int i=1; i<=iterations; i++) {
      if (iterations > 1) {
        final String hr = StringUtils.padRight("", '-', 15);
        System.out.println(hr);
        System.out.println("Iteration #" + i);
        System.out.println(hr);
      }
      executeIteration();
    }
  }

  /**
   * Execute one iteration of this scenario.
   * @throws Exception if any error occurs.
   */
  public void executeIteration() throws Exception {
    try {
      final Map<String, Object> variables = new HashMap<>();
      variables.put("$n", 1);
      variables.put("$scenario_dir", configDir.getAbsolutePath().replace("\\", "/"));
      variables.put("$templates_dir", ScenarioConfiguration.TEMPLATES_DIR);
      final String jppf = doConfigOverride("client.template.properties", "client.properties", variables);
      System.setProperty("jppf.config", jppf);
      final String log4j = doConfigOverride("log4j-client.template.properties", "log4j-client.properties", variables);
      final URL url = new File(log4j).toURI().toURL();
      System.out.println("Log4j url = " + url);
      // use reflection to avoid compile-time dependency on log4j lib.
      final Class<?> configuratorClass = Class.forName("org.apache.log4j.PropertyConfigurator");
      final Method method = configuratorClass.getMethod("configure", URL.class);
      method.invoke(null, url);
      //PropertyConfigurator.configure(url);
      System.setProperty("log4j.configuration", url.toString());
      final String logging = doConfigOverride("logging-client.template.properties", "logging-client.properties", variables);
      System.setProperty("java.util.logging.config.file", logging);
      setup = new Setup(configuration);
      final String className = configuration.getRunnerClassName();
      final Class<?> clazz = Class.forName(className);
      final ScenarioRunner runner = (ScenarioRunner) clazz.newInstance();
      setup.setup(configuration.getNbDrivers(), configuration.getNbNodes());
      runner.setSetup(setup);
      runner.setConfiguration(configuration);
      runner.run();
    } finally {
      try {
        printDiagnostics();
      } finally {
        if (setup != null) setup.cleanup();
      }
    }
  }

  /**
   * Display the diagnostics for all drivers and nodes.
   * @throws Exception if any error occurs.
   */
  private void printDiagnostics() throws Exception {
    if (!configuration.isStartClient()) return;
    final String fileName = configuration.getDiagnosticsOutputFilename();
    if ("none".equals(fileName)) return;
    PrintStream out = null;
    if ("out".equals(fileName)) out = System.out;
    else if ("err".equals(fileName)) out = System.err;
    else out = new PrintStream(new BufferedOutputStream(new FileOutputStream(fileName)));
    try {
      final Map<JMXResult<DiagnosticsResult>, List<JMXResult<DiagnosticsResult>>> map =
        setup.getJmxHandler().performJmxOperations(new DiagnosticsGrabber(true), new DiagnosticsGrabber(false));
      final String rule = "---------------------------------------------------------";
      for (final Map.Entry<JMXResult<DiagnosticsResult>, List<JMXResult<DiagnosticsResult>>> entry: map.entrySet()) {
        out.println(rule);
        out.println("results for driver " + entry.getKey().getJmxId());
        out.println(rule);
        out.println("before GC: " + entry.getKey().getResult().getDiagnosticsInfo());
        out.println("after GC:  " + entry.getKey().getResult().getDiagnosticsInfoAfterGC());
        for (final JMXResult<DiagnosticsResult> dr: entry.getValue()) {
          out.println(rule);
          out.println("results for node " + dr.getJmxId());
          out.println("before GC: " + dr.getResult().getDiagnosticsInfo());
          out.println("after GC:  " + dr.getResult().getDiagnosticsInfoAfterGC());
        }
      }
    } finally {
      if ((out != null) && (out != System.err) && (out != System.out)) StreamUtils.closeSilent(out);
    }
  }

  /**
   * Get the configuration for this scenario.
   * @return a {@link ScenarioConfiguration} instance.
   */
  public ScenarioConfiguration getConfiguration() {
    return configuration;
  }

  /**
   * Create a config file from the template and override its entries with those in the specified override file.
   * @param template the name of the template file.
   * @param override the name of the override file.
   * @param variables a map of variable names to their value, which can be used in a groovy expression.
   * @return the path of the created file.
   */
  protected String doConfigOverride(final String template, final String override, final Map<String, Object> variables) {
    File templateFile = new File(configuration.getConfigDir(), template);
    if (!templateFile.exists()) templateFile = new File(ScenarioConfiguration.TEMPLATES_DIR, template);
    final TypedProperties config = ConfigurationHelper.createConfigFromTemplate(templateFile.getAbsolutePath().replace("\\", "/"), variables);
    final File overrideFile = new File(configuration.getConfigDir(), override);
    if (overrideFile.exists()) ConfigurationHelper.overrideConfig(config, overrideFile);
    final String path = ConfigurationHelper.createTempConfigFile(config);
    return path;
  }

  /**
   * Instances of this class get the diagnostics information for a driver or a node.
   */
  private class DiagnosticsGrabber extends JmxAwareCallable<DiagnosticsResult> {
    /**
     * <code>true</code> if this object connects to a driver's JMX, <code>false</code> for a node.
     */
    private final boolean driver;

    /**
     * Initialize this object with the psecified type of remote JMX server.
     * @param driver <code>true</code> if this object connects to a driver's JMX, <code>false</code> for a node.
     */
    public DiagnosticsGrabber(final boolean driver) {
      this.driver = driver;
    }

    @Override
    public JMXResult<DiagnosticsResult> call() throws Exception {
      final String name = driver ? DiagnosticsMBean.MBEAN_NAME_DRIVER : DiagnosticsMBean.MBEAN_NAME_NODE;
      final HealthSnapshot info = (HealthSnapshot) getJmx().invoke(name, "healthSnapshot");
      getJmx().invoke(name, "gc", (Object[]) null, (String[]) null);
      final HealthSnapshot info2 = (HealthSnapshot) getJmx().invoke(name, "healthSnapshot");
      return new JMXResult<>(getJmx().getURL().toString(), new DiagnosticsResult(info, info2));
    }
  }
}
