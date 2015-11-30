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

package org.jppf.test.scenario;

import org.jppf.test.setup.Setup;

/**
 * 
 * @author Laurent Cohen
 */
public interface ScenarioRunner extends Runnable
{
  /**
   * Get the TestSetup which manages the drivers, nodes and client pprocesses.
   * @return a {@link Setup} instance.
   */
  Setup getSetup();

  /**
   * Set the <code>TestSetup</code> which manages the drivers, nodes and client pprocesses.
   * @param testSetup a {@link Setup} instance.
   */
  void setSetup(Setup testSetup);

  /**
   * Get the configuration of the scenario.
   * @return a {@link ScenarioConfiguration} instance.
   */
  ScenarioConfiguration getConfiguration();

  /**
   * Set the configuration of the scenario.
   * @param config a {@link ScenarioConfiguration} instance.
   */
  void setConfiguration(ScenarioConfiguration config);

  /**
   * Release the resources used by this runner.
   */
  void dispose();
}
