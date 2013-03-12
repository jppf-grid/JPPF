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

package org.jppf.utils;

import java.lang.Thread.UncaughtExceptionHandler;

import org.slf4j.*;

/**
 * The default uncaught exception handler for JPPF drivers and nodes.
 * @author Laurent Cohen
 */
public class JPPFDefaultUncaughtExceptionHandler implements UncaughtExceptionHandler
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFDefaultUncaughtExceptionHandler.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  @Override
  public void uncaughtException(final Thread t, final Throwable e)
  {
    if (debugEnabled) log.debug("Uncaught exception in thread " + t, e);
    else log.warn("Uncaught exception in thread " + t + ": " + ExceptionUtils.getMessage(e));
  }
}
