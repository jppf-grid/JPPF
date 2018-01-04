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

package org.jppf.utils.concurrent;

/**
 *
 * @author Laurent Cohen
 */
public class MutableBoolean {
  /**
   * The boolean value of this object.
   */
  private boolean value;

  /**
   * Initialize to {@code false}.
   */
  public MutableBoolean() {
  }

  /**
   * Initialize with the specified value.
   * @param value the initial value.
   */
  public MutableBoolean(final boolean value) {
    this.value = value;
  }

  /**
   * @return the value.
   */
  public boolean get() {
    return value;
  }

  /**
   * Set the value.
   * @param value the value to set.
   */
  public void set(final boolean value) {
    this.value = value;
  }
}
