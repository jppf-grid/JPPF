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

package org.jppf.management;

import java.io.*;

import org.jppf.comm.socket.BootstrapObjectSerializer;

/**
 * 
 * @author Laurent Cohen
 * @exclude
 */
public class WrappedObject implements Serializable {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * 
   */
  private static final BootstrapObjectSerializer SERIALIZER = new BootstrapObjectSerializer();
  /**
   * The object to wrap.
   */
  private Object object;

  /**
   * 
   * @param object the object to wrap.
   */
  public WrappedObject(final Object object) {
    this.object = object;
  }

  /**
   * Save the state of the notification to a stream (i.e. serialize it).
   * @param out the output stream to which to write the job. 
   * @throws IOException if any I/O error occurs.
   */
  private void writeObject(final ObjectOutputStream out) throws IOException {
    try {
      SERIALIZER.serialize(object, out, false);
    } catch (final IOException e) {
      throw e;
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  /**
   * Reconstitute the notification instance from an object stream (i.e. deserialize it).
   * @param in the input stream from which to read the job. 
   * @throws IOException if any I/O error occurs.
   * @throws ClassNotFoundException if the class of an object in the object graph can not be found.
   */
  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    try {
      object = SERIALIZER.deserialize(in, false);
    } catch (final IOException|ClassNotFoundException e) {
      throw e;
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  /**
   * @return the wrapped object.
   */
  public Object getObject() {
    return object;
  }
}
