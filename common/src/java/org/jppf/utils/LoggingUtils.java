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

package org.jppf.utils;

import org.jppf.utils.concurrent.AsyncLogger;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public final class LoggingUtils {
  /**
   * Whether this JVM is running an Android jPPF node.
   */
  private static final boolean ANDROID = JPPFConfiguration.get(JPPFProperties.NODE_ANDROID);

  /**
   * Instanciation not permitted.
   */
  private LoggingUtils() {
  }

  /**
   * Create an optionally asynchronous logger.
   * @param clazz the class whose name is given as the logger name.
   * @param async whether to create an asynchronous logger.
   * @return a {@link Logger} instance.
   */
  public static Logger getLogger(final Class<?> clazz, final boolean async) {
    Logger log = LoggerFactory.getLogger(clazz);
    return async ? new AsyncLogger(log) : log;
  }

  /**
   * Determine whether debug level is enabled for the specified logger.
   * @param log the logger to check.
   * @return {@code true} if debug level is enabled, {@code false} otherwise.
   */
  public static boolean isDebugEnabled(final Logger log) {
    return ANDROID || log.isDebugEnabled();
  }
}
