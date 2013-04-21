/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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
import org.jppf.utils.JPPFConfiguration;
import org.slf4j.*;

/**
 * Interface and factory for object serialization and deserialization schemes in JPPF.
 * <p>A serialization scheme is defined as an implementation of this interface,
 * and configured in a JPPF configuration file using the property definition:<br>
 * <code>jppf.object.serialization.class = my.implementation.OfJPPFSerialization</code>
 * <p>The same implementation must be used for all nodes, servers and clients.
 * <p>The following built-in serialization schemes are provided:<br>
 * - {@link JPPFSerialization.DefaultJavaSerialization}, configured as<br>
 *   <code>jppf.object.serialization.class = org.jppf.serialization.JPPFSerialization$DefaultJavaSerialization</code><br>
 * - {@link JPPFSerialization.DefaultJPPFSerialization}, configured as<br>
 *   <code>jppf.object.serialization.class = org.jppf.serialization.JPPFSerialization$DefaultJPPFSerialization</code><br>
 * (note the use of '$' for the inner class).
 * @author Laurent Cohen
 */
public interface JPPFSerialization
{
  /**
   * Configuration property name for object serialization.
   */
  String SERIALIZATION_CLASS = "jppf.object.serialization.class";

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
  public static class Factory
  {
    /**
     * Logger for this class.
     */
    private static Logger log = LoggerFactory.getLogger(Factory.class);
    /**
     * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
     */
    private static boolean debugEnabled = log.isDebugEnabled();
    /**
     * The serialization to use.
     */
    private static JPPFSerialization serialization = init();

    /**
     * Initialize the serialization.
     * @return the defined {@link JPPFSerialization} instance.
     */
    private static JPPFSerialization init()
    {
      String className = JPPFConfiguration.getProperties().getString(SERIALIZATION_CLASS);
      if (debugEnabled) log.debug("found " + SERIALIZATION_CLASS + " = " + className);
      if (className != null)
      {
        try
        {
          Class<?> clazz = Class.forName(className);
          return (JPPFSerialization) clazz.newInstance();
        }
        catch (Exception e)
        {
          StringBuilder sb = new StringBuilder();
          sb.append("Could not instantiate JPPF serialization [").append(SERIALIZATION_CLASS).append(" = ").append(className);
          sb.append(", terminating this application");
          log.error(sb.toString(), e);
          throw new JPPFError(sb.toString(), e);
        }
      }
      else // use "legacy" object stream builder if one is configured
      {
        @SuppressWarnings("deprecation")
        JPPFObjectStreamBuilder builder = JPPFObjectStreamFactory.init();
        if (debugEnabled) log.debug("found JPPFObjectStreamBuilder = " + builder);
        if (builder != null) return new ObjectStreamBuilderSerialization(builder);
      }
      if (debugEnabled) log.debug("using DefaultJavaSerialization");
      return new DefaultJavaSerialization();
    }

    /**
     * Get the configured serialization.
     * @return an instance of {@link JPPFSerialization}.
     */
    public static JPPFSerialization getSerialization()
    {
      return serialization;
    }
  }

  /**
   * This implementation uses the default Java serialization.
   */
  public static class DefaultJavaSerialization implements JPPFSerialization
  {
    @Override
    public void serialize(final Object o, final OutputStream os) throws Exception
    {
      new ObjectOutputStream(os).writeObject(o);
    }

    @Override
    public Object deserialize(final InputStream is) throws Exception
    {
      ObjectInputStream ois = new ObjectInputStream(is) {
        @Override
        protected Class<?> resolveClass(final ObjectStreamClass desc) throws IOException, ClassNotFoundException {
          ClassLoader cl = Thread.currentThread().getContextClassLoader();
          if (cl == null)  return super.resolveClass(desc);
          return Class.forName(desc.getName(), false, cl);
        }
      };
      return ois.readObject();
    }
  }

  /**
   * This implementation uses the JPPF serialization.
   */
  public static class DefaultJPPFSerialization implements JPPFSerialization
  {
    @Override
    public void serialize(final Object o, final OutputStream os) throws Exception
    {
      new JPPFObjectOutputStream(os).writeObject(o);
    }

    @Override
    public Object deserialize(final InputStream is) throws Exception
    {
      return new JPPFObjectInputStream(is).readObject();
    }
  }

  /**
   * This implementation uses a {@link JPPFObjectStreamBuilder} and is used for compatibility with versions up to 3.3.
   */
  @SuppressWarnings("deprecation")
  public static class ObjectStreamBuilderSerialization implements JPPFSerialization
  {
    /**
     * The stream builder to use.
     */
    private final JPPFObjectStreamBuilder builder;

    /**
     * Initialize this serialization with the specified object stream builder.
     * @param builder the stream builder to use.
     */
    ObjectStreamBuilderSerialization(final JPPFObjectStreamBuilder builder)
    {
      this.builder = builder;
    }

    @Override
    public void serialize(final Object o, final OutputStream os) throws Exception
    {
      builder.newObjectOutputStream(os).writeObject(o);
    }

    @Override
    public Object deserialize(final InputStream is) throws Exception
    {
      return builder.newObjectInputStream(is).readObject();
    }
  }
}
