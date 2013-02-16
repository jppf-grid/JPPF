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
package org.jppf.ui.monitoring.job;

import java.util.HashMap;
import java.util.Map;

/**
 * Instances of this class represent changes made to the branch in tree table.
 * @param <T> the type of the values that are changed.
 * @param <K> the type of the key for changes accumulation.
 * @param <V> the type of the values that are accumulated.
 * @author Martin Janda
 */
public class JobAccumulatorBranch<T, K, V> extends JobAccumulator<T>
{
  /**
   * Represents map of accumulated changes.
   */
  private final Map<K, V> map = new HashMap<K, V>();

  /**
   * Initialize this branch job accumulator with the specified value and type of change.
   * @param type the type of updates.
   * @param value the initial value to change.
   */
  public JobAccumulatorBranch(final Type type, final T value)
  {
    super(type, value);
  }

  /**
   * Get the map of accumulated changes.
   * @return map of accumulated changes by key.
   */
  public Map<K, V> getMap()
  {
    return map;
  }

  @Override
  public boolean equals(final Object o)
  {
    if (this == o) return true;
    if (!(o instanceof JobAccumulatorBranch)) return false;
    if (!super.equals(o)) return false;

    JobAccumulatorBranch that = (JobAccumulatorBranch) o;

    return map.equals(that.map);
  }

  @Override
  public int hashCode()
  {
    int result = super.hashCode();
    result = 31 * result + map.hashCode();
    return result;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("JobAccumulatorBranch");
    sb.append("{type=").append(getType());
    sb.append(", value=").append(getValue());
    sb.append(", map=").append(getMap());
    sb.append('}');
    return sb.toString();
  }
}
