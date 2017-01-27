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
 * An implementation of the {@link Metadata} interface backed by a {@link Hashtable}.
 * @author Laurent Cohen
 */
public class MetadataImpl implements Metadata
{
  /**
   * The map holding the mapping of keys to values.
   */
  protected final Map<Object, Object> parameters = new Hashtable<>();

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getParameter(final Object key)
  {
    return (T) parameters.get(key);
  }

  @Override
  public <T> T getParameter(final Object key, final T defaultValue)
  {
    @SuppressWarnings("unchecked")
    T res = (T) parameters.get(key);
    return res == null ? defaultValue : res;
  }

  @Override
  public void setParameter(final Object key, final Object value)
  {
    parameters.put(key, value);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T removeParameter(final Object key)
  {
    return (T) parameters.remove(key);
  }

  @Override
  public Map<Object, Object> getAll()
  {
    return parameters;
  }

  @Override
  public String toString()
  {
    return new StringBuilder(getClass().getSimpleName()).append(parameters).toString();
  }

  @Override
  public void clear()
  {
    parameters.clear();
  }
}
