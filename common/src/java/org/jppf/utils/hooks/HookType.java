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

package org.jppf.utils.hooks;

/**
 * This enum defines the possible types of hooks that can be defined.
 * @author Laurent Cohen
 */
public enum HookType
{
  /**
   * A single instance of the hook is discovered and created, based on a configurtion property.
   */
  CONFIG_SINGLE_INSTANCE,
  /**
   * A single instance of the hook is discovered and created using the SPI mechanism.
   */
  SPI_SINGLE_INSTANCE,
  /**
   * Multiple instances of the hook are discovered and created using the SPI mechanism.
   */
  SPI_MULTIPLE_INSTANCES
}
