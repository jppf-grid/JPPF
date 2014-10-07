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

package org.jppf.utils;

/**
 * A set of utility methods to facilitate concurrent and multithreaded rpogramming.
 * @author Laurent Cohen
 * @since 5.0
 */
public final class ComparisonUtils {
  /**
   * Instantiation is not permitted.
   */
  private ComparisonUtils() {
  }

  /**
   * Determine whether the specified strigns are equal.
   * @param s1 the first string to compare.
   * @param s2 the second string to compare.
   * @return {@code true} if the two strings are equal, {@code false} otherwise.
   */
  public static boolean equalStrings(final String s1, final String s2) {
    if (s1 == null) return s2 == null;
    return s1.equals(s2);
  }

  /**
   * Determine whether the specified strigns are equal.
   * @param a1 the first array to compare.
   * @param a2 the second array to compare.
   * @return {@code true} if the two strings are equal, {@code false} otherwise.
   */
  public static boolean equalIntArrays(final int[] a1, final int[] a2) {
    if (a1 == a2) return true;
    if ((a1 == null) || (a2 == null)) return false;
    if (a1.length != a2.length) return false;
    for (int i=0; i<a1.length; i++) {
      if (a1[i] != a2[i]) return false;
    }
    return true;
  }
}
