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

package org.jppf.utils;

import java.util.regex.Pattern;



/**
 * This class provides a set of utility methods for manipulating regular expressions.
 * @author Laurent Cohen
 */
public final class RegexUtils {
  /**
   * Pre-compiled regex pattern that matches any sequence of one or more spaces.
   */
  public static final Pattern SPACES_PATTERN = Pattern.compile("\\s+");
  /**
   * Pre-compiled regex pattern that matches any one minus sign.
   */
  public static final Pattern MINUS_PATTERN = Pattern.compile("-");
  /**
   * Pre-compiled regex pattern that matches any slash '/' character.
   */
  public static final Pattern SLASH_PATTERN = Pattern.compile("/");
  /**
   * Pre-compiled regex pattern that matches any dot '.' character.
   */
  public static final Pattern DOT_PATTERN = Pattern.compile("\\.");
  /**
   * Pre-compiled regex pattern that matches any column ':' character.
   */
  public static final Pattern COLUMN_PATTERN = Pattern.compile(":");
  /**
   * Pre-compiled regex pattern that matches any pipe '|' character.
   */
  public static final Pattern PIPE_PATTERN = Pattern.compile("\\|");

  /**
   * Instantiation of this class is not permitted.
   */
  private RegexUtils() {
  }
}
