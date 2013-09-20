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
package org.jppf.server.nio.classloader;

import org.jppf.utils.Pair;

/**
 * This class represents the key used in the class cache.
 * @author Domingos Creado
 */
public class CacheClassKey extends Pair<String, String>
{
  /**
   * Initialize this key with a specified provider uuid and resource string.
   * @param uuid the provider uuid.
   * @param res string describing the cached resource.
   */
  public CacheClassKey(final String uuid, final String res)
  {
    super(uuid, res);
    if (uuid == null) throw new IllegalArgumentException("uuid is null");
    if (res == null) throw new IllegalArgumentException("res is null");
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[uuid=" + first + ", res=" + second + ']';
  }
}
