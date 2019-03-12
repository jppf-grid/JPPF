/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

import java.util.UUID;

/**
 * Utility methods to generate UUIDs in various formats.
 * @author Laurent Cohen
 */
public class JPPFUuid {
  /**
   * Create a UUID in a standard format as described in {@link java.util.UUID#toString()}.
   * @return a normalized UUID represented as a string.
   */
  public static String normalUUID() {
    return normalUUID(true);
  }

  /**
   * Create a UUID in a standard format as described in {@link java.util.UUID#toString()}.
   * @param upper whether to return an uppercase string.
   * @return a normalized UUID represented as a string.
   */
  public static String normalUUID(final boolean upper) {
    final String s = UUID.randomUUID().toString();
    return upper ? s.toUpperCase() : s;
  }

  /**
   * Create a UUID in hexadecimal format.
   * @param upper whether to return an uppercase string.
   * @return a normalized UUID represented as a string.
   */
  public static String hexUUID(final boolean upper) {
    return normalUUID(upper).replace("-", "");
  }
}
