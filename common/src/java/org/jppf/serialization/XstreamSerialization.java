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

package org.jppf.serialization;

import java.io.*;
import java.lang.reflect.*;

import org.jppf.JPPFError;

/**
 * This implementation uses the Xstream serialization library.
 */
public class XstreamSerialization implements JPPFSerialization
{
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

  @Override
  public void serialize(final Object o, final OutputStream os) throws Exception
  {
    ObjectOutputStream oos = (ObjectOutputStream) createOosMethod.invoke(xstream, os);
    oos.writeObject(o);
  }

  @Override
  public Object deserialize(final InputStream is) throws Exception
  {
    ObjectInputStream ois = (ObjectInputStream) createOisMethod.invoke(xstream, is);
    return ois.readObject();
  }

  /**
   * Create an Xstream object using reflection.
   * @return an Object instance.
   */
  private static Object getXstream()
  {
    Object o = null;
    try
    {
      Class<?> xstreamClass = Class.forName("com.thoughtworks.xstream.XStream");
      Class<?> hierarchicalStreamDriverClass = Class.forName("com.thoughtworks.xstream.io.HierarchicalStreamDriver");
      Class<?> driverClass = Class.forName("com.thoughtworks.xstream.io.xml.XppDriver");
      Constructor<?> c = xstreamClass.getConstructor(hierarchicalStreamDriverClass);
      o = c.newInstance(driverClass.newInstance());
      createOisMethod = xstreamClass.getMethod("createObjectInputStream", InputStream.class);
      createOosMethod = xstreamClass.getMethod("createObjectOutputStream", OutputStream.class);
    }
    catch(Exception e)
    {
      throw new JPPFError("A fatal error occurred: " + e.getMessage(), e);
    }
    return o;
  }
}
