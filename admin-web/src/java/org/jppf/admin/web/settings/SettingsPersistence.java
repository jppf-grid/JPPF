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
public interface SettingsPersistence {
  /**
   * Load the settings for the specified user.
   * @param userHash a hash of the user name.
   * @param settings the settings to load into.
   * @throws Exception if any error occurs.
   */
  public void load(String userHash, TypedProperties settings) throws Exception;

  /**
   * Save the settings for the specified user.
   * @param userHash a hash of the user name.
   * @param settings the settings to save.
   * @throws Exception if any error occurs.
   */
  public void save(String userHash, TypedProperties settings) throws Exception;
}
