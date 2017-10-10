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

package com.sun.jmx.remote.protocol.jmxmp;

import java.lang.reflect.*;
import java.util.*;

import com.sun.jmx.remote.opt.util.ClassLogger;

/**
 * @author Laurent Cohen
 */
public class EnvManager {
  /** */
  private final ClassLogger logger = new ClassLogger("com.sun.jmx.remote.protocol.jmxmp", "EnvManager");
  /**
   * Handles the environment providers that allow adding to, or overriding, the environment properties
   * passed to each new JMX connector server instance.  
   */
  private final Object envHandler;
  /**
   * 
   */
  private Map<Method, Object> providerMap = new HashMap<>();

  /**
   * 
   * @param inf  the fully qualified class name of the interface of the environment providers to load.
   */
  public EnvManager(final String inf) {
    envHandler = createEnvProviderHandler(inf);
  }

  /**
   * Create an environment provider handler.
   * @param inf the fully qualified class name of the interface of the environment providers to load.
   * @return an instance of {@code EnvironmentProviderHandler}.
   */
  private Object createEnvProviderHandler(final String inf) {
    Object handler = null;
    try {
      Class<?> infClass = Class.forName(inf);
      Class<?> handlerClass = Class.forName("org.jppf.jmx.EnvironmentProviderHandler");
      Constructor<?> c = handlerClass.getConstructor(Class.class);
      Method getProvidersMethod = handlerClass.getDeclaredMethod("getProviders");
      handler = c.newInstance(infClass);
      List<?> list = (List<?>) getProvidersMethod.invoke(handler);
      for (Object o: list) {
        if (o != null) {
          providerMap.put(o.getClass().getDeclaredMethod("getEnvironment"), o);
        }
      }
    } catch (Exception e) {
      logger.error("createEnvProviderHandler", "Error creating EnvironmentProviderHandler for '%s'", e, inf);
    }
    return handler;
  }

  /**
   * 
   * @param env the environment to augment
   */
  public void augmentEnvironment(final Map<String, Object> env) {
    try {
      for (Map.Entry<Method, Object> entry: providerMap.entrySet()) {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) entry.getKey().invoke(entry.getValue());
        if ((map != null) && !map.isEmpty()) env.putAll(map);
      }
    } catch (Exception e) {
      logger.error("augmentEnvironment", "Error augmenting environment %s", e, env);
    }
  }
}
