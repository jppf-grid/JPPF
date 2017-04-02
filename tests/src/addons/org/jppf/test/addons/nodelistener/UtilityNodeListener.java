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

package org.jppf.test.addons.nodelistener;

import java.lang.reflect.Method;

import org.jppf.node.event.*;
import org.jppf.node.protocol.*;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class UtilityNodeListener extends NodeLifeCycleListenerAdapter {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(UtilityNodeListener.class);
  /**
   * Default contructor.
   */
  public UtilityNodeListener() {
    System.out.println("in UtilityNodeListener()");
  }

  @Override
  public void jobStarting(final NodeLifeCycleEvent event) {
    try {
      for (Task<?> t: event.getTasks()) {
        Class<?> clazz = t.getClass();
        if ("LifeCycleTask".equals(clazz.getSimpleName())) {
          Method m1 = clazz.getMethod("isFetchMetadata");
          boolean isFetchMetadata = (Boolean) m1.invoke(t);
          if (isFetchMetadata) {
            System.out.println("setting job metadata on task " + t);
            Method m2 = clazz.getMethod("setMetadata", JobMetadata.class);
            m2.invoke(t, event.getJob().getMetadata());
          }
        }
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }
}
