/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.utils.configuration;

import java.util.*;

import org.jppf.utils.TypedProperties;
import org.jppf.utils.collections.*;

/**
 * Handles property substitutions in a properties file, that is resolve all
 * references of the following form in properties values:
 * <pre>property.name = ${other.property.name}</pre>
 * @author Laurent Cohen
 */
public class SubstitutionsHandler {
  /**
   * Start of a value substitution.
   */
  private static final String SUBST_START ="${";
  /**
   * Stop of a value substitution.
   */
  private static final String SUBST_STOP = "}";
  /**
   * Prefix for references to environment variables.
   */
  private static final String ENV_PREFIX ="env.";
  /**
   * The properties object in which to resolve substitutions.
   */
  private final TypedProperties props;
  /**
   * Mapping of properties whose resolution other properties depend on.
   */
  private final CollectionMap<String, String> dependedOnMap = new SetHashMap<>();
  /**
   * Mapping of properties to the properties they depend on.
   */
  private final CollectionMap<String, String> dependenciesMap = new SetHashMap<>();
  /**
   * Stores the properties whose values are fully resolved.
   */
  private TypedProperties resolved = new TypedProperties();

  /**
   * Initialize this substitution handler with the specified unresolved properties.
   * @param props the properties where substitutions must be resolved.
   */
  public SubstitutionsHandler(final TypedProperties props) {
    this.props = props;
  }

  /**
   * Resolve the substitutions.
   * @return TypedProperties object in which substitutions have been resolved whenever possible.
   */
  public TypedProperties resolve() {
    for (Map.Entry<Object, Object> entry: props.entrySet()) {
      if (!(entry.getKey() instanceof String) || !(entry.getValue() instanceof String)) continue;
      String key = (String) entry.getKey();
      String value = (String) entry.getValue();
      if (value == null) continue;
      boolean found = true;
      int pos = 0;
      while (found) {
        int idx1 = value.indexOf(SUBST_START, pos);
        if (idx1 < 0) break;
        pos = idx1 + SUBST_START.length();
        int idx2 = value.indexOf(SUBST_STOP, pos);
        if (idx2 < 0) break;
        String name = value.substring(pos, idx2);
        if (name.startsWith(ENV_PREFIX)) {
          String envVar = name.substring(ENV_PREFIX.length());
          String resolvedValue = System.getenv(envVar);
          if (resolvedValue == null) resolvedValue = "";
          value = value.replace(SUBST_START + name + SUBST_STOP, resolvedValue);
        } else dependenciesMap.putValue(key, name);
        pos = idx2 + SUBST_STOP.length() + 1;
      }
      if (dependenciesMap.containsKey(key)) {
        Set<String> toRemove = new HashSet<>();
        for (String dep: dependenciesMap.getValues(key)) {
          if (resolved.containsKey(dep)) {
            String resolvedValue = resolved.getProperty(dep);
            value = value.replace(SUBST_START + dep + SUBST_STOP, resolvedValue);
            toRemove.add(dep);
          }
        }
        for (String dep: toRemove) dependenciesMap.removeValue(key, dep);
      }
      if (dependenciesMap.containsKey(key)) {
        for (String dep: dependenciesMap.getValues(key)) dependedOnMap.putValue(dep, key);
      } else {
        resolved.setProperty(key, value);
        propagateResolution(key, value);
      }
    }
    // add the unresolved properties
    for (String unresolvedProp: dependenciesMap.keySet()) resolved.put(unresolvedProp, props.getProperty(unresolvedProp));
    return resolved;
  }

  /**
   * Recursively propagate the resolution of a property value to the properties that depend on it.
   * @param key the name of the resolved property.
   * @param resolvedValue the resolved value of the property.
   */
  private void propagateResolution(final String key, final String resolvedValue) {
    if (!dependedOnMap.containsKey(key)) return;
    Map<String, String> toPropagate = new HashMap<>();
    for (String dependent: dependedOnMap.getValues(key)) {
      if (!dependenciesMap.containsKey(dependent)) continue;
      String value = props.getProperty(dependent);
      if (value != null) props.setProperty(dependent, value = value.replace(SUBST_START + key + SUBST_STOP, resolvedValue));
      dependenciesMap.removeValue(dependent, key);
      if (!dependenciesMap.containsKey(dependent)) toPropagate.put(dependent, value);
    }
    dependedOnMap.removeKey(key);
    for (Map.Entry<String, String> entry: toPropagate.entrySet()) {
      resolved.setProperty(entry.getKey(), entry.getValue());
      propagateResolution(entry.getKey(), entry.getValue());
    }
  }
}
