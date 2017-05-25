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

package org.jppf.utils.collections;

import java.util.Comparator;

/**
 * This comparator defines a descending value order for integers.
 * @exclude
 */
public class DescendingIntegerComparator implements Comparator<Integer> {
  /**
   * Compare two integers. This comparator defines a descending order for integers.
   * @param o1 first integer to compare.
   * @param o2 second integer to compare.
   * @return -1 if o1 > o2, 0 if o1 == o2, 1 if o1 < o2
   */
  @Override
  public int compare(final Integer o1, final Integer o2) {
    if (o1 == null) return (o2 == null) ? 0 : 1;
    else if (o2 == null) return -1;
    return o2.compareTo(o1);
  }
}