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
package org.jppf.doc;

import java.io.FileFilter;

/**
 * Filter that only accepts directories.
 * It is possible to include or excluded specific directory names, in which case the including filter is applied before the excluding one.
 */
public abstract class AbstractFileFilter implements FileFilter
{
  /**
   * Included names. If specified, only these directories will be included.
   */
  protected String[] includes;
  /**
   * Excluded names. If specified, only these directories will be excluded.
   */
  protected String[] excludes;

  /**
   * Initialize a filter accepting all names.
   */
  public AbstractFileFilter()
  {
  }

  /**
   * Check whether the specified name is included by this filter.
   * @param name the name to check.
   * @param ignoreCase determines whether name comparisons should ignore case.
   * @return true if the name is included, false otherwise.
   */
  protected boolean included(final String name, final boolean ignoreCase)
  {
    return checkFilter(name, ignoreCase, includes, true);
  }

  /**
   * Check whether the specified name is excluded by this filter.
   * @param name the name to check.
   * @param ignoreCase determines whether name comparisons should ignore case.
   * @return true if the name is excluded, false otherwise.
   */
  protected boolean excluded(final String name, final boolean ignoreCase)
  {
    return checkFilter(name, ignoreCase, excludes, false);
  }

  /**
   * Check whether the specified name matches a value in the specified array.
   * @param name the name to check.
   * @param ignoreCase determines whether name comparisons should ignore case.
   * @param array the filter array to check against.
   * @param returnValueIfEmpty the value to return if the array is null or empty.
   * @return true if the name matches one of the values in the array, false otherwise.
   */
  private static boolean checkFilter(final String name, final boolean ignoreCase, final String[] array, final boolean returnValueIfEmpty)
  {
    if ((array == null) || (array.length == 0)) return returnValueIfEmpty;
    for (String s: array)
    {
      if (s == null) continue;
      boolean b = ignoreCase ? s.equalsIgnoreCase(name) : s.equals(name);
      if (b) return true;
    }
    return false;
  }
}
