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

import java.util.List;

import org.slf4j.*;

/**
 * JPPF class loader implementation for remote standalone nodes.
 * @author Laurent Cohen
 */
public class JPPFClassLoader extends AbstractJPPFClassLoader
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFClassLoader.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this class loader with a parent class loader.
   * @param connection the connection to the driver.
   * @param parent a ClassLoader instance.
   */
  public JPPFClassLoader(final ClassLoaderConnection connection, final ClassLoader parent)
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
  public JPPFClassLoader(final ClassLoaderConnection connection, final ClassLoader parent, final List<String> uuidPath)
  {
    super(connection, parent, uuidPath);
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  void reset()
  {
    try
    {
      connection.reset();
    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
    }
  }
}
