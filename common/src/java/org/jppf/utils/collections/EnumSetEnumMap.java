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

package org.jppf.utils.collections;

import java.util.*;

/**
 * A map whose keys and values are elements of an enum type.
 * @param <S> the type of enumeration handled by this map.
 * @author Laurent Cohen
 */
public class EnumSetEnumMap<S extends Enum<S>> extends AbstractCollectionMap<S, S> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The class of the enumeration handled by this map.
   */
  private final Class<S> enumClass;

  /**
   * Initialize this map with the specified enumeration class.
   * @param enumClass the class of the enumeration handled by this map.
   */
  public EnumSetEnumMap(final Class<S> enumClass) {
    this.enumClass = enumClass;
    map = createMap();
  }

  @Override
  protected Map<S, Collection<S>> createMap() {
    return new EnumMap<>(enumClass);
  }

  @Override
  protected Collection<S> newCollection() {
    return EnumSet.noneOf(enumClass);
  }
}
