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

package org.jppf.comm.socket;

import java.io.*;

import org.jppf.serialization.*;
import org.jppf.utils.JPPFBuffer;
import org.jppf.utils.streams.JPPFByteArrayOutputStream;

/**
 * This serializer is used solely by the nodes to enable their bootstrapping.
 * @author Laurent Cohen
 */
public class BootstrapObjectSerializer implements ObjectSerializer {
  /**
   * 
   */
  private JPPFSerialization serialization = JPPFSerialization.Factory.getSerialization();

  /**
   * The default constructor must be public to allow for instantiation through Java reflection.
   */
  public BootstrapObjectSerializer() {
  }

  /**
   * Serialize an object into an array of bytes.
   * @param o the object to Serialize.
   * @return a <code>JPPFBuffer</code> instance holding the serialized object.
   * @throws Exception if the object can't be serialized.
   */
  @Override
  public JPPFBuffer serialize(final Object o) throws Exception {
    return serialize(o, false);
  }

  /**
   * Serialize an object into an array of bytes.
   * @param o the object to Serialize.
   * @param noCopy avoid copying intermediate buffers.
   * @return a <code>JPPFBuffer</code> instance holding the serialized object.
   * @throws Exception if the object can't be serialized.
   */
  @Override
  public JPPFBuffer serialize(final Object o, final boolean noCopy) throws Exception {
    final JPPFByteArrayOutputStream baos = new JPPFByteArrayOutputStream();
    serialize(o, baos);
    final byte[] data = noCopy ? baos.getBuf() : baos.toByteArray();
    return new JPPFBuffer(data, baos.size());
  }

  /**
   * Serialize an object into an output stream.
   * @param o the object to Serialize.
   * @param os the output stream to serialize to.
   * @throws Exception if the object can't be serialized.
   */
  @Override
  public void serialize(final Object o, final OutputStream os) throws Exception {
    serialize(o, os, true);
  }

  /**
   * Serialize an object into an output stream.
   * @param o the object to Serialize.
   * @param os the output stream to serialize to.
   * @param closeStream whether to close the stream before returning.
   * @throws Exception if the object can't be serialized.
   */
  public void serialize(final Object o, final OutputStream os, final boolean closeStream) throws Exception {
    try {
      serialization.serialize(o, os);
    } finally {
      if (closeStream) os.close();
    }
  }

  /**
   * Read an object from an array of bytes.
   * @param buf buffer holding the array of bytes to deserialize from.
   * @return the object that was deserialized from the array of bytes.
   * @throws Exception if the ObjectInputStream used for deserialization raises an error.
   */
  @Override
  public Object deserialize(final JPPFBuffer buf) throws Exception {
    return deserialize(new ByteArrayInputStream(buf.getBuffer()));
  }

  /**
   * Read an object from an array of bytes.
   * @param bytes buffer holding the array of bytes to deserialize from.
   * @return the object that was deserialized from the array of bytes.
   * @throws Exception if the ObjectInputStream used for deserialization raises an error.
   */
  @Override
  public Object deserialize(final byte[] bytes) throws Exception {
    return deserialize(new ByteArrayInputStream(bytes));
  }

  /**
   * Read an object from an array of bytes.
   * @param bytes buffer holding the array of bytes to deserialize from.
   * @param offset position at which to start reading the bytes from.
   * @param length the number of bytes to read.
   * @return the object that was deserialized from the array of bytes.
   * @throws Exception if the ObjectInputStream used for deserialization raises an error.
   */
  @Override
  public Object deserialize(final byte[] bytes, final int offset, final int length) throws Exception {
    return deserialize(new ByteArrayInputStream(bytes, offset, length));
  }

  /**
   * Read an object from an input stream.
   * @param is the input stream to deserialize from.
   * @return the object that was deserialized from the array of bytes.
   * @throws Exception if the ObjectInputStream used for deserialization raises an error.
   */
  @Override
  public Object deserialize(final InputStream is) throws Exception {
    return deserialize(is, true);
  }

  /**
   * Read an object from an input stream.
   * @param is the input stream to deserialize from.
   * @param closeStream whether to close the stream before returning.
   * @return the object that was deserialized from the array of bytes.
   * @throws Exception if the ObjectInputStream used for deserialization raises an error.
   */
  public Object deserialize(final InputStream is, final boolean closeStream) throws Exception {
    try {
      return serialization.deserialize(is);
    } finally {
      if (closeStream) is.close();
    }
  }
}
