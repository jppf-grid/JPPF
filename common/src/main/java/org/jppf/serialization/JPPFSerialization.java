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
import java.util.*;

import org.jppf.JPPFError;
import org.jppf.io.IOHelper;
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
    private static final Logger log = LoggerFactory.getLogger(Factory.class);
    /**
     * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
     */
    private static final boolean debugEnabled = log.isDebugEnabled();
    /**
     * The class of the serialization to use.
     */
    private static Class<? extends JPPFSerialization> serializationClass;
    /**
     * The class of the composite to use on top of the serialization.
     */
    private final static Map<String, Class<? extends JPPFCompositeSerialization>> compositeMap = new HashMap<>();
    /**
     * The class of the composite to use on top of the serialization.
     */
    private final static List<Class<? extends JPPFCompositeSerialization>> compositeClasses = new ArrayList<>();
    static {
      init();
      configure();
    }

    /**
     * Initialize the serialization.
     */
    private static void init() {
      final List<Class<? extends JPPFCompositeSerialization>> list = new ServiceFinder().findProviderClassess(JPPFCompositeSerialization.class, null, false);
      for (final Class<? extends JPPFCompositeSerialization> c: list) {
        try {
          final JPPFCompositeSerialization jcs = c.newInstance();
          compositeMap.put(jcs.getName().toUpperCase(), c);
        } catch (final Exception e) {
          final StringBuilder sb = new StringBuilder("Could not instantiate composite serialization '");
          sb.append(c.getName()).append("', terminating this application");
          log.error(sb.toString(), e);
          throw new JPPFError(sb.toString(), e);
        }
      }
      if (debugEnabled) log.debug("compositeMap = {}", compositeMap);
    }

    /**
     * Initialize the serialization.
     */
    @SuppressWarnings("unchecked")
    private static void configure() {
      final JPPFProperty<String> prop = JPPFProperties.OBJECT_SERIALIZATION_CLASS;
      String className = null;
      final String value  = JPPFConfiguration.get(prop);
      if (value != null) {
        final String[] elts = RegexUtils.SPACES_PATTERN.split(value);
        if (elts.length == 1) className = elts[0];
        else if (elts.length >= 2) {
          for (int i=0; i<elts.length-1; i++) {
            final String name = elts[i].toUpperCase();
            final Class<? extends JPPFCompositeSerialization> c = compositeMap.get(name);
            compositeClasses.add(c);
          }
          className = elts[elts.length - 1];
        }
      }
      if (debugEnabled) log.debug("found {} = ", prop.getName(), className);
      if (className != null) {
        if (debugEnabled) log.debug("serializationClass={}, compositeClasses={}, compositeMap={}", className, compositeClasses, compositeMap);
        try {
          serializationClass = (Class<? extends JPPFSerialization>) Class.forName(className);
        } catch (final Exception e) {
          final StringBuilder sb = new StringBuilder("Could not instantiate JPPF serialization [");
          sb.append(prop.getName()).append(" = ").append(className);
          sb.append(", terminating this application");
          log.error(sb.toString(), e);
          throw new JPPFError(sb.toString(), e);
        }
      } else {
        if (debugEnabled) log.debug("using DefaultJavaSerialization");
        serializationClass = DefaultJavaSerialization.class;
      }
    }

    /**
     * Get the configured serialization.
     * @return an instance of {@link JPPFSerialization}.
     */
    public static JPPFSerialization getSerialization() {
      try {
        final JPPFSerialization serialization = serializationClass.newInstance();
        JPPFCompositeSerialization composite = null;
        JPPFCompositeSerialization prev = null;
        for (Class<? extends JPPFCompositeSerialization> c: compositeClasses) {
          final JPPFCompositeSerialization tmp = c.newInstance();
          if (composite == null) composite = tmp;
          if (prev != null) prev.delegateTo(tmp);
          prev = tmp;
        }
        if (prev != null) prev.delegateTo(serialization);
        return (composite == null) ? serialization : composite;
      } catch (final Exception e) {
        final String msg = String.format("error instantiating serialization scheme '%s'", JPPFConfiguration.get(JPPFProperties.OBJECT_SERIALIZATION_CLASS));
        log.error(msg + " :\n{}", ExceptionUtils.getStackTrace(e));
        throw new JPPFError(msg, e);
      }
    }

    /**
     * Reset the configured serialization.
     * @exclude
     */
    public static void reset() {
      serializationClass = null;
      compositeClasses.clear();
      configure();
      IOHelper.resetDefaultSerializer();
      if (debugEnabled) log.debug("serialization = {}, composite = {}", serializationClass, compositeClasses);
    }
  }
}
