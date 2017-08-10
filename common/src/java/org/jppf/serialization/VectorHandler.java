/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

import java.util.*;

import org.slf4j.*;

/**
 * A specfic serialization handler for {@link Vector}.
 * @author Laurent Cohen
 */
public class VectorHandler extends AbstractSerializationHandler {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(Serializer.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean traceEnabled = log.isTraceEnabled();

  @Override
  public void writeDeclaredFields(final Serializer serializer, final ClassDescriptor cd, final Object obj) throws Exception {
    if (traceEnabled) log.trace("writing declared fields for cd={}", cd);
    Vector<?>  vector = (Vector<?>) obj;
    ClassDescriptor tmpDesc = null;
    try {
      tmpDesc = serializer.currentClassDescriptor;
      serializer.currentClassDescriptor = cd;
      synchronized(vector) {
        serializer.writeInt(vector.size());
        List<Object> list = new ArrayList<>(vector);
        for (Object o: list) serializer.writeObject(o);
      }
    } finally {
      serializer.currentClassDescriptor = tmpDesc;
    }
  }

  @Override
  public void readDeclaredFields(final Deserializer deserializer, final ClassDescriptor cd, final Object obj) throws Exception {
    if (traceEnabled) log.trace("reading declared fields for cd={}", cd);
    @SuppressWarnings("unchecked")
    Vector<? super Object> vector = (Vector<? super Object>) obj;
    ClassDescriptor tmpDesc = null;
    try {
      tmpDesc = deserializer.currentClassDescriptor;
      deserializer.currentClassDescriptor = cd;
      copyFields(new Vector<>(), vector, cd);
      int size = deserializer.readInt();
      for (int i=0; i<size; i++) {
        Object value = deserializer.readObject();
        vector.add(value);
      }
    } finally {
      deserializer.currentClassDescriptor = tmpDesc;
    }
  }
}
