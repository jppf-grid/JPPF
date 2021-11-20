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
import java.lang.reflect.*;

import javax.management.Notification;

import org.jppf.JPPFError;
import org.jppf.job.JobNotification;
import org.jppf.management.TaskExecutionNotification;
import org.jppf.management.forwarding.JPPFNodeForwardingNotification;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This implementation uses the Xstream serialization library.
 */
public class XstreamSerialization implements JPPFSerialization {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(XstreamSerialization.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * The method to invoke to create an object input stream.
   */
  private static Method createOisMethod;
  /**
   * The method to invoke to create an object output stream.
   */
  private static Method createOosMethod;
  /**
   * The Xstream facade object.
   */
  private static final Object xstream = getXstream();

  @Override
  public void serialize(final Object o, final OutputStream os) throws Exception {
    final ObjectOutputStream oos = (ObjectOutputStream) createOosMethod.invoke(xstream, os, "xml-stream");
    oos.writeObject(o);
    oos.flush();
  }

  @Override
  public Object deserialize(final InputStream is) throws Exception {
    final ObjectInputStream ois = (ObjectInputStream) createOisMethod.invoke(xstream, is);
    return ois.readObject();
  }

  /**
   * Create an Xstream object using reflection.
   * @return an Object instance.
   */
  private static Object getXstream() {
    Object o = null;
    try {
      final Class<?> xstreamClass = Class.forName("com.thoughtworks.xstream.XStream");
      final Class<?> hierarchicalStreamDriverClass = Class.forName("com.thoughtworks.xstream.io.HierarchicalStreamDriver");
      final Class<?> concreteDriverClass = Class.forName("com.thoughtworks.xstream.io.xml.XppDriver");
      final Class<?> reflectionProviderClass = Class.forName("com.thoughtworks.xstream.converters.reflection.ReflectionProvider");
      final Class<?> concreteProviderClass = Class.forName("com.thoughtworks.xstream.converters.reflection.SunUnsafeReflectionProvider");
      final Object concreteProvider = concreteProviderClass.newInstance();
      final Constructor<?> c = xstreamClass.getConstructor(reflectionProviderClass, hierarchicalStreamDriverClass);
      o = c.newInstance(concreteProvider, concreteDriverClass.newInstance());
      // register a converter for javax.management.Notification and subclasses:
      // xstream.registerConverter(new ReflectionConverter(xstream.getMapper(), xstream.getReflectionProvider(), Notification.class));
      final Method getMapper = xstreamClass.getMethod("getMapper");
      final Method registerConverter = ReflectionHelper.findMethod(xstreamClass.getName(), "registerConverter", "com.thoughtworks.xstream.converters.Converter");
      final Constructor<?> converterConst = ReflectionHelper.findConstructor("com.thoughtworks.xstream.converters.reflection.ReflectionConverter",
        "com.thoughtworks.xstream.mapper.Mapper", "com.thoughtworks.xstream.converters.reflection.ReflectionProvider", Class.class.getName());
      final Class<?>[] classesToregister = { TaskExecutionNotification.class, Notification.class, JobNotification.class, JPPFNodeForwardingNotification.class };
      for (final Class<?> clazz: classesToregister) {
        final Object converter = converterConst.newInstance(getMapper.invoke(o), concreteProvider, clazz);
        registerConverter.invoke(o, converter);
      }
      // methods to create an ObjectInputStream and ObjectOutputStream
      createOisMethod = xstreamClass.getMethod("createObjectInputStream", InputStream.class);
      createOosMethod = xstreamClass.getMethod("createObjectOutputStream", OutputStream.class, String.class);
      // to avoid the annoying security warning
      final Method allowMethod = xstreamClass.getMethod("allowTypesByRegExp", String[].class);
      final String[] patterns = { ".*" };
      allowMethod.invoke(o, (Object) patterns);
    } catch (final Throwable t) {
      log.error("A fatal error occurred: {}", ExceptionUtils.getStackTrace(t));
      if (debugEnabled) {
        log.debug("JPPF properties:\n{}", JPPFConfiguration.getProperties().asString());
        log.debug("system properties:\n{}", new TypedProperties(System.getProperties()).asString());
      }
      throw new JPPFError("A fatal error occurred: " + t.getMessage(), t);
    }
    return o;
  }
}
