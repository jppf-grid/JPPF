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
package org.jppf.task.storage;

import org.jppf.utils.collections.MetadataImpl;


/**
 * Implementation of a data provider that handles in-memory data backed by a <code>Map</code>.
 * @see org.jppf.task.storage.DataProvider
 * @author Laurent Cohen
 */
public class MemoryMapDataProvider extends MetadataImpl implements DataProvider
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * {@inheritDoc}
   * @deprecated use {@link #getParameter(Object)} instead)
   */
  @Override
  public <T> T  getValue(final Object key)
  {
    return getParameter(key);
  }

  /**
   * {@inheritDoc}
   * @deprecated use {@link #setParameter(Object, Object)} instead)
   */
  @Override
  public void setValue(final Object key, final Object value)
  {
    setParameter(key, value);
  }
}
