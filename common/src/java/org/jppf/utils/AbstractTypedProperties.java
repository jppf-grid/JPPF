/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

import org.jppf.utils.configuration.*;

/**
 * Extension of the <code>java.util.Properties</code> class to handle the conversion of string values to other types.
 * @author Laurent Cohen
 * @since 6.0
 */
public abstract class AbstractTypedProperties extends Properties {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The default (initial) properties, if any.
   */
  private AbstractTypedProperties initialProps;

  /**
   * Default constructor.
   */
  public AbstractTypedProperties() {
  }

  /**
   * Initialize this object with a set of existing properties.
   * This will copy into the present object all map entries such that both key and value are strings.
   * @param map the properties to be copied. No reference to this parameter is kept in this TypedProperties object.
   */
  public AbstractTypedProperties(final Map<?, ?> map) {
    if (map != null) {
      map.forEach((key, value) -> {
        if ((key instanceof String) && (value instanceof String)) setProperty((String) key, (String) value);
      });
    }
  }

  /**
   * Load the properties from the specified reader.
   * The properties are first loaded, then includes are resolved, variable substitutions are resolved, and finally scripted values are computed.
   * @param <T> the type of object returned by this method.
   * @param reader the reader to read the properties from.
   * @return this {@code TypedProperties} object.
   * @throws IOException if any error occurs.
   */
  @SuppressWarnings("unchecked")
  public synchronized <T extends AbstractTypedProperties> T loadAndResolve(final Reader reader) throws IOException {
    new PropertiesLoader().load(this, reader);
    new SubstitutionsHandler().resolve(this);
    new ScriptHandler().process(this);
    return (T) this;
  }

  /**
   * Convert this set of properties into a string.
   * @return a representation of this object as a string.
   */
  public String asString() {
    return asString(null);
  }

  /**
   * Convert this set of properties into a string.
   * @param delimiter the delimiter between two properties definitions.
   * @return a representation of this object as a string.
   */
  public String asString(final String delimiter) {
    String result = "";
    try (final Writer writer = new StringWriter()) {
      store(writer, null);
      result = writer.toString();
    } catch(final Exception e) {
      return String.format("error converting properties to string: %s", e);
    }
    return (delimiter == null) ? result : result.replace("\n", delimiter);
  }

  /**
   * Populate this propertis object from a string source.
   * @param <T> the type of object returned by this method.
   * @param source the source to read the properties from.
   * @return this properties object.
   */
  @SuppressWarnings("unchecked")
  public <T extends AbstractTypedProperties> T fromString(final String source) {
    return fromString(source, null);
  }

  /**
   * Populate this propertis object from a string source.
   * @param <T> the type of object returned by this method.
   * @param source the source to read the properties from.
   * @param delimiter the delimiter between two properties definitions.
   * @return this properties object.
   */
  @SuppressWarnings("unchecked")
  public <T extends AbstractTypedProperties> T fromString(final String source, final String delimiter) {
    clear();
    try (final Reader reader = new StringReader((delimiter == null) ? source : source.replace(delimiter, "\n"))) {
      loadAndResolve(reader);
    } catch (@SuppressWarnings("unused") final Exception e) {
    }
    return (T) this;
  }

  /**
   * 
   * @return the default (initial) properties, if any.
   * @exclude
   */
  public AbstractTypedProperties getDefaults() {
    return initialProps;
  }

  /**
   * @param defaults the default (initial) properties, if any.
   * @exclude
   */
  public void setDefaults(final AbstractTypedProperties defaults) {
    this.initialProps = defaults;
  }

  /**
   * @exclude
   */
  public void reset() {
    if (initialProps != null) {
      clear();
      putAll(initialProps);
    }
  }

  @Override
  public synchronized String toString() {
    final Set<String> set = new TreeSet<>();
    forEach((k, v) -> {
      if ((k instanceof String) && (v instanceof String)) set.add((String) k + '=' + v);
    });
    final StringBuilder sb = new StringBuilder("{");
    int count = 0;
    for (final String s: set) {
      if (count > 0) sb.append(", ");
      sb.append(s);
      count++;
    }
    return sb.append('}').toString();
  }
}
