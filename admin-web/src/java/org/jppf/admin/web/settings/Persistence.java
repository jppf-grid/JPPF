/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.admin.web.settings;

import org.jppf.utils.TypedProperties;

/**
 * 
 * @author Laurent Cohen
 */
public interface Persistence {
  /**
   * Load the settings for the specified user.
   * @param name the name fo the file to load from.
   * @return the settings propeties loadded from the persistent store.
   * @throws Exception if any error occurs.
   */
  public TypedProperties loadProperties(String name) throws Exception;

  /**
   * Save the settings for the specified user.
   * @param name the name of the file to save to.
   * @param settings the settings to save.
   * @throws Exception if any error occurs.
   */
  public void saveProperties(String name, TypedProperties settings) throws Exception;

  /**
   * Load the settings for the specified user.
   * @param name the name fo the file to load from.
   * @return the settings propeties loadded from the persistent store.
   * @throws Exception if any error occurs.
   */
  public String loadString(String name) throws Exception;

  /**
   * Save the settings for the specified user.
   * @param name the name of the file to save to.
   * @param settings the settings to save.
   * @throws Exception if any error occurs.
   */
  public void saveString(String name, String settings) throws Exception;
}
