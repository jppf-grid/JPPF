/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

import java.io.File;

import org.jppf.utils.TypedProperties;

/**
 * 
 * @author Laurent Cohen
 */
public interface ScenarioConfiguration
{
  /**
   * Path to where the common (default) config files are located.
   */
  String TEMPLATES_DIR = "scenarios/templates";

  /**
   * The name of the file containing the configuration of the scenario.
   */
  String SCENARIO_FILE = "scenario.properties";

  /**
   * Get the path to the scenario directory, where the config files are.
   * @return the path as a <code>File</code> object.
   */
  File getConfigDir();

  /**
   * Get the name of the scenario.
   * @return the scenario name.
   */
  String getName();

  /**
   * Get the description of the scenario.
   * @return the scenario description.
   */
  String getDescription();

  /**
   * Get the number of drivers.
   * @return the number of drivers as an int.
   */
  int getNbDrivers();

  /**
   * Get the number of nodes.
   * @return the number of nodes as an int.
   */
  int getNbNodes();

  /**
   * Get the number of iterations of the scenario.
   * @return the number of iterations as an int.
   */
  int getNbIterations();

  /**
   * Get the raw properties for this configuration.
   * @return an instance of {@link TypedProperties}.
   */
  TypedProperties getProperties();

  /**
   * Get the fully qualified name of the sceario runner class for this scenaerio.
   * @return the class name as a string.
   */
  String getRunnerClassName();

  /**
   * Get the name of the while where the diagnostics post scenario run will be written.
   * <p>The vlaue for this property can be either:
   * <ul>
   * <li>out - the diagnostics will be printed to <code>System.out</code></li>
   * <li>err - the diagnostics will be printed to <code>System.err</code></li>
   * <li>a valid file path</code></li>
   * <li></li>
   * <li></li>
   * </ul>
   * @return the diagnostics out file name.
   */
  String getDiagnosticsOutputFilename();

  /**
   * Get the name of the file to which the processes' stdout is redirected.
   * @return 'out', 'err' or a valid file path.
   */
  String getStdoutFilename();

  /**
   * Get the name of the file to which the processes' stderr is redirected.
   * @return 'out', 'err' or a valid file path.
   */
  String getStderrFilename();
}
