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

package org.jppf.example.fj;

import java.io.Serializable;

/**
 * Instance of this class represents result of computation.
 * @author Martin JANDA
 */
public class FibonacciResult implements Serializable {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Idicator whether fork join was used for computation.
   */
  private final boolean forkJoinUsed;
  /**
   * The task result.
   */
  private final long    result;

  /**
   * Instatiates new result object.
   * @param forkJoinUsed indicator whether result was computer by fork join.
   * @param result computed Fibonacci number.
   */
  public FibonacciResult(final boolean forkJoinUsed, final long result) {
    this.forkJoinUsed = forkJoinUsed;
    this.result = result;
  }

  /**
   * Get indicator whether for join was used for computation.
   * @return <code>true</code> if ForkJoin algorithm was used for computation.
   */
  public boolean isForkJoinUsed() {
    return forkJoinUsed;
  }

  /**
   * Get Fibonacci number.
   * @return the Fibonacci number.
   */
  public long getResult() {
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("FibonacciResult");
    sb.append("{forkJoinUsed=").append(forkJoinUsed);
    sb.append(", result=").append(result);
    sb.append('}');
    return sb.toString();
  }
}
