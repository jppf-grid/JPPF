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
package org.jppf.utils;


import java.io.*;
import java.util.*;

/**
 * This class is an extension of {@link TypedProperties} which does not allow modifying the properties it contains.
 * The methods in the super class that modify the properties have been overriden such that they do nothing, without
 * rasing any exception.
 * @author Laurent Cohen
 */
public class UnmodifiableTypedProperties extends TypedProperties {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Creates an empty unmodifiable properties map.
   */
  public UnmodifiableTypedProperties() {
  }

  /**
   * Initialize this object with a set of existing properties.
   * This will copy into the present object all map entries such that both key and value are strings.
   * @param map the properties to be copied. No reference to this parameter is kept in this TypedProperties object.
   */
  public UnmodifiableTypedProperties(final Map<Object, Object> map) {
    if (map != null) {
      for (Map.Entry<Object, Object> entry: map.entrySet()) {
        if ((entry.getKey() instanceof String) && (entry.getValue() instanceof String)) {
          super.put(entry.getKey(), entry.getValue());
        }
      }
    }
  }

  /**
   * This method does nothing as modification is not permitted.
   * @param key not used.
   * @param value not used.
   * @return <code>null</code>.
   */
  @Override
  public synchronized Object setProperty(final String key, final String value) {
    return null;
  }

  /**
   * This method does nothing as modification is not permitted.
   * @param reader not used.
   * @throws IOException never thrown.
   */
  @Override
  public synchronized void load(final Reader reader) throws IOException {
  }

  /**
   * This method does nothing as modification is not permitted.
   * @param in not used.
   * @throws IOException never thrown.
   */
  @Override
  public synchronized void load(final InputStream in) throws IOException {
  }

  /**
   * This method does nothing as modification is not permitted.
   * @param in not used.
   * @throws IOException never thrown.
   * @throws InvalidPropertiesFormatException never thrown.
   */
  @Override
  public synchronized void loadFromXML(final InputStream in) throws IOException, InvalidPropertiesFormatException {
  }

  /**
   * This method does nothing as modification is not permitted.
   * @param key not used.
   * @param value not used.
   * @return <code>null</code>.
   */
  @Override
  public synchronized Object put(final Object key, final Object value) {
    return null;
  }

  /**
   * This method does nothing as modification is not permitted.
   * @param key not used.
   * @return <code>null</code>.
   */
  @Override
  public synchronized Object remove(final Object key) {
    return null;
  }

  /**
   * This method does nothing as modification is not permitted.
   * @param map not used.
   */
  @Override
  public synchronized void putAll(final Map<? extends Object, ? extends Object> map) {
  }
}
