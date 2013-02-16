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
import java.lang.reflect.*;
import java.util.concurrent.locks.ReentrantLock;

import org.jppf.JPPFError;
import org.slf4j.*;

/**
 * Object stream factory that creates streams from the XStream framework.
 * This enables serialization to and deserialization from XML streams, and allows non-serializable classes to be processed.
 * If you do not use it, it will not generate compile time errors, even if the XStream libraries are not in the classpath,
 * as this class relies entirely on reflection to instantiate the required objects.
 * @author Laurent Cohen
 */
public class XstreamObjectStreamBuilder implements JPPFObjectStreamBuilder
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(XstreamObjectStreamBuilder.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Used for thread synchronization when initializing the xstream object.
   */
  private static ReentrantLock lock = new ReentrantLock();
  /**
   * The method to invoke to create an object input stream.
   */
  private static Method createOisMethod = null;
  /**
   * The method to invoke to create an object output stream.
   */
  private static Method createOosMethod = null;
  /**
   * The Xstream facade object.
   */
  private static Object xstream = getXstream();

  /**
   * Obtain an input stream used for deserializing objects.
   * @param	in input stream to read from.
   * @return an <code>ObjectInputStream</code>
   * @throws Exception if an error is raised while creating the stream.
   * @see org.jppf.serialization.JPPFObjectStreamBuilder#newObjectInputStream(java.io.InputStream)
   */
  @Override
  public ObjectInputStream newObjectInputStream(final InputStream in) throws Exception
  {
    return (ObjectInputStream) createOisMethod.invoke(xstream, new Object[] {in});
  }

  /**
   * Obtain an Output stream used for serializing objects.
   * @param	out output stream to write to.
   * @return an <code>ObjectOutputStream</code>
   * @throws Exception if an error is raised while creating the stream.
   * @see org.jppf.serialization.JPPFObjectStreamBuilder#newObjectOutputStream(java.io.OutputStream)
   */
  @Override
  public ObjectOutputStream newObjectOutputStream(final OutputStream out) throws Exception
  {
    return (ObjectOutputStream) createOosMethod.invoke(xstream, new Object[] {out});
  }

  /**
   * Create an Xstream object using reflection.
   * @return an Object instance.
   */
  private static synchronized Object getXstream()
  {
    try
    {
      if (xstream == null)
      {
        lock.lock();
        try
        {
          if (xstream == null)
          {
            Class<?> xstreamClass = Class.forName("com.thoughtworks.xstream.XStream");
            Class<?> hierarchicalStreamDriverClass = Class.forName("com.thoughtworks.xstream.io.HierarchicalStreamDriver");
            Class<?> driverClass = Class.forName("com.thoughtworks.xstream.io.xml.XppDriver"); // the fastest so far
            //Class driverClass = Class.forName("com.thoughtworks.xstream.io.xml.Dom4JDriver"); // causes org.dom4j.DocumentException: Error on line 1 of document  : Premature end of file
            //Class driverClass = Class.forName("com.thoughtworks.xstream.io.xml.JDomDriver"); // causes StackOverflowError
            //Class driverClass = Class.forName("com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver"); // causes com.thoughtworks.xstream.mapper.CannotResolveClassException: loadFactor : loadFactor
            //Class driverClass = Class.forName("com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver"); // causes java.lang.UnsupportedOperationException: The JsonHierarchicalStreamDriver can only write JSON
            //Class driverClass = Class.forName("com.thoughtworks.xstream.io.xml.XomDriver"); // causes org.xml.sax.SAXException: FWK005 parse may not be called while parsing
            //Class driverClass = Class.forName("com.thoughtworks.xstream.io.xml.StaxDriver"); // causes a stack overflow at not initialization
            //Class driverClass = Class.forName("com.thoughtworks.xstream.io.xml.DomDriver"); // very very slow
            Constructor<?> c = xstreamClass.getConstructor(hierarchicalStreamDriverClass);
            Object o = c.newInstance(driverClass.newInstance());
            createOisMethod = xstreamClass.getMethod("createObjectInputStream", new Class[] {InputStream.class});
            createOosMethod = xstreamClass.getMethod("createObjectOutputStream", new Class[] {OutputStream.class});
            xstream = o;
          }
        }
        finally
        {
          lock.unlock();
        }
      }
    }
    catch(Exception e)
    {
      log.error(e.getMessage(), e);
      throw new JPPFError("A fatal error occurred: " + e.getMessage(), e);
    }
    return xstream;
  }
}
