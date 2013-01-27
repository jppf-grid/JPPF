/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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
import java.util.Collections;

import org.jppf.utils.TypedProperties;

import test.org.jppf.test.setup.ConfigurationHelper;

/**
 * 
 * @author Laurent Cohen
 */
public class ScenarioConfigurationImpl extends TypedProperties implements ScenarioConfiguration
{
  /**
   * The configuration directory.
   */
  private final File configDir;

  /**
   * Initialize this confiugration from the specified diretory.
   * @param configDir the configuration directory.
   */
  public ScenarioConfigurationImpl(final File configDir)
  {
    super(ConfigurationHelper.createConfigFromTemplate(new File(configDir, SCENARIO_FILE).getPath(), Collections.<String, Object>emptyMap()));
    this.configDir = configDir;
    //ConfigurationHelper.loadProperties(this, new File(configDir, SCENARIO_FILE));
  }

  @Override
  public File getConfigDir()
  {
    return configDir;
  }

  @Override
  public String getName()
  {
    return getString("jppf.scenario.name");
  }

  @Override
  public String getDescription()
  {
    return getString("jppf.scenario.description");
  }

  @Override
  public int getNbDrivers()
  {
    return getInt("jppf.scenario.nbDrivers", 1);
  }

  @Override
  public int getNbNodes()
  {
    return getInt("jppf.scenario.nbNodes", 1);
  }

  @Override
  public TypedProperties getProperties()
  {
    return this;
  }

  @Override
  public String getRunnerClassName()
  {
    return getString("jppf.scenario.runner.class");
  }

  @Override
  public String getDiagnosticsOutputFilename()
  {
    return getString("jppf.scenario.diagnostics.output.file", "out");
  }
}
