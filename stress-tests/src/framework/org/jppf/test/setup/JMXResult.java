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

package org.jppf.test.setup;

import org.jppf.utils.Pair;

/**
 * Result generated when querying nodes and drivers for diagnostics via JMX.
 * @param <V> the type of results.
 * @author Laurent Cohen
 */
public class JMXResult<V> extends Pair<String, V>
{
  /**
   * Intiialize this result with the specified jmx id and diagnostics information.
   * @param jmxId the id of the jmx connection wrapper.
   * @param result the diagnostics information.
   */
  public JMXResult(final String jmxId, final V result)
  {
    super(jmxId, result);
  }

  /**
   * Get the id of the jmx connection wrapper.
   * @return the id as a string.
   */
  public String getJmxId()
  {
    return first();
  }

  /**
   * Get the diagnostics information.
   * @return a {@link org.jppf.management.diagnostics.MemoryInformation MemoryInformation} instance.
   */
  public V getResult()
  {
    return second();
  }
}
