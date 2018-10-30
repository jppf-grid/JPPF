/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

import org.jppf.serialization.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.*;

/**
 * A generic serializer which resorts to default JPPF serialization.
 * @author Laurent Cohen
 */
public class GenericObjectSerializer extends Serializer<Object> {
  @SuppressWarnings("resource")
  @Override
  public Object read(final Kryo kryo, final Input input, final Class<Object> clazz) {
    try {
      return new JPPFObjectInputStream(input).readObject();
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("resource")
  @Override
  public void write(final Kryo kryo, final Output output, final Object object) {
    try {
      new JPPFObjectOutputStream(output).writeObject(object);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
}
