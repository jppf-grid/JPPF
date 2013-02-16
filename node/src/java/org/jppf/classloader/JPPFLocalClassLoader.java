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
package org.jppf.classloader;

import static org.jppf.utils.StringUtils.build;

import java.io.InputStream;
import java.util.List;

import org.jppf.utils.streams.StreamUtils;
import org.slf4j.*;

/**
 * JPPF class loader implementation for nodes running within the same JVM as the JPPF server (local nodes).
 * @author Laurent Cohen
 */
public class JPPFLocalClassLoader extends AbstractJPPFClassLoader
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFLocalClassLoader.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this class loader with a parent class loader.
   * @param connection the connection to the driver.
   * @param parent a ClassLoader instance.
   */
  public JPPFLocalClassLoader(final ClassLoaderConnection connection, final ClassLoader parent)
  {
    super(connection, parent);
    init();
  }

  /**
   * Initialize this class loader with a parent class loader.
   * @param connection the connection to the driver.
   * @param parent a ClassLoader instance.
   * @param uuidPath unique identifier for the submitting application.
   */
  public JPPFLocalClassLoader(final ClassLoaderConnection connection, final ClassLoader parent, final List<String> uuidPath)
  {
    super(connection, parent, uuidPath);
  }

  /**
   * @exclude
   */
  @Override
  public void reset()
  {
    init();
  }

  /**
   * Terminate this classloader and clean the resources it uses.
   * @see org.jppf.classloader.AbstractJPPFClassLoader#close()
   * @exclude
   */
  @Override
  public void close()
  {
    try
    {
      connection.close();
    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
    }
    super.close();
  }

  /**
   * Load a JPPF class from the server.
   * @param name the binary name of the class
   * @return the resulting <tt>Class</tt> object
   * @throws ClassNotFoundException if the class could not be found
   * @exclude
   */
  @Override
  public synchronized Class<?> loadJPPFClass(final String name) throws ClassNotFoundException
  {
    if (debugEnabled) log.debug(build("looking up resource [", name, "]"));
    Class<?> c = findLoadedClass(name);
    if (c == null)
    {
      if (debugEnabled) log.debug(build("resource [", name, "] not already loaded"));
      ClassLoader cl = this;
      while (cl instanceof AbstractJPPFClassLoader) cl = cl.getParent();
      if (cl != null)
      {
        int i = name.lastIndexOf('.');
        if (i >= 0)
        {
          String pkgName = name.substring(0, i);
          Package pkg = getPackage(pkgName);
          if (pkg == null)
          {
            definePackage(pkgName, null, null, null, null, null, null, null);
          }
        }
        String resName = name.replace(".", "/") + ".class";
        InputStream is = cl.getResourceAsStream(resName);
        try
        {
          byte[] definition = StreamUtils.getInputStreamAsByte(is);
          c = defineClass(name, definition, 0, definition.length);
        }
        catch(Exception e)
        {
          log.warn(e.getMessage(), e);
        }
      }
    }
    if (c == null)
    {
      c = findClass(name);
    }
    if (debugEnabled) log.debug(build("definition for resource [", name, "] : ", c));
    return c;
  }
}
