/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

import org.jppf.JPPFError;
import org.jppf.utils.*;
import org.jppf.utils.configuration.*;
import org.slf4j.*;

/**
 * Interface and factory for object serialization and deserialization schemes in JPPF.
 * <p>A serialization scheme is defined as an implementation of this interface,
 * and configured in a JPPF configuration file using the property definition:<br>
 * <code>jppf.object.serialization.class = my.implementation.OfJPPFSerialization</code>
 * <p>The same implementation must be used for all nodes, servers and clients.
 * @author Laurent Cohen
 */
public interface JPPFSerialization {
  /**
   * Serialize an object into the specified output stream.
   * @param o the object to serialize.
   * @param os the stream that receives the serialized object.
   * @throws Exception if any error occurs.
   */
  void serialize(Object o, OutputStream os) throws Exception;

  /**
   * Deserialize an object from the specified input stream.
   * @param is the stream from which to deserialize.
   * @return the serialized object.
   * @throws Exception if any error occurs.
   */
  Object deserialize(InputStream is) throws Exception;

  /**
   * Factory class for instantiating a default or configured serialization.
   */
  public static class Factory {
    /**
     * Logger for this class.
     */
    private static Logger log = LoggerFactory.getLogger(Factory.class);
    /**
     * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
     */
    private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
    /**
     * The serialization to use.
     */
    private static JPPFSerialization serialization = init();

    /**
     * Initialize the serialization.
     * @return the defined {@link JPPFSerialization} instance.
     */
    private static JPPFSerialization init() {
      JPPFProperty<String> prop = JPPFProperties.OBJECT_SERIALIZATION_CLASS;
      String className = null;
      CompositeSerialization compressor = null;
      String value  = JPPFConfiguration.get(prop);
      if (value != null) {
        String[] elts = value.split("\\s");
        if (elts.length == 1) className = elts[0];
        else if (elts.length >= 2) {
          className = elts[1];
          switch(elts[0].toUpperCase()) {
            case "LZ4":
              compressor = new LZ4Serialization();
              break;
            case "GZIP":
              break;
          }
        }
      }
      if (debugEnabled) log.debug("found " + prop.getName() + " = " + className);
      if (className != null) {
        try {
          Class<?> clazz = Class.forName(className);
          JPPFSerialization ser = (JPPFSerialization) clazz.newInstance();
          return compressor == null ? ser : compressor.delegateTo(ser);
        } catch (Exception e) {
          StringBuilder sb = new StringBuilder("Could not instantiate JPPF serialization [");
          sb.append(prop.getName()).append(" = ").append(className);
          sb.append(", terminating this application");
          log.error(sb.toString(), e);
          throw new JPPFError(sb.toString(), e);
        }
      }
      if (debugEnabled) log.debug("using DefaultJavaSerialization");
      return new DefaultJavaSerialization();
    }

    /**
     * Get the configured serialization.
     * @return an instance of {@link JPPFSerialization}.
     */
    public static JPPFSerialization getSerialization() {
      return serialization;
    }

    /**
     * Reset the configured serialization.
     * @exclude
     */
    public static void reset() {
      serialization = init();
    }
  }
}
