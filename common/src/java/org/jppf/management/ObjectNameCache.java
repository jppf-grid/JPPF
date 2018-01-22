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

package org.jppf.management;

import java.util.Map;

import javax.management.*;

import org.jppf.utils.collections.SoftReferenceValuesMap;

/**
 * A soft references cache of {@link ObjectName} instances looked up with a single string name.
 * <p>The rationale for this cache is that, based on profiling data, the construction of an
 * {@link ObjectName} is computationally costly and will gain from caching.
 * @author Laurent Cohen
 * @exclude
 * @since 6.0
 */
public final class ObjectNameCache {
  /**
   * The actual cache.
   */
  private static final Map<String, ObjectName> objectNameMap = new SoftReferenceValuesMap<>();

  /**
   * Get an object name for the specified name string, creating it if necessary.
   * @param name the name to lookup.
   * @return an {@link ObjectName} initialized with the specified string.
   * @throws MalformedObjectNameException if the provided name is malformed.
   */
  public static ObjectName getObjectName(final String name) throws MalformedObjectNameException {
    ObjectName result;
    synchronized(objectNameMap) {
      result = objectNameMap.get(name);
      if (result == null) objectNameMap.put(name, result = new ObjectName(name));
    }
    return result;
  }
}
