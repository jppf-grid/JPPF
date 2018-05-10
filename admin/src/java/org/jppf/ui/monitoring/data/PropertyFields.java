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

package org.jppf.ui.monitoring.data;

import org.jppf.utils.configuration.JPPFProperty;

/**
 * 
 * @author Laurent Cohen
 */
public class PropertyFields implements Fields, Comparable<PropertyFields> {
  /**
   * The associated property.
   */
  private final JPPFProperty<?> property;

  /**
   * Initialize with the specified property.
   * @param property the associated property.
   */
  public PropertyFields(final JPPFProperty<?> property) {
    this.property = property;
  }

  @Override
  public int compareTo(final PropertyFields o) {
    if (o == null) return 1;
    return getName().compareTo(o.getName());
  }

  @Override
  public String getName() {
    return property.getName();
  }

  @Override
  public String getLocalizedName() {
    return property.getShortLabel();
  }

  @Override
  public String toString() {
    return property.getShortLabel();
  }
}
