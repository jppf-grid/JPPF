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

package org.jppf.utils.compilation;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * A class loader that can load classes from bytecode stored in memory.
 * @author Laurent Cohen
 */
class CustomClassLoader extends URLClassLoader
{
  /**
   * A cache of the definitions of the classes loaded by this class loader.<br>
   * This is in fact the "classpath" for this class loader.
   */
  private Map<String, byte[]> bytecodes = new Hashtable<>();

  /**
   * Initialize this class loader with the specified urls and parent.
   * @param urls a set of urls to add to this class loader's classpath.
   * @param bytecodes a mapping of class names to their bytecode.
   * @param parent the parent class loader.
   */
  public CustomClassLoader(final URL[] urls, final Map<String, byte[]> bytecodes, final ClassLoader parent)
  {
    super(urls == null ? new URL[0] : urls, parent);
    if (bytecodes != null) this.bytecodes.putAll(bytecodes);
  }

  /**
   * Add more classes to this class loader, so it can be reused when
   * additional classes are compiled.
   * @param bytecodes a map of the class defintions to add.
   */
  public void addClasses(final Map<String, byte[]> bytecodes)
  {
    this.bytecodes.putAll(bytecodes);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Class<?> findClass(final String name) throws ClassNotFoundException
  {
    Class<?> c = null;
    byte[] bytes = bytecodes.get(name);
    if (bytes != null)
    {
      c = defineClass(name, bytes, 0, bytes.length);
    }
    else
    {
      c = super.findClass(name);
    }
    return c;
  }

  /**
   * Overriden in case someone attempts to get the class definition via this method,
   * so we can effectively point to the definition in memory.
   * {@inheritDoc}
   */
  @Override
  public InputStream getResourceAsStream(final String name)
  {
    if (name == null) return null;
    InputStream is = null;
    // compute the class name from its binary name
    int idx = name.lastIndexOf(".class");
    if (idx >= 0)
    {
      String className = name.substring(0, idx).replace("/", ".");
      byte[] bytes = bytecodes.get(className);
      if (bytes != null) is = new ByteArrayInputStream(bytes);
    }
    if (is == null) is = super.getResourceAsStream(name);
    return is;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addURL(final URL url)
  {
    super.addURL(url);
  }

  /**
   * Determine whether this class loader already has the specified url in its classpath.
   * @param url the URL to check.
   * @return <code>true</code> if the URL already exists in this class loader's classpath, <code>false</code> otherwise.
   */
  public boolean hasURL(final URL url)
  {
    if (url == null) return false;
    for (URL u: getURLs())
    {
      if (u.equals(url)) return true;
    }
    return false;
  }
}
