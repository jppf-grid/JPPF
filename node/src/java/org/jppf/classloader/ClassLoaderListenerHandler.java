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

import java.util.*;

import org.jppf.utils.ServiceFinder;
import org.slf4j.*;

/**
 * This class loads and registers class loader listeners found in the class path via SPI.
 * @author Laurent Cohen
 */
public final class ClassLoaderListenerHandler
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ClassLoaderListenerHandler.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The singleton instance of this class.
   */
  private static ClassLoaderListenerHandler instance = new ClassLoaderListenerHandler();
  /**
   * The list of listeners loaded via the SPI mechanism.
   */
  private final List<ClassLoaderListener> listeners = new LinkedList<ClassLoaderListener>();

  /**
   * Instantiation is not permitted.
   */
  private ClassLoaderListenerHandler()
  {
    loadHooks();
  }

  /**
   * Load all class loader listeners found in the class path via a service definition.
   */
  private void loadHooks()
  {
    Iterator<ClassLoaderListener> it = ServiceFinder.lookupProviders(ClassLoaderListener.class);
    while (it.hasNext())
    {
      try
      {
        ClassLoaderListener listener = it.next();
        listeners.add(listener);
        if (debugEnabled) log.debug("successfully added initialization class loader listener " + listener.getClass().getName());
      }
      catch(Error e)
      {
        log.error(e.getMessage(), e);
      }
    }
  }

  /**
   * Get the list of listeners loaded via the SPI mechanism.
   * @return a list of {@link ClassLoaderListener} instances.
   */
  List<ClassLoaderListener> getListeners()
  {
    return listeners;
  }

  /**
   * Get the singleton instance of this class.
   * @return a <code>ClassLoaderListenerHandler</code> instance.
   */
  static ClassLoaderListenerHandler getInstance()
  {
    return instance;
  }
 }
