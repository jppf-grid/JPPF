/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.dotnet;

import java.lang.reflect.Method;

import javax.management.*;

import org.jppf.utils.ExceptionUtils;
import org.slf4j.*;

/**
 * This class wraps a .Net job listener to which job event notifications are delegated.
 * @author Laurent Cohen
 * @since 5.0
 * @exclude
 */
public class DotnetNotificationListenerWrapper implements NotificationListener {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(DotnetNotificationListenerWrapper.class);
  /**
   * A proxy to a .Net job listener.
   */
  private Object dotnetListener;

  /**
   * Initialize this wrapper with the specified proxy to a .Net job listener.
   * @param dotnetListener a proxy to a .Net job listener.
   */
  public DotnetNotificationListenerWrapper(final system.Object dotnetListener) {
    if (dotnetListener == null) throw new IllegalArgumentException(".Net listener cannot be null");
    //System.out.printf("Creating job listener with dotnetListener=%s, class=%s%n", dotnetListener, dotnetListener.getClass());
    this.dotnetListener = dotnetListener;
    if (log.isDebugEnabled()) log.debug("initializing with listener = {}", this.dotnetListener);
  }


  @Override
  public void handleNotification(final Notification notification, final Object handback) {
    if (log.isDebugEnabled()) log.debug("received notification {}", notification);
    if (dotnetListener == null) return;
    try {
      Class<?> clazz = dotnetListener.getClass();
      /*
      System.out.println("[Java] notification dispatcher class = " + clazz.getName());
      for (Method m: clazz.getDeclaredMethods()) {
        if ("HandleNotification".equals(m.getName())) System.out.println("found method HandleNotification: " + m);
      }
      */
      Method m = clazz.getMethod("HandleNotification", Object.class);
      m.invoke(dotnetListener, notification);
    } catch (Exception e) {
      log.error("error invoking {}() : {}", "HandleNotification", ExceptionUtils.getStackTrace(e));
    }
  }
}
