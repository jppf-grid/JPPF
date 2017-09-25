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

import java.io.*;

import org.jppf.utils.streams.JPPFByteArrayOutputStream;

/**
 * A collection of static utility methods to help with serialization in JPPF.
 * @author Laurent Cohen
 */
public class JPPFSerializationHelper {
  /**
   * The configured serialization.
   */
  private static final JPPFSerialization serialization = JPPFSerialization.Factory.getSerialization();

  /**
   * Serialize the specified object to the specified stream, according to the configured serialization scheme.
   * @param object the object to serialize.
   * @param os the outout stream to serialize to.
   * @throws Exception if any error occurs during the serialization.
   */
  public static void serialize(final Object object, final OutputStream os) throws Exception {
    serialization.serialize(object, os);
  }

  /**
   * Serialize an object to an array of bytes.
   * @param object the object to serialize.
   * @return an array of byte containing the serialized object, starting at position 0 and with no unused position.
   * @throws Exception if any error occurs during the serialization.
   */
  public static byte[] serializeToBytes(final Object object) throws Exception {
    try (JPPFByteArrayOutputStream os = new JPPFByteArrayOutputStream()) {
      serialize(object, os);
      return os.toByteArray();
    }
  }

  /**
   * Serialize the an object from the specified stream, according to the configured serialization scheme.
   * @param is the input stream to deserialize from.
   * @return a deserialized object.
   * @throws Exception if any error occurs during the deserialization.
   */
  public static Object deserialize(final InputStream is) throws Exception {
    return serialization.deserialize(is);
  }

  /**
   * Desrialize an object from an array of bytes.
   * @param bytes the bytes to deserialize from.
   * @param offset the position at which to start reading in the array.
   * @param len the number of bytes to read.
   * @return a deserialized object.
   * @throws Exception if any error occurs during the deserialization.
   */
  public static Object deserializeFromBytes(final byte[] bytes, final int offset, final int len) throws Exception {
    try (ByteArrayInputStream is = new ByteArrayInputStream(bytes, offset, len)) {
      return deserialize(is);
    }
  }
}
