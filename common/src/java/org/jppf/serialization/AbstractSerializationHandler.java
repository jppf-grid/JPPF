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

package org.jppf.serialization;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A specfic serialization handler for {@link ConcurrentHashMap}.
 * @author Laurent Cohen
 */
public abstract class AbstractSerializationHandler implements SerializationHandler {
  /**
   * Copy the declared fields of the specified source object into the destination object.
   * @param src the source object.
   * @param dest the destination object.
   * @param cd the class descriptor for both objects.
   * @throws Exception if any error occurs.
   */
  protected void copyFields(final Object src, final Object dest, final ClassDescriptor cd) throws Exception {
    if (src.getClass() != dest.getClass())
      throw new IllegalArgumentException(String.format("source and destination object classes are different: src class = %s, dest class = %s", src.getClass(), dest.getClass()));
    for (FieldDescriptor fd: cd.fields) {
      Object val = fd.field.get(src);
      fd.field.set(dest, val);
    }
  }
}
