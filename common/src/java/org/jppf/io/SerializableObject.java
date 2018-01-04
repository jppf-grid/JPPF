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

package org.jppf.io;

import java.io.*;

import org.jppf.JPPFRuntimeException;
import org.jppf.serialization.*;

/**
 *
 * @param <T> the type of of object to handle.
 * @author Laurent Cohen
 */
public class SerializableObject<T> implements JPPFSerializable<T> {
  /**
   * The actual object.
   */
  private transient T object;
  /**
   * The serialized object.
   */
  private transient byte[] bytes;
  /**
   * Whether the object is available in deserialized form.
   */
  private transient boolean deserialized;

  /**
   * Default construction.
   */
  public SerializableObject() {
  }

  /**
   * Initialize with the specified object.
   * @param object the handled object.
   */
  public SerializableObject(final T object) {
    this.object = object;
    deserialized = true;
  }

  @Override
  @SuppressWarnings("unchecked")
  public T get() {
    if (!deserialized) {
      deserialized = true;
      try {
        object = (T) JPPFSerializationHelper.deserializeFromBytes(bytes, 0, bytes.length);
      } catch (final RuntimeException e) {
        throw e;
      } catch (final Exception e) {
        throw new JPPFRuntimeException(e);
      }
    }
    return object;
  }

  @Override
  public void set(final T object) {
    deserialized = true;
    bytes = null;
    this.object = object;
  }

  /**
   * Save the state of this object to a stream (i.e.,serialize it).
   * @param out the output stream to which to write this object. 
   * @throws IOException if any I/O error occurs.
   */
  private void writeObject(final ObjectOutputStream out) throws IOException {
    try {
      if (bytes == null) bytes = JPPFSerializationHelper.serializeToBytes(object);
      out.writeInt(bytes.length);
      out.write(bytes);
    } catch (final IOException e) {
      throw e;
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  /**
   * Reconstitute this object from a stream (i.e., deserialize it).
   * @param in the input stream from which to read the object. 
   * @throws IOException if any I/O error occurs.
   * @throws ClassNotFoundException if the class of an object in the object graph could not be found.
   */
  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    try {
      final int len = in.readInt();
      bytes = new byte[len];
      SerializationUtils.readToBuf(in, bytes, 0, len);
    } catch (final IOException e) {
      throw e;
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }
}
