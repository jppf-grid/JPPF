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

package org.jppf.node.protocol;

/**
 * Interface representig an object that cn be accessed by its position in an ordered set.
 * @param <T> the type of this element.
 * @author Laurent Cohen
 */
public interface PositionalElement<T extends PositionalElement<T>> {
  /**
   * Returns the position of this element in its container.
   * @return the position of this element  as an {@code int}.
   * @exclude
   */
  int getPosition();

  /**
   * Set the position of this element in its container.
   * @param position the position of this task as an {@code int}.
   * @return this element, for method call chaining.
   * @exclude
   */
  default T setPosition(int position) {
    return null;
  }
}
