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

package org.jppf.serialization.kryo;

import javax.management.ObjectName;

import org.jppf.management.ObjectNameCache;

import com.esotericsoftware.kryo.*;
import com.esotericsoftware.kryo.io.*;

/**
 *
 * @author Laurent Cohen
 */
public class ObjectNameSerializer extends Serializer<ObjectName> {
  @Override
  public ObjectName read(final Kryo kryo, final Input input, final Class<ObjectName> clazz) {
    final String name = kryo.readObject(input, String.class);
    try {
      return ObjectNameCache.getObjectName(name);
    } catch (final Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public void write(final Kryo kryo, final Output output, final ObjectName object) {
    kryo.writeObject(output, object.getCanonicalName());
  }
}
