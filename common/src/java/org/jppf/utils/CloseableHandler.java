/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.utils;

import java.io.Closeable;
import java.util.Collection;

import org.jppf.utils.collections.*;
import org.slf4j.*;

/**
 * A helper class to handle close operations throughout a JVM.
 * @author Laurent Cohen
 * @exclude
 */
public class CloseableHandler
{
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(CloseableHandler.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Name for the set of driver closeables.
   */
  public static String DRIVER = "driver";
  /**
   * Name for the set of node closeables.
   */
  public static String NODE = "node";
  /**
   * Map of named handlers.
   */
  private static CollectionMap<String, Closeable> handlerMap = new CopyOnWriteListConcurrentMap<>();

  /**
   * Close() all the closeables.
   * @param name the type of closeable to handle.
   */
  public static void handleCloseables(final String name)
  {
    Collection<Closeable> coll = handlerMap.getValues(name);
    if (coll == null) return;
    try
    {
      for (Closeable c: coll)
      {
        try
        {
          if (c != null) c.close();
        }
        catch (Exception e)
        {
          if (debugEnabled) log.debug(e.getMessage(), e);
          else log.warn(ExceptionUtils.getMessage(e));
        }
      }
    }
    finally
    {
      coll.clear();
      handlerMap.removeKey(name);
    }
  }

  /**
   * Add a {@link Closeable} to the list of reset closeables.
   * @param handlerName the type of closeable to add.
   * @param c the {@link Closeable} object to add.
   */
  public static void addResetCloseable(final String handlerName, final Closeable c)
  {
    handlerMap.putValue(handlerName, c);
  }
}
