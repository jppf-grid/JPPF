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

package org.jppf.serialization;

import java.io.*;

/**
 * This implementation uses the default Java serialization.
 */
public class DefaultJavaSerialization implements JPPFSerialization {
  @Override
  public void serialize(final Object o, final OutputStream os) throws Exception {
    new ObjectOutputStream(os).writeObject(o);
  }

  @SuppressWarnings("resource")
  @Override
  public Object deserialize(final InputStream is) throws Exception {
    return new InternalObjectInputStream(is).readObject();
  }

  /**
   * An {@link ObjectInputSteam} which overrides the class resolution.
   */
  private static final class InternalObjectInputStream extends ObjectInputStream {
    /**
     * Initialize with the specified input stream.
     * @param is the stream from which to deserialize.
     * @throws IOException if any error occurs.
     */
    private InternalObjectInputStream(final InputStream is) throws IOException {
      super(is);
    }

    @Override
    protected Class<?> resolveClass(final ObjectStreamClass desc) throws IOException, ClassNotFoundException {
      final ClassLoader cl = Thread.currentThread().getContextClassLoader();
      if (cl == null) return super.resolveClass(desc);
      return Class.forName(desc.getName(), false, cl);
    }
  }
}
