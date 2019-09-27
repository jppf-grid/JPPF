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

package org.jppf.job;

/**
 * 
 * @param <E> the type of operand to compare with.
 * @author Laurent Cohen
 */
abstract class AbstractTernaryComparisonJobSelector<E> extends AbstractBinaryComparisonJobSelector<E> {
  /**
   * The upper bound value to compare the metadata value with.
   */
  final E operand2;

  /**
   * 
   * @param key the metadata key of the value to compare with.
   * @param operand the lower bound to compare with.
   * @param operand2 the upper bound to compare with.
   */
  public AbstractTernaryComparisonJobSelector(final Object key, final E operand, final E operand2) {
    super(key, operand);
    this.operand2 = operand2;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((operand2 == null) ? 0 : operand2.hashCode());
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (!super.equals(obj)) return false;
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    final AbstractTernaryComparisonJobSelector<?> other = (AbstractTernaryComparisonJobSelector<?>) obj;
    if (operand2 == null) {
      if (other.operand2 != null) return false;
    } else if (!operand2.equals(other.operand2)) return false;
    return true;
  }
}
