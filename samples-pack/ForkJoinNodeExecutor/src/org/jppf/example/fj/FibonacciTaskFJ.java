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

import java.util.concurrent.RecursiveTask;

/**
 * Task to compute Fibonacci number.
 * @author Martin JANDA
 */
public class FibonacciTaskFJ extends RecursiveTask<Long> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Order of Fibonacci number to compute.
   */
  private final int n;

  /**
   * Initializes Fibonacci task with given order.
   * @param n order of Fibonacci number to compute. Must be greater or equal to zero.
   */
  public FibonacciTaskFJ(final int n) {
    if (n < 0) throw new IllegalArgumentException("n < 0");
    this.n = n;
  }

  /**
   * Computes Fibonacci number for nth order.
   * @return Fibonacci number.
   */
  @Override
  protected Long compute() {
    if (n <= 1) return (long) n;
    FibonacciTaskFJ f1 = new FibonacciTaskFJ(n - 1);
    f1.fork();
    FibonacciTaskFJ f2 = new FibonacciTaskFJ(n - 2);
    return f2.compute() + f1.join();
  }
}
