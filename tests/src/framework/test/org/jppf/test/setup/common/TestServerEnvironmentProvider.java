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

package test.org.jppf.test.setup.common;

import java.util.*;

import javax.management.remote.generic.*;

/**
 * A test {@link ClientEnvironmentProvider}.
 * @author Laurent Cohen
 */
public class TestServerEnvironmentProvider implements ServerEnvironmentProvider {
  /**
   * If {@code true} then return a populated map, otherwise return null.
   */
  public static boolean active = false;
  /**
   * The populated environment provided when active.
   */
  public static Map<String, ?> env = getPopulatedMap();

  @Override
  public Map<String, ?> getEnvironment() {
    return active ? env : null;
  }

  /**
   * Generate a populated environment.
   * @return a map of string keys to proerty values.
   */
  private static Map<String, Object> getPopulatedMap() {
    Object[] values = { "value1", 2, true, '4'};
    Map<String, Object> map = new HashMap<>();
    for (int i=0; i<values.length; i++) map.put("server.env.prop." + (i+1), values[i]);
    return map;
  }
}
