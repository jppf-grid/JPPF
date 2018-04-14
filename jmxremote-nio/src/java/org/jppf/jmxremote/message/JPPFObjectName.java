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

package org.jppf.jmxremote.message;

import java.io.*;

import javax.management.*;

import org.jppf.management.ObjectNameCache;

/**
 * A wrapper for {@link ObjectName} objects to make serialization/serialization faster.
 * @author Laurent Cohen
 */
public class JPPFObjectName implements Serializable {
  /**
   * 
   */
  private transient ObjectName objectName;

  /**
   * Initialize this wrapper witht he specified {@link ObjectName}.
   * @param objectName .
   */
  public JPPFObjectName(final ObjectName objectName) {
    this.objectName = objectName;
  }

  /**
   * @return an {@code ObjectName}.
   */
  public ObjectName getObjectName() {
    return objectName;
  }

  /**
   * Save the state of this object to a stream (i.e.,serialize it).
   * @param out the output stream to which to write this object. 
   * @throws IOException if any I/O error occurs.
   */
  private void writeObject(final ObjectOutputStream out) throws IOException {
    out.writeObject(objectName.getCanonicalName());
  }

  /**
   * Reconstitute this object from a stream (i.e., deserialize it).
   * @param in the input stream from which to read the object. 
   * @throws IOException if any I/O error occurs.
   * @throws ClassNotFoundException if the class of an object in the object graph could not be found.
   */
  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    try {
      objectName = ObjectNameCache.getObjectName((String) in.readObject());
    } catch (final MalformedObjectNameException e) {
      throw new IOException(e);
    }
  }
}
