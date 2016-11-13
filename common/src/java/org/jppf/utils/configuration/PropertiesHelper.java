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

package org.jppf.utils.configuration;

import java.io.*;
import java.util.*;

/**
 * 
 * @author Laurent Cohen
 */
public class PropertiesHelper {
  /**
   * Store the specified properties into the specified writer.
   * @param props the properties to store.
   * @param writer the writer to store with.
   * @throws IOException if any error occurs.
   */
  public static void store(final Properties props, final Writer writer) throws IOException {
    Set<String> keys = new TreeSet<>(props.stringPropertyNames());
    for (String k: keys) {
      Object v = props.get(k);
      if (v instanceof String) {
        writer.write(k);
        writer.write(" = ");
        writer.write((String) v);
        writer.write('\n');
      }
    }
  }
}
