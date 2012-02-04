/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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
public class CustomClassLoader extends URLClassLoader
{
  /**
   * A cache of the definitions of the classes loaded by this class loader.<br>
   * This is in fact the "classpath" for this class loader.
   */
  private Map<String, byte[]> bytecodes = new Hashtable<String, byte[]>();

  /**
   * Initialize this class loader with the specified urls and parent.
   * @param bytecodes a mapping of class names to their bytecode.
   * @param parent the parent class loader.
   */
  public CustomClassLoader(final Map<String, byte[]> bytecodes, final ClassLoader parent) {
    super(new URL[0], parent);
    this.bytecodes.putAll(bytecodes);
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
  protected Class<?> findClass(final String name) throws ClassNotFoundException {
    byte[] bytes = bytecodes.get(name);
    if (bytes == null) throw new ClassNotFoundException("could not find class " + name);
    return defineClass(name, bytes, 0, bytes.length);
  }

  /**
   * Overriden in case someone attempts to get the class definition via this method,
   * so we can effectively point to the definition in memory.
   * {@inheritDoc}
   */
  @Override
  public InputStream getResourceAsStream(final String name) {
    if (name == null) return null;
    InputStream is = null;
    if (getParent() != null) is = getParent().getResourceAsStream(name);
    if (is == null) {
      // compute the class name from its binary name
      int idx = name.lastIndexOf(".class");
      if (idx >= 0) {
        String className = name.substring(0, idx).replace("/", ".");
        byte[] bytes = bytecodes.get(className);
        if (bytes != null) is = new ByteArrayInputStream(bytes);
      }
    }
    return is;
  }
}
