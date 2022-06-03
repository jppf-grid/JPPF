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

package test.org.jppf.server.protocol;

import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.node.protocol.AbstractTask;

/**
 * This task maintains a counter in the node and resubmits itself
 * until the counter has reached a specified value.
 */
public class ResubmittingTask extends AbstractTask<Integer> {
  /** */
  private static final long serialVersionUID = 1L;
  /**
   * Maximum number of runs for this task = max resubmit + 1.
   */
  private final int nbRuns;
  /** */
  public static AtomicInteger counter;

  /**
   * Initialie this task.
   * @param nbRuns the maximum number of runs for this task.
   */
  public ResubmittingTask(final int nbRuns) {
    this.nbRuns = nbRuns;
  }

  @Override
  public void run() {
    if (counter == null) {
      System.out.printf("ResubmittingTask: creating counter\n");
      counter = new AtomicInteger(0);
    }
    final int n = counter.incrementAndGet();
    System.out.printf("ResubmittingTask: counter = %d\n", n);
    if (n < nbRuns) setResubmit(true);
    else counter = null;
    System.out.printf("ResubmittingTask: resubmit = %b\n", isResubmit());
    setResult(n);
  }
}
