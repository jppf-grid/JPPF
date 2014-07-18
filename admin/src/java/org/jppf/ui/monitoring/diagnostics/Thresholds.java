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

package org.jppf.ui.monitoring.diagnostics;

import java.util.*;

/**
 * 
 * @author Laurent Cohen
 */
public class Thresholds
{
  /**
   * An enumeration of names for all types of thresholds.
   */
  public enum Name
  {
    /**
     * Memory warning.
     */
    MEMORY_WARNING("health.thresholds.memory.warning"),
    /**
     * Critical memory.
     */
    MEMORY_CRITICAL("health.thresholds.memory.critical"),
    /**
     * CPU warning.
     */
    CPU_WARNING("health.thresholds.cpu.warning"),
    /**
     * Critical CPU.
     */
    CPU_CRITICAL("health.thresholds.cpu.critical");

    /**
     * The associated name.
     */
    private final String name;

    /**
     * Initialize with t he specified name.
     * @param name the name to use.
     */
    private Name(final String name)
    {
      this.name = name;
    }

    /**
     * Get the associated name.
     * @return the name as a string.
     */
    public String getName()
    {
      return name;
    }
  }

  /**
   * The map of values.
   */
  private final Map<Name, Double> values = new EnumMap<>(Name.class);

  /**
   * Default contructor.
   */
  public Thresholds()
  {
    values.put(Name.MEMORY_WARNING, 0.6d);
    values.put(Name.MEMORY_CRITICAL, 0.8d);
    values.put(Name.CPU_WARNING, 0.6d);
    values.put(Name.CPU_CRITICAL, 0.8d);
  }

  /**
   * Get the map of values.
   * @return a map of <code>Name</code> enum values to their threshold value.
   */
  public Map<Name, Double> getValues()
  {
    return values;
  }

  /**
   * Get the threashold value for the specified threshold.
   * @param name the name of the threshold.
   * @return the threshold value as a <code>Double</code>.
   */
  public Double getValue(final Name name)
  {
    return values.get(name);
  }
}
