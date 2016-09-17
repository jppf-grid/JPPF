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

package com.sun.jmx.remote.opt.internal;

import java.security.*;
import java.util.Map;

import javax.management.*;

import com.sun.jmx.remote.opt.util.ClassLogger;

/**
 * 
 * @author Laurent Cohen
 */
class NotificationUtils {
  /**
   * Name of the attribute that specifies whether notifications should be purged upon first fetch on the connector server side.
   */
  public static final String PURGE_UPON_FETCH = "jmx.remote.x.notification.purge.upon.fetch";
  /** */
  private static final ClassLogger logger = new ClassLogger("javax.management.remote.misc", "NotificationUtils");

  /**
   * @param mbs .
   * @param name .
   * @param className .
   * @return .
   */
  static boolean isInstanceOf(final MBeanServer mbs, final ObjectName name, final String className) {
    PrivilegedExceptionAction<Boolean> act = new PrivilegedExceptionAction<Boolean>() {
      @Override
      public Boolean run() throws InstanceNotFoundException {
        return new Boolean(mbs.isInstanceOf(name, className));
      }
    };
    try {
      return AccessController.doPrivileged(act).booleanValue();
    } catch (Exception e) {
      logger.fine("isInstanceOf", "failed: " + e);
      logger.debug("isInstanceOf", e);
      return false;
    }
  }

  /**
   * Iterate until we extract the real exception from a stack of PrivilegedActionExceptions.
   * @param e .
   * @return .
   */
  static Exception extractException(final Exception e) {
    Exception ex = e;
    while (ex instanceof PrivilegedActionException) {
      ex = ((PrivilegedActionException) ex).getException();
    }
    return ex;
  }

  /**
   * Determine whether notifications should be purged upon first fetch on the connector server side.<br>
   * This is specified as the boolean attribute "{@code jmx.remote.x.notification.purge.upon.fetch}",
   * which is looked up first in the provided {@code env} map, then in the system properties if not specified in {@code env}.
   * @param env the environment map provided upon construction of the remote connector server.
   * @return {@code true} if notifications should be purged on first fetch, {@code false} otherwise.
   */
  static boolean isPurgeUponFetch(final Map<String, ?> env) {
    Object o = null;
    if (env != null) o = env.get(PURGE_UPON_FETCH);
    if (o == null) o = System.getProperty(PURGE_UPON_FETCH);
    if (o instanceof Boolean) return (Boolean) o;
    else if (o instanceof String) return Boolean.valueOf((String) o);
    return false;
  }
}
